import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { EntityManager, Repository } from 'typeorm';
import type {
  ICardsetRepository,
  CardsetPageOptions,
  CardsetPageResult,
} from '../../domain/repository/cardset.repository';
import { Cardset } from '../../domain/model/cardset';
import { CardsetOrmEntity } from './orm/cardset.orm-entity';
import { CardsetMapper } from './mapper/cardset.mapper';

@Injectable()
export class CardsetRepositoryImpl implements ICardsetRepository {
  constructor(
    @InjectRepository(CardsetOrmEntity)
    private readonly ormRepository: Repository<CardsetOrmEntity>,
  ) {}

  async findAll(): Promise<Cardset[]> {
    const orms = await this.ormRepository.find({
      order: { createdAt: 'DESC' },
    });
    return orms.map((orm) => CardsetMapper.toDomain(orm));
  }

  async findAllPaged(options: CardsetPageOptions): Promise<CardsetPageResult> {
    const {
      page,
      size,
      sortBy = 'createdAt',
      order = 'DESC',
      keyword,
      category,
    } = options;

    const qb = this.ormRepository.createQueryBuilder('cs');

    if (keyword) {
      qb.andWhere('cs.name LIKE :keyword', { keyword: `%${keyword}%` });
    }
    if (category) {
      qb.andWhere('cs.category = :category', { category });
    }

    const allowedSortFields: Record<string, string> = {
      createdAt: 'cs.createdAt',
      name: 'cs.name',
      cardCount: 'cs.cardCount',
    };
    const sortField = allowedSortFields[sortBy] ?? 'cs.createdAt';
    qb.orderBy(sortField, order)
      .skip(page * size)
      .take(size);

    const [orms, total] = await qb.getManyAndCount();
    return { items: orms.map((orm) => CardsetMapper.toDomain(orm)), total };
  }

  async findByGroupId(groupId: number): Promise<Cardset[]> {
    const orms = await this.ormRepository.find({
      where: { groupId },
      order: { createdAt: 'DESC' },
    });
    return orms.map((orm) => CardsetMapper.toDomain(orm));
  }

  async findById(id: number): Promise<Cardset | null> {
    const orm = await this.ormRepository.findOne({ where: { id } });
    return orm ? CardsetMapper.toDomain(orm) : null;
  }

  async findByIds(ids: number[]): Promise<Cardset[]> {
    if (ids.length === 0) return [];
    const orms = await this.ormRepository.findBy(ids.map((id) => ({ id })));
    return orms.map((orm) => CardsetMapper.toDomain(orm));
  }

  async save(cardset: Cardset, manager?: EntityManager): Promise<Cardset> {
    const repo = manager
      ? manager.getRepository(CardsetOrmEntity)
      : this.ormRepository;
    const ormData = CardsetMapper.toOrm(cardset);
    const created = repo.create(ormData);
    const saved = await repo.save(created);
    return CardsetMapper.toDomain(saved);
  }

  async update(id: number, cardset: Partial<Cardset>): Promise<Cardset | null> {
    await this.ormRepository.update(
      id,
      CardsetMapper.toOrm(cardset as Cardset),
    );
    return this.findById(id);
  }

  async delete(id: number): Promise<void> {
    await this.ormRepository.delete(id);
  }
}
