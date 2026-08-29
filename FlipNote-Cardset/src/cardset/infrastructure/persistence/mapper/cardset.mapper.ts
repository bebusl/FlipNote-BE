import { Cardset } from '../../../domain/model/cardset';
import { CardsetOrmEntity } from '../orm/cardset.orm-entity';

export class CardsetMapper {
  static toDomain(orm: CardsetOrmEntity): Cardset {
    return new Cardset(
      orm.id,
      orm.name,
      orm.groupId,
      orm.visibility,
      orm.category,
      orm.hashtag ?? null,
      orm.imageRefId != null && Number(orm.imageRefId) !== 0
        ? Number(orm.imageRefId)
        : null,
      orm.cardCount,
      orm.createdAt,
      orm.updatedAt,
      orm.deletedAt,
    );
  }

  static toOrm(domain: Cardset): Partial<CardsetOrmEntity> {
    return {
      id: domain.id || undefined,
      name: domain.name,
      groupId: domain.groupId,
      visibility: domain.visibility,
      category: domain.category,
      hashtag: domain.hashtag,
      imageRefId: domain.imageRefId,
      cardCount: domain.cardCount,
    };
  }
}
