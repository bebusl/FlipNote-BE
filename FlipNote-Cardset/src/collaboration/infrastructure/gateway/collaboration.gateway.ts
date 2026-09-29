import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  ConnectedSocket,
  MessageBody,
  OnGatewayConnection,
  OnGatewayDisconnect,
} from '@nestjs/websockets';
import { Logger, UseFilters, UseGuards } from '@nestjs/common';
import { Server, Socket } from 'socket.io';
import * as Y from 'yjs';
import { WsAuthGuard } from '../../../auth/infrastructure/guard/ws-auth.guard';
import { WsUser } from '../../../shared/decorator/ws-user.decorator';
import type { UserAuth } from '../../../shared/types/user-auth.type';
import { YjsDocumentService } from '../redis/yjs-document.service';
import { CollaborationUseCase } from '../../application/collaboration.use-case';
import { WsExceptionFilter } from '../../../shared/common/ws-exception.filter';

interface EditorSession {
  editable: boolean;
  joining: boolean;
  pendingUpdates: number[][];
}

@UseFilters(WsExceptionFilter)
@UseGuards(WsAuthGuard)
@WebSocketGateway({
  cors: false,
  path: '/v1/card-sets/ws',
  pingTimeout: 10000,
  pingInterval: 25000,
})
export class CollaborationGateway
  implements OnGatewayConnection, OnGatewayDisconnect
{
  @WebSocketServer()
  server!: Server;

  private static readonly FLUSH_DELAY_MS = 5000;

  private readonly logger = new Logger(CollaborationGateway.name);
  private flushTimeouts = new Map<string, NodeJS.Timeout>();
  private readonly sessions = new Map<string, Map<string, EditorSession>>();

  constructor(
    private readonly yjsDocumentService: YjsDocumentService,
    private readonly collaborationUseCase: CollaborationUseCase,
  ) {}

  handleConnection(client: Socket) {
    this.logger.log(`[클라이언트 연결] clientId=${client.id}`);
  }

  async revokeEditor(
    cardsetId: string,
    userId: string,
    reason: string,
  ): Promise<void> {
    // The server owns live sockets; Redis indexes can expire or be unavailable.
    // Include pending joins so a stale permission lookup cannot reopen the room.
    const clients = [...this.server.sockets.sockets.values()].filter(
      (client) =>
        this.getUserId(client) === userId && this.getSession(client, cardsetId),
    );
    await Promise.all(
      clients.map(async (client) => {
        const cleanup = this.removeClientFromCardset(client, cardsetId);
        client.emit('kicked', { cardsetId, reason });
        await cleanup;
      }),
    );
  }

  async handleDisconnect(client: Socket) {
    this.logger.log(`[클라이언트 연결 해제] clientId=${client.id}`);
    await this.removeClientFromAllCardsets(client);
  }

  @SubscribeMessage('join-cardset')
  async handleJoinCardset(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { cardsetId: string },
  ) {
    const cardsetId = this.parseCardsetId(data.cardsetId);
    if (!cardsetId) {
      client.emit('error', { message: 'Invalid cardset ID' });
      return;
    }
    if (this.getSession(client, cardsetId)) return;
    const session: EditorSession = {
      editable: false,
      joining: true,
      pendingUpdates: [],
    };
    const sessions =
      this.sessions.get(client.id) ?? new Map<string, EditorSession>();
    sessions.set(cardsetId, session);
    this.sessions.set(client.id, sessions);
    const isCurrent = () => this.getSession(client, cardsetId) === session;

    try {
      const isManager = await this.collaborationUseCase.isManager(
        Number(cardsetId),
        Number(user.userId),
      );
      if (!isCurrent()) return;
      if (!isManager) {
        this.rejectEditEvent(client, cardsetId);
        await this.removeClientFromCardset(client, cardsetId);
        return;
      }

      await client.join(this.roomName(cardsetId));
      if (!isCurrent()) return;
      session.editable = true;
      await this.yjsDocumentService.registerClient(
        cardsetId,
        client.id,
        user.userId,
      );
      if (!isCurrent()) {
        if (!this.getSession(client, cardsetId)) {
          await this.yjsDocumentService.unregisterClient(
            cardsetId,
            client.id,
            user.userId,
          );
        }
        return;
      }
      this.clearScheduledFlush(cardsetId);

      let doc = await this.yjsDocumentService.loadDocument(cardsetId);
      if (!isCurrent()) return;
      if (!doc) doc = await this.loadDocumentFromDBOrCreate(cardsetId);
      if (!isCurrent()) return;

      const state = Y.encodeStateAsUpdate(doc);
      client.emit('sync', { cardsetId, update: Array.from(state) });
      // Keep buffering during the drain, and recheck after every asynchronous save.
      while (session.pendingUpdates.length > 0 && isCurrent()) {
        const update = session.pendingUpdates.shift()!;
        const finalState = await this.yjsDocumentService.saveUpdate(
          cardsetId,
          new Uint8Array(update),
        );
        if (!isCurrent()) return;
        this.server
          .to(this.roomName(cardsetId))
          .emit('sync', { cardsetId, update: finalState });
      }
      session.joining = false;
    } catch (error) {
      this.logger.error('Error joining cardset:', error);
      if (isCurrent()) {
        await this.removeClientFromCardset(client, cardsetId);
        client.emit('error', { message: 'Failed to join cardset' });
      }
    }
  }

  @SubscribeMessage('leave-cardset')
  async handleLeaveCardset(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { cardsetId: string },
  ) {
    try {
      const cardsetId = this.parseCardsetId(data.cardsetId);
      if (!cardsetId) {
        client.emit('error', { message: 'Invalid cardset ID' });
        return;
      }
      this.logger.log(
        `[cardset 퇴장] userId=${user.userId}, cardsetId=${cardsetId}, clientId=${client.id}`,
      );

      await this.removeClientFromCardset(client, cardsetId);
      this.logger.log(`User ${user.userId} left cardset ${cardsetId}`);
    } catch (error) {
      this.logger.error('Error leaving cardset:', error);
    }
  }

  @SubscribeMessage('awareness')
  handleAwareness(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody() payload: { cardsetId: string; awareness: number[] },
  ) {
    const { awareness } = payload;
    const cardsetId = this.parseCardsetId(payload.cardsetId);
    if (!cardsetId) {
      client.emit('error', { message: 'Invalid cardset ID' });
      return;
    }
    if (!this.canEdit(client, cardsetId)) {
      this.rejectEditEvent(client, cardsetId);
      return;
    }
    this.logger.log(
      `[awareness 브로드캐스트] cardsetId=${cardsetId}, userId=${user.userId}, clientId=${client.id}, awarenessSize=${awareness.length}`,
    );
    client.to(`cardset:${cardsetId}`).emit('awareness', {
      data: {
        cardsetId,
        awareness: new Uint8Array(awareness),
        userId: user.userId,
        userName: user.nickname,
      },
    });
  }

  @SubscribeMessage('update')
  async handleUpdate(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { cardsetId: string; update?: number[] },
  ) {
    try {
      const { update } = data;
      const cardsetId = this.parseCardsetId(data.cardsetId);
      if (!cardsetId) {
        client.emit('error', { message: 'Invalid cardset ID' });
        return;
      }
      this.logger.log(
        `[update 수신] userId=${user.userId}, cardsetId=${cardsetId}, clientId=${client.id}, updateSize=${update?.length ?? 0}`,
      );

      if (!this.canEdit(client, cardsetId)) {
        this.rejectEditEvent(client, cardsetId);
        return;
      }

      const session = this.getSession(client, cardsetId)!;
      if (session.joining) {
        if (update) session.pendingUpdates.push(update);
        return;
      }

      if (!update) {
        client.emit('error', { message: 'Update data is required' });
        return;
      }

      const updateBuffer = new Uint8Array(update);
      const finalState = await this.yjsDocumentService.saveUpdate(
        cardsetId,
        updateBuffer,
      );

      if (this.getSession(client, cardsetId) !== session) return;
      this.server.to(`cardset:${cardsetId}`).emit('sync', {
        cardsetId,
        update: finalState,
      });
      this.logger.log(
        `Sync update from user ${user.userId} broadcasted to all clients in cardset ${cardsetId}`,
      );
    } catch (error) {
      this.logger.error('Error during sync:', error);
      client.emit('error', { message: 'Sync failed' });
    }
  }

  private async loadDocumentFromDBOrCreate(cardsetId: string): Promise<Y.Doc> {
    const numericCardsetId = Number(cardsetId);
    this.logger.log(
      `[DB에서 문서 로드 시도] cardsetId=${cardsetId}, numericCardsetId=${numericCardsetId}`,
    );

    try {
      const doc =
        await this.collaborationUseCase.loadCardsetContentFromDB(
          numericCardsetId,
        );
      if (doc) {
        // DB 로드 완료 후 Redis에 저장하기 전에 다시 확인 — 그 사이 update가 Redis에 저장했을 수 있음
        const existing = await this.yjsDocumentService.loadDocument(cardsetId);
        if (existing) {
          this.logger.log(
            `[DB에서 문서 로드 시도] Redis에 이미 doc 존재, 덮어쓰기 생략 - cardsetId=${cardsetId}`,
          );
          return existing;
        }
        await this.yjsDocumentService
          .saveDocument(cardsetId, doc)
          .catch((error) => {
            this.logger.warn(
              `Failed to save document to Redis after DB load: ${error}`,
            );
          });
        this.logger.log(
          `Loaded Yjs document from DB and saved to Redis for cardset ${cardsetId}`,
        );
        return doc;
      }
    } catch (error) {
      this.logger.warn(
        `Failed to load from DB for cardset ${cardsetId}, creating new document: ${error}`,
      );
    }

    return this.createNewDocument(cardsetId);
  }

  private async createNewDocument(cardsetId: string): Promise<Y.Doc> {
    this.logger.log(`[새 문서 생성] cardsetId=${cardsetId}`);
    // 저장 전 Redis 재확인 — 그 사이 update가 저장했을 수 있음
    const existing = await this.yjsDocumentService.loadDocument(cardsetId);
    if (existing) {
      this.logger.log(
        `[새 문서 생성] Redis에 이미 doc 존재, 빈 doc 저장 생략 - cardsetId=${cardsetId}`,
      );
      return existing;
    }
    const doc = new Y.Doc();
    this.logger.log(`Created new Yjs document for cardset ${cardsetId}`);
    await this.yjsDocumentService
      .saveDocument(cardsetId, doc)
      .catch((error) => {
        this.logger.warn(
          `Failed to save new document to Redis: ${error}, continuing anyway`,
        );
      });
    return doc;
  }

  private async removeClientFromAllCardsets(client: Socket) {
    const cardsets = [...(this.sessions.get(client.id)?.keys() ?? [])];
    await Promise.all(
      cardsets.map((cardsetId) =>
        this.removeClientFromCardset(client, cardsetId),
      ),
    );
  }

  private async removeClientFromCardset(
    client: Socket,
    cardsetId: string,
  ): Promise<void> {
    const sessions = this.sessions.get(client.id);
    const session = sessions?.get(cardsetId);
    if (!session) return;
    // Cancel synchronously, before any asynchronous room/Redis cleanup.
    session.editable = false;
    session.pendingUpdates.length = 0;
    sessions!.delete(cardsetId);
    if (sessions!.size === 0) this.sessions.delete(client.id);
    await client.leave(this.roomName(cardsetId));
    await this.yjsDocumentService.unregisterClient(
      cardsetId,
      client.id,
      this.getUserId(client),
    );
    const activeCount =
      await this.yjsDocumentService.getActiveClientCount(cardsetId);
    if (activeCount === 0) this.scheduleFlush(cardsetId);
  }

  private parseCardsetId(value: unknown): string | null {
    if (typeof value !== 'string' && typeof value !== 'number') return null;
    if (!/^\d+$/.test(String(value))) return null;
    const id = Number(value);
    return Number.isSafeInteger(id) && id > 0 ? String(id) : null;
  }

  private roomName(cardsetId: string): string {
    return `cardset:${cardsetId}`;
  }

  private getSession(
    client: Socket,
    cardsetId: string,
  ): EditorSession | undefined {
    return this.sessions.get(client.id)?.get(cardsetId);
  }

  private canEdit(client: Socket, cardsetId: string): boolean {
    return (
      client.rooms.has(this.roomName(cardsetId)) &&
      this.getSession(client, cardsetId)?.editable === true
    );
  }

  private rejectEditEvent(client: Socket, cardsetId: string): void {
    this.logger.warn(
      `[편집 이벤트 거부] cardsetId=${cardsetId}, clientId=${client.id}`,
    );
    client.emit('error', { message: '카드셋 편집 권한이 없습니다.' });
  }

  private getUserId(client: Socket): string | undefined {
    const data = client.data as { user?: UserAuth };
    return data.user?.userId;
  }

  private scheduleFlush(cardsetId: string) {
    this.logger.log(
      `[flush 예약] cardsetId=${cardsetId}, delayMs=${CollaborationGateway.FLUSH_DELAY_MS}`,
    );
    if (this.flushTimeouts.has(cardsetId)) return;
    const timeout = setTimeout(() => {
      this.flushTimeouts.delete(cardsetId);
      void this.flushCardset(cardsetId);
    }, CollaborationGateway.FLUSH_DELAY_MS);
    this.flushTimeouts.set(cardsetId, timeout);
    this.logger.log(`Scheduled cardset ${cardsetId} flush`);
  }

  private clearScheduledFlush(cardsetId: string) {
    const timeout = this.flushTimeouts.get(cardsetId);
    this.logger.log(
      `[flush 예약 취소] cardsetId=${cardsetId}, hadScheduled=${!!timeout}`,
    );
    if (timeout) {
      clearTimeout(timeout);
      this.flushTimeouts.delete(cardsetId);
    }
  }

  private async flushCardset(cardsetId: string) {
    const activeCount =
      await this.yjsDocumentService.getActiveClientCount(cardsetId);
    this.logger.log(
      `[cardset flush 실행] cardsetId=${cardsetId}, activeCount=${activeCount}`,
    );
    if (activeCount > 0) return;

    try {
      await this.yjsDocumentService.flushIncrementalHistory(cardsetId);
      this.logger.log(
        `[cardset flush 완료] 증분값 DB 저장 완료 - cardsetId=${cardsetId}`,
      );
      await this.yjsDocumentService.deleteDocument(cardsetId);
      this.logger.log(
        `[Redis 문서 삭제] 방 비어있어 Redis 문서 삭제 완료 - cardsetId=${cardsetId}`,
      );
    } catch (error) {
      this.logger.error(`Failed to flush cardset ${cardsetId}:`, error);
    }
  }
}
