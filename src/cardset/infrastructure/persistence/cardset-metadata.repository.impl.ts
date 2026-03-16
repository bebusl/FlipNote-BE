import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ICardSetMetadataRepository } from '../../domain/repository/cardset-metadata.repository';
import { CardSetMetadataOrmEntity } from './orm/cardset-metadata.orm-entity';

@Injectable()
export class CardSetMetadataRepositoryImpl
  implements ICardSetMetadataRepository
{
  constructor(
    @InjectRepository(CardSetMetadataOrmEntity)
    private readonly ormRepository: Repository<CardSetMetadataOrmEntity>,
  ) {}

  async findByCardSetId(
    cardSetId: number,
  ): Promise<{ likeCount: number; bookmarkCount: number } | null> {
    const orm = await this.ormRepository.findOne({ where: { cardSetId } });
    if (!orm) return null;
    return {
      likeCount: Number(orm.likeCount),
      bookmarkCount: Number(orm.bookmarkCount),
    };
  }

  async findByCardSetIds(
    cardSetIds: number[],
  ): Promise<Map<number, { likeCount: number; bookmarkCount: number }>> {
    if (cardSetIds.length === 0) return new Map();
    const orms = await this.ormRepository
      .createQueryBuilder('m')
      .where('m.cardSetId IN (:...ids)', { ids: cardSetIds })
      .getMany();
    const map = new Map<number, { likeCount: number; bookmarkCount: number }>();
    for (const orm of orms) {
      map.set(Number(orm.cardSetId), {
        likeCount: Number(orm.likeCount),
        bookmarkCount: Number(orm.bookmarkCount),
      });
    }
    return map;
  }

  async upsertAndIncrementLike(cardSetId: number): Promise<void> {
    await this.ormRepository.query(
      `INSERT INTO card_set_metadata (card_set_id, like_count, bookmark_count)
       VALUES (?, 1, 0)
       ON DUPLICATE KEY UPDATE like_count = like_count + 1`,
      [cardSetId],
    );
  }

  async upsertAndDecrementLike(cardSetId: number): Promise<void> {
    await this.ormRepository.query(
      `UPDATE card_set_metadata
       SET like_count = GREATEST(like_count - 1, 0)
       WHERE card_set_id = ?`,
      [cardSetId],
    );
  }

  async upsertAndIncrementBookmark(cardSetId: number): Promise<void> {
    await this.ormRepository.query(
      `INSERT INTO card_set_metadata (card_set_id, like_count, bookmark_count)
       VALUES (?, 0, 1)
       ON DUPLICATE KEY UPDATE bookmark_count = bookmark_count + 1`,
      [cardSetId],
    );
  }

  async upsertAndDecrementBookmark(cardSetId: number): Promise<void> {
    await this.ormRepository.query(
      `UPDATE card_set_metadata
       SET bookmark_count = GREATEST(bookmark_count - 1, 0)
       WHERE card_set_id = ?`,
      [cardSetId],
    );
  }
}
