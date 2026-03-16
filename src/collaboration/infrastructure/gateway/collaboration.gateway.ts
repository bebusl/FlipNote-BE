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
import { WsAuthGuard } from '../../../auth/infrastructure/guard/ws-auth.guard';
import { WsUser } from '../../../shared/decorator/ws-user.decorator';
import type { UserAuth } from '../../../shared/types/user-auth.type';
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

  private readonly logger = new Logger(CollaborationGateway.name);

  constructor(private readonly collaborationUseCase: CollaborationUseCase) {}

  handleConnection(client: Socket) {
    this.logger.log(`Client connected: ${client.id}`);
  }

  handleDisconnect(client: Socket) {
    this.logger.log(`Client disconnected: ${client.id}`);
  }

  @SubscribeMessage('join-cardset')
  async handleJoinCardset(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { cardsetId: string },
  ) {
    try {
      const { cardsetId } = data;
      this.logger.log(`User ${user.userId} joining cardset ${cardsetId}`);

      void client.join(`cardset:${cardsetId}`);

      const state = await this.collaborationUseCase.getState(
        parseInt(cardsetId),
      );
      client.emit('sync-response', {
        cardsetId,
        update: Array.from(state),
      });

      this.logger.log(`User ${user.userId} joined cardset ${cardsetId}`);
    } catch (error) {
      this.logger.error('Error joining cardset:', error);
      client.emit('error', { message: 'Failed to join cardset' });
    }
  }

  @SubscribeMessage('leave-cardset')
  async handleLeaveCardset(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { cardsetId: string },
  ) {
    try {
      await client.leave(`cardset:${data.cardsetId}`);
      this.logger.log(`User ${user.userId} left cardset ${data.cardsetId}`);
    } catch (error) {
      this.logger.error('Error leaving cardset:', error);
    }
  }

  @SubscribeMessage('sync')
  async handleSync(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody()
    data: { cardsetId: string; update?: number[] },
  ) {
    try {
      const { cardsetId, update } = data;
      this.logger.log(
        `Sync request from user ${user.userId} for cardset ${cardsetId}`,
      );

      let state: Uint8Array;
      if (update) {
        state = await this.collaborationUseCase.applyUpdate(
          parseInt(cardsetId),
          update,
        );
      } else {
        state = await this.collaborationUseCase.getState(parseInt(cardsetId));
      }

      client.emit('sync-response', {
        cardsetId,
        update: Array.from(state),
      });
    } catch (error) {
      this.logger.error('Error during sync:', error);
      client.emit('error', { message: 'Sync failed' });
    }
  }

  @SubscribeMessage('update-card')
  async handleUpdateCard(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody()
    data: {
      cardsetId: string;
      cardId: string;
      updates: Partial<{ content: string; order: number }>;
    },
  ) {
    try {
      const { cardsetId } = data;
      const state = await this.collaborationUseCase.getState(
        parseInt(cardsetId),
      );

      this.server.to(`cardset:${cardsetId}`).emit('sync-response', {
        cardsetId,
        update: Array.from(state),
      });

      this.logger.log(
        `User ${user.userId} updated card in cardset ${cardsetId}`,
      );
    } catch (error) {
      this.logger.error('Error updating card:', error);
      client.emit('error', { message: 'Failed to update card' });
    }
  }

  @SubscribeMessage('reorder-cards')
  async handleReorderCards(
    @WsUser() user: UserAuth,
    @ConnectedSocket() client: Socket,
    @MessageBody()
    data: {
      cardsetId: string;
      cardOrders: { cardId: string; order: number }[];
    },
  ) {
    try {
      const { cardsetId } = data;
      const state = await this.collaborationUseCase.getState(
        parseInt(cardsetId),
      );

      this.server.to(`cardset:${cardsetId}`).emit('sync-response', {
        cardsetId,
        update: Array.from(state),
      });

      this.logger.log(
        `User ${user.userId} reordered cards in cardset ${cardsetId}`,
      );
    } catch (error) {
      this.logger.error('Error reordering cards:', error);
      client.emit('error', { message: 'Failed to reorder cards' });
    }
  }
}
