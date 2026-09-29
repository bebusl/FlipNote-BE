import { Injectable, Logger } from '@nestjs/common';
import { RabbitSubscribe } from '@golevelup/nestjs-rabbitmq';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CardsetOrmEntity } from '../../../cardset/infrastructure/persistence/orm/cardset.orm-entity';
import { CardsetManagerOrmEntity } from '../../../cardset/infrastructure/persistence/orm/cardset-manager.orm-entity';
import { CollaborationGateway } from '../gateway/collaboration.gateway';

type EditorAccessMessage =
  | {
      eventType: 'CARDSET_EDITOR_REVOKED';
      cardsetId: number;
      userId: number;
      reason: string;
    }
  | {
      eventType: 'GROUP_MEMBER_KICKED';
      groupId: number;
      userId: number;
      reason: string;
    };

function isEditorAccessMessage(
  message: unknown,
): message is EditorAccessMessage {
  if (!message || typeof message !== 'object') return false;
  const value = message as Record<string, unknown>;
  const isId = (id: unknown): boolean =>
    typeof id === 'number' && Number.isSafeInteger(id) && id > 0;
  return (
    isId(value.userId) &&
    ((value.eventType === 'CARDSET_EDITOR_REVOKED' &&
      isId(value.cardsetId) &&
      value.reason === 'MANAGER_REMOVED') ||
      (value.eventType === 'GROUP_MEMBER_KICKED' &&
        isId(value.groupId) &&
        value.reason === 'GROUP_MEMBER_KICKED'))
  );
}

@Injectable()
export class EditorAccessConsumer {
  private readonly logger = new Logger(EditorAccessConsumer.name);

  constructor(
    @InjectRepository(CardsetOrmEntity)
    private readonly cardsetRepository: Repository<CardsetOrmEntity>,
    @InjectRepository(CardsetManagerOrmEntity)
    private readonly managerRepository: Repository<CardsetManagerOrmEntity>,
    private readonly collaborationGateway: CollaborationGateway,
  ) {}

  @RabbitSubscribe({
    exchange: 'editor-access.exchange',
    routingKey: ['cardset.editor.revoked', 'group.member.kicked'],
    queue: 'cardset.editor-access.queue',
    queueOptions: { durable: true },
  })
  async handle(message: unknown): Promise<void> {
    // Invalid criteria must never become an unfiltered TypeORM delete/query.
    if (!isEditorAccessMessage(message)) {
      this.logger.warn('Discarding invalid editor-access message');
      return;
    }
    if (message.eventType === 'CARDSET_EDITOR_REVOKED') {
      await this.collaborationGateway.revokeEditor(
        String(message.cardsetId),
        String(message.userId),
        message.reason,
      );
      return;
    }

    const cardsets = await this.cardsetRepository.find({
      where: { groupId: message.groupId },
    });
    for (const cardset of cardsets) {
      // Delete by identity and always revoke: a previous attempt may have
      // deleted the row before socket cleanup failed and RabbitMQ redelivered.
      await this.managerRepository.delete({
        cardSetId: cardset.id,
        userId: message.userId,
      });
      await this.collaborationGateway.revokeEditor(
        String(cardset.id),
        String(message.userId),
        message.reason,
      );
    }
    this.logger.log(
      `[그룹 kick 반영] groupId=${message.groupId}, userId=${message.userId}`,
    );
  }
}
