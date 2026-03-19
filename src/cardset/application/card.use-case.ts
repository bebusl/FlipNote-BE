import { Inject, Injectable, Logger } from '@nestjs/common';
import { DataSource } from 'typeorm';
import { Card } from '../domain/model/card';
import { CARD_REPOSITORY } from '../domain/repository/card.repository';
import type { ICardRepository } from '../domain/repository/card.repository';
import { CreateCardRequest } from './dto/request/create-card.request';
import { UpdateCardRequest } from './dto/request/update-card.request';

@Injectable()
export class CardUseCase {
  private readonly logger = new Logger(CardUseCase.name);

  constructor(
    @Inject(CARD_REPOSITORY)
    private readonly cardRepository: ICardRepository,
    private readonly dataSource: DataSource,
  ) {}

  async create(dto: CreateCardRequest): Promise<Card> {
    this.logger.log(
      `[카드 생성] cardsetId=${dto.cardsetId}, content="${dto.content}"`,
    );
    const card = Card.create(dto);
    const result = await this.cardRepository.save(card);
    this.logger.log(
      `[카드 생성 완료] cardId=${result.id}, cardsetId=${result.cardsetId}`,
    );
    return result;
  }

  async findAllByCardsetId(cardsetId: number): Promise<Card[]> {
    return this.cardRepository.findAllByCardsetId(cardsetId);
  }

  async findOne(id: number): Promise<Card | null> {
    return this.cardRepository.findById(id);
  }

  async update(id: number, dto: UpdateCardRequest): Promise<Card | null> {
    this.logger.log(
      `[카드 수정] cardId=${id}, content="${dto.content ?? '변경없음'}"`,
    );
    const result = await this.cardRepository.update(id, dto);
    this.logger.log(
      `[카드 수정 완료] cardId=${id}, updated=${result !== null}`,
    );
    return result;
  }

  async remove(id: number): Promise<void> {
    this.logger.log(`[카드 삭제] cardId=${id}`);
    return this.cardRepository.delete(id);
  }

  async reorderCards(
    cardOrders: { cardId: number; order: number }[],
  ): Promise<void> {
    this.logger.log(
      `[카드 순서 변경] count=${cardOrders.length}, orders=${JSON.stringify(cardOrders)}`,
    );
    await this.dataSource.transaction(async (manager) => {
      for (const { cardId, order } of cardOrders) {
        await this.cardRepository.updateOrder(cardId, order, manager);
      }
    });
    this.logger.log(`[카드 순서 변경 완료] count=${cardOrders.length}`);
  }
}
