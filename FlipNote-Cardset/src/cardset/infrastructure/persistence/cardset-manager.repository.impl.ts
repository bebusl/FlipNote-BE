import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { EntityManager, Repository } from 'typeorm';
import { ICardsetManagerRepository } from '../../domain/repository/cardset-manager.repository';
import { CardsetManager } from '../../domain/model/cardset-manager';
import { CardsetManagerOrmEntity } from './orm/cardset-manager.orm-entity';
import { CardsetManagerMapper } from './mapper/cardset-manager.mapper';

@Injectable()
export class CardsetManagerRepositoryImpl implements ICardsetManagerRepository {
  constructor(
    @InjectRepository(CardsetManagerOrmEntity)
    private readonly ormRepository: Repository<CardsetManagerOrmEntity>,
  ) {}

  async save(
    cardsetManager: CardsetManager,
    manager?: EntityManager,
  ): Promise<CardsetManager> {
    const repo = manager
      ? manager.getRepository(CardsetManagerOrmEntity)
      : this.ormRepository;
    const ormData = CardsetManagerMapper.toOrm(cardsetManager);
    const created = repo.create(ormData);
    const saved = await repo.save(created);
    return CardsetManagerMapper.toDomain(saved);
  }

  async findByUserIdAndCardSetId(
    userId: number,
    cardSetId: number,
  ): Promise<CardsetManager | null> {
    const orm = await this.ormRepository.findOne({
      where: { userId, cardSetId },
    });
    return orm ? CardsetManagerMapper.toDomain(orm) : null;
  }

  async findAllByCardSetId(cardSetId: number): Promise<CardsetManager[]> {
    const orms = await this.ormRepository.find({ where: { cardSetId } });
    return orms.map((orm) => CardsetManagerMapper.toDomain(orm));
  }

  async findByCardSetIds(cardSetIds: number[]): Promise<CardsetManager[]> {
    if (cardSetIds.length === 0) return [];
    const orms = await this.ormRepository
      .createQueryBuilder('m')
      .where('m.cardSetId IN (:...cardSetIds)', { cardSetIds })
      .getMany();
    return orms.map((orm) => CardsetManagerMapper.toDomain(orm));
  }

  async delete(id: number): Promise<void> {
    await this.ormRepository.delete(id);
  }
}
