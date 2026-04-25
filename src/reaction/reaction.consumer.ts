import { Injectable, Logger } from '@nestjs/common';
import { RabbitSubscribe } from '@golevelup/nestjs-rabbitmq';
import type { ICardSetMetadataRepository } from '../cardset/domain/repository/cardset-metadata.repository';
import { CARDSET_METADATA_REPOSITORY } from '../cardset/domain/repository/cardset-metadata.repository';
import { Inject } from '@nestjs/common';

interface ReactionMessage {
  eventType:
    | 'LIKE_ADDED'
    | 'LIKE_REMOVED'
    | 'BOOKMARK_ADDED'
    | 'BOOKMARK_REMOVED';
  targetType: string;
  targetId: number;
  userId: number;
}

@Injectable()
export class ReactionConsumer {
  private readonly logger = new Logger(ReactionConsumer.name);

  constructor(
    @Inject(CARDSET_METADATA_REPOSITORY)
    private readonly metadataRepository: ICardSetMetadataRepository,
  ) {}

  @RabbitSubscribe({
    exchange: 'reaction.exchange',
    routingKey: [
      'reaction.like.added',
      'reaction.like.removed',
      'reaction.bookmark.added',
      'reaction.bookmark.removed',
    ],
    queue: 'cardset.reaction.queue',
    queueOptions: {
      durable: true,
      arguments: {
        'x-dead-letter-exchange': 'reaction.dlx',
        'x-dead-letter-routing-key': 'cardset.reaction.dead',
      },
    },
  })
  async handleReaction(msg: ReactionMessage): Promise<void> {
    if (msg.targetType !== 'CARD_SET') return;

    const cardSetId = Number(msg.targetId);
    this.logger.log(
      `Reaction event: ${msg.eventType} for cardSetId=${cardSetId}`,
    );

    try {
      switch (msg.eventType) {
        case 'LIKE_ADDED':
          await this.metadataRepository.upsertAndIncrementLike(cardSetId);
          break;
        case 'LIKE_REMOVED':
          await this.metadataRepository.upsertAndDecrementLike(cardSetId);
          break;
        case 'BOOKMARK_ADDED':
          await this.metadataRepository.upsertAndIncrementBookmark(cardSetId);
          break;
        case 'BOOKMARK_REMOVED':
          await this.metadataRepository.upsertAndDecrementBookmark(cardSetId);
          break;
      }
    } catch (err) {
      this.logger.error(
        `Reaction 처리 실패 - eventType=${msg.eventType}, cardSetId=${cardSetId}: ${err instanceof Error ? err.message : String(err)}`,
      );
      throw err; // nack → DLQ로 이동
    }
  }
}
