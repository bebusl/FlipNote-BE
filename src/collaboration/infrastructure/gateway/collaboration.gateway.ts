import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  ConnectedSocket,
  MessageBody,
  OnGatewayConnection,
  OnGatewayDisconnect,
} from '@nestjs/websockets';
import { Logger, UseGuards } from '@nestjs/common';
import { Server, Socket } from 'socket.io';
import * as Y from 'yjs';
import { WsAuthGuard } from '../../../auth/infrastructure/guard/ws-auth.guard';
import { WsUser } from '../../../shared/decorator/ws-user.decorator';
import type { UserAuth } from '../../../shared/types/user-auth.type';
import { YjsDocumentService } from '../redis/yjs-document.service';
import { CollaborationUseCase } from '../../application/collaboration.use-case';

@UseGuards(WsAuthGuard)
@WebSocketGateway({
  cors: { origin: '*' },
  namespace: '/v1/card-sets/ws',
  pingTimeout: 60000,
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

  constructor(
    private readonly yjsDocumentService: YjsDocumentService,
    private readonly collaborationUseCase: CollaborationUseCase,
  ) {}

  handleConnection(client: Socket) {
    this.logger.log(`Client connected: ${client.id}`);
  }

  async handleDisconnect(client: Socket) {
    this.logger.log(`Client disconnected: ${client.id}`);
    await this.removeClientFromAllCardsets(client);
  }

  @SubscribeMessage('join-cardset')
  async handleJoinCardset(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { cardsetId: string },
  ) {
    const { cardsetId } = data;
    this.logger.log(`User ${user.userId} joining cardset ${cardsetId}`);

    try {
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

      this.logger.log(`User ${user.userId} joined cardset ${cardsetId}`);
    } catch (error) {
      this.logger.error('Error joining cardset:', error);
      this.logger.error('Error details:', {
        cardsetId,
        userId: user?.userId,
        errorMessage: error instanceof Error ? error.message : String(error),
        errorStack: error instanceof Error ? error.stack : undefined,
      });

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
      this.logger.log(`User ${user.userId} leaving cardset ${cardsetId}`);

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
        `Sync request from user ${user.userId} for cardset ${cardsetId}`,
      );

      if (!update) {
        client.emit('error', { message: 'Update data is required' });
        return;
      }

      let doc = await this.yjsDocumentService.loadDocument(cardsetId);
      if (!doc) {
        doc = new Y.Doc();
        this.logger.log(
          `Created new Yjs document for cardset ${cardsetId} during update`,
        );
      }

      const updateBuffer = new Uint8Array(update);
      Y.applyUpdate(doc, updateBuffer);

      await this.yjsDocumentService.saveUpdate(cardsetId, updateBuffer);

      const state = Y.encodeStateAsUpdate(doc);
      this.server.to(`cardset:${cardsetId}`).emit('sync', {
        cardsetId,
        update: state,
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

    try {
      const doc =
        await this.collaborationUseCase.loadCardsetContentFromDB(
          numericCardsetId,
        );
      if (doc) {
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
    if (timeout) {
      clearTimeout(timeout);
      this.flushTimeouts.delete(cardsetId);
    }
  }

  private async flushCardset(cardsetId: string) {
    const activeCount =
      await this.yjsDocumentService.getActiveClientCount(cardsetId);
    if (activeCount > 0) return;

    try {
      await this.collaborationUseCase.saveCardsetContent(Number(cardsetId));
      this.logger.log(`Flushed cardset ${cardsetId} snapshot to database`);
    } catch (error) {
      this.logger.error(`Failed to flush cardset ${cardsetId}:`, error);
    }
  }
}
