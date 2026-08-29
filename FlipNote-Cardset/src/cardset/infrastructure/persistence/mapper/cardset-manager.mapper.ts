import { CardsetManager } from '../../../domain/model/cardset-manager';
import { CardsetManagerOrmEntity } from '../orm/cardset-manager.orm-entity';

export class CardsetManagerMapper {
  static toDomain(orm: CardsetManagerOrmEntity): CardsetManager {
    return new CardsetManager(orm.id, orm.userId, orm.cardSetId);
  }

  static toOrm(domain: CardsetManager): Partial<CardsetManagerOrmEntity> {
    return {
      id: domain.id || undefined,
      userId: domain.userId,
      cardSetId: domain.cardSetId,
    };
  }
}
