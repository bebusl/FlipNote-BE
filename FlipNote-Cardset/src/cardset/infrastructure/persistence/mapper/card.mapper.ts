import { Card } from '../../../domain/model/card';
import { CardOrmEntity } from '../orm/card.orm-entity';

export class CardMapper {
  static toDomain(orm: CardOrmEntity): Card {
    return new Card(
      orm.id,
      orm.content,
      orm.order,
      orm.cardsetId,
      orm.createdAt,
      orm.updatedAt,
      orm.deletedAt,
    );
  }

  static toOrm(domain: Card): Partial<CardOrmEntity> {
    return {
      id: domain.id || undefined,
      content: domain.content,
      order: domain.order,
      cardsetId: domain.cardsetId,
    };
  }
}
