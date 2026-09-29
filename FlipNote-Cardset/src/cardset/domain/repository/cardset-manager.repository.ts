import { EntityManager } from 'typeorm';
import { CardsetManager } from '../model/cardset-manager';

export const CARDSET_MANAGER_REPOSITORY = Symbol('CARDSET_MANAGER_REPOSITORY');

export interface ICardsetManagerRepository {
  save(
    cardsetManager: CardsetManager,
    manager?: EntityManager,
  ): Promise<CardsetManager>;
  findByUserIdAndCardSetId(
    userId: number,
    cardSetId: number,
  ): Promise<CardsetManager | null>;
  findAllByCardSetId(
    cardSetId: number,
    manager?: EntityManager,
  ): Promise<CardsetManager[]>;
  findByCardSetIds(cardSetIds: number[]): Promise<CardsetManager[]>;
  delete(id: number, manager?: EntityManager): Promise<void>;
}
