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

@UseFilters(WsExceptionFilter)
@UseGuards(WsAuthGuard)
@WebSocketGateway({
  cors: false,
  path: '/v1/card-sets/ws',
  pingTimeout: 10000,
  pingInterval: 25000,
})
export class CollaborationGateway
  implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server!: Server;

  private static readonly FLUSH_DELAY_MS = 5000;

  private readonly logger = new Logger(CollaborationGateway.name);
  private flushTimeouts = new Map<string, NodeJS.Timeout>();
  private joiningClients = new Set<string>(); // join 처리 중인 clientId
  private pendingUpdates = new Map<string, { cardsetId: string; update: number[] }[]>(); // join 완료 전 수신된 update 버퍼

  constructor(
    private readonly yjsDocumentService: YjsDocumentService,
    private readonly collaborationUseCase: CollaborationUseCase,
  ) { }

  handleConnection(client: Socket) {
    this.logger.log(`[클라이언트 연결] clientId=${client.id}`);
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
    const { cardsetId } = data;
    this.logger.log(
      `[cardset 입장] userId=${user.userId}, cardsetId=${cardsetId}, clientId=${client.id}`,
    );

    try {
      const isManager = await this.collaborationUseCase.isManager(
        Number(cardsetId),
        Number(user.userId),
      );
      if (!isManager) {
        this.logger.warn(
          `[cardset 입장 거부] 매니저 아님 - userId=${user.userId}, cardsetId=${cardsetId}`,
        );
        client.emit('error', { message: '카드셋 편집 권한이 없습니다.' });
        return;
      }

      this.joiningClients.add(client.id);
      void client.join(`cardset:${cardsetId}`);

      await this.yjsDocumentService.registerClient(cardsetId, client.id);
      this.clearScheduledFlush(cardsetId);

      let doc = await this.yjsDocumentService.loadDocument(cardsetId);
      if (!doc) {
        doc = await this.loadDocumentFromDBOrCreate(cardsetId);
      }

      if (!doc) {
        this.logger.warn(
          `Failed to load or create document for cardset ${cardsetId}, creating empty document`,
        );
        doc = new Y.Doc();
      }

      const state = Y.encodeStateAsUpdate(doc);
      client.emit('sync', { cardsetId, update: Array.from(state) });
      // client.emit('joined', {
      //   cardsetId,
      //   userId: user.userId,
      //   nickname: user.nickname
      // });

      this.joiningClients.delete(client.id);
      this.logger.log(`User ${user.userId} (${user.nickname}) joined cardset ${cardsetId}`);

      const buffered = this.pendingUpdates.get(client.id);
      if (buffered && buffered.length > 0) {
        this.logger.log(
          `[버퍼 처리] join 완료 후 밀린 update 처리 - clientId=${client.id}, count=${buffered.length}`,
        );
        this.pendingUpdates.delete(client.id);
        for (const pending of buffered) {
          const updateBuffer = new Uint8Array(pending.update);
          const finalState = await this.yjsDocumentService.saveUpdate(pending.cardsetId, updateBuffer);
          this.server.to(`cardset:${pending.cardsetId}`).emit('sync', {
            cardsetId: pending.cardsetId,
            update: finalState,
          });
          this.logger.log(
            `[버퍼 처리 완료] cardsetId=${pending.cardsetId}, clientId=${client.id}`,
          );
        }
      }
    } catch (error) {
      this.logger.error('Error joining cardset:', error);
      this.logger.error('Error details:', {
        cardsetId,
        userId: user?.userId,
        errorMessage: error instanceof Error ? error.message : String(error),
        errorStack: error instanceof Error ? error.stack : undefined,
      });

      this.joiningClients.delete(client.id);
      this.pendingUpdates.delete(client.id);
      try {
        const emptyDoc = new Y.Doc();
        const state = Y.encodeStateAsUpdate(emptyDoc);
        client.emit('sync', { cardsetId, update: Array.from(state) });
        this.logger.warn(
          `Sent empty document to client due to error for cardset ${cardsetId}`,
        );
      } catch (fallbackError) {
        this.logger.error('Failed to send fallback document:', fallbackError);
        client.emit('error', {
          message: 'Failed to join cardset',
          details: error instanceof Error ? error.message : String(error),
        });
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
      const { cardsetId } = data;
      this.logger.log(
        `[cardset 퇴장] userId=${user.userId}, cardsetId=${cardsetId}, clientId=${client.id}`,
      );

      void client.leave(`cardset:${cardsetId}`);
      await this.yjsDocumentService.unregisterClient(cardsetId, client.id);
      const activeCount =
        await this.yjsDocumentService.getActiveClientCount(cardsetId);
      if (activeCount === 0) {
        this.scheduleFlush(cardsetId);
      }
      this.logger.log(`User ${user.userId} left cardset ${cardsetId}`);
    } catch (error) {
      this.logger.error('Error leaving cardset:', error);
    }
  }

  @SubscribeMessage('awareness')
  handleAwareness(
    client: Socket,
    payload: { cardsetId: string; awareness: number[] },
  ) {
    const { cardsetId, awareness } = payload;
    this.logger.log(
      `[awareness 브로드캐스트] cardsetId=${cardsetId}, clientId=${client.id}, awarenessSize=${awareness.length}`,
    );
    client.to(`cardset:${cardsetId}`).emit('awareness', {
      data: { cardsetId, awareness: new Uint8Array(awareness) },
    });
  }

  @SubscribeMessage('update')
  async handleUpdate(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { cardsetId: string; update?: number[] },
  ) {
    try {
      const { cardsetId, update } = data;
      this.logger.log(
        `[update 수신] userId=${user.userId}, cardsetId=${cardsetId}, clientId=${client.id}, updateSize=${update?.length ?? 0}`,
      );

      if (this.joiningClients.has(client.id)) {
        if (update) {
          const buffer = this.pendingUpdates.get(client.id) ?? [];
          buffer.push({ cardsetId, update });
          this.pendingUpdates.set(client.id, buffer);
          this.logger.log(
            `[update 버퍼링] join 완료 후 처리 예정 - clientId=${client.id}, cardsetId=${cardsetId}, 버퍼 크기=${buffer.length}`,
          );
        }
        return;
      }

      if (!update) {
        client.emit('error', { message: 'Update data is required' });
        return;
      }

      const updateBuffer = new Uint8Array(update);
      const finalState = await this.yjsDocumentService.saveUpdate(cardsetId, updateBuffer);

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
    const cardsets = await this.yjsDocumentService.getClientCardsets(client.id);
    this.logger.log(
      `[클라이언트 전체 cardset 제거] clientId=${client.id}, cardsets=${JSON.stringify(cardsets)}`,
    );
    if (cardsets.length === 0) return;

    for (const cardsetId of cardsets) {
      void client.leave(`cardset:${cardsetId}`);
      await this.yjsDocumentService.unregisterClient(cardsetId, client.id);
      const activeCount =
        await this.yjsDocumentService.getActiveClientCount(cardsetId);
      if (activeCount === 0) {
        this.scheduleFlush(cardsetId);
      }
    }
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
