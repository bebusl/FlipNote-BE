import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { EntityManager, Repository } from 'typeorm';
import { ICardRepository } from '../../domain/repository/card.repository';
import { Card } from '../../domain/model/card';
import { CardOrmEntity } from './orm/card.orm-entity';
import { CardMapper } from './mapper/card.mapper';

@Injectable()
export class CardRepositoryImpl implements ICardRepository {
  constructor(
    @InjectRepository(CardOrmEntity)
    private readonly ormRepository: Repository<CardOrmEntity>,
  ) {}

  async findById(id: number): Promise<Card | null> {
    const orm = await this.ormRepository.findOne({ where: { id } });
    return orm ? CardMapper.toDomain(orm) : null;
  }

  async findAllByCardsetId(cardsetId: number): Promise<Card[]> {
    const orms = await this.ormRepository.find({
      where: { cardsetId },
      order: { order: 'ASC' },
    });
    return orms.map((orm) => CardMapper.toDomain(orm));
  }

  async save(card: Card, manager?: EntityManager): Promise<Card> {
    const repo = manager
      ? manager.getRepository(CardOrmEntity)
      : this.ormRepository;
    const ormData = CardMapper.toOrm(card);
    const created = repo.create(ormData);
    const saved = await repo.save(created);
    return CardMapper.toDomain(saved);
  }

  async update(
    id: number,
    card: Partial<Card>,
    manager?: EntityManager,
  ): Promise<Card | null> {
    const repo = manager
      ? manager.getRepository(CardOrmEntity)
      : this.ormRepository;
    await repo.update(id, CardMapper.toOrm(card as Card));
    return this.findById(id);
  }

  async delete(id: number, manager?: EntityManager): Promise<void> {
    const repo = manager
      ? manager.getRepository(CardOrmEntity)
      : this.ormRepository;
    await repo.delete(id);
  }

  async updateOrder(
    cardId: number,
    order: number,
    manager?: EntityManager,
  ): Promise<void> {
    const repo = manager
      ? manager.getRepository(CardOrmEntity)
      : this.ormRepository;
    await repo.update(cardId, { order });
  }
}
