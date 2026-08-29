import { EntityManager } from 'typeorm';
import { Card } from '../model/card';

export const CARD_REPOSITORY = Symbol('CARD_REPOSITORY');

export interface ICardRepository {
  findById(id: number): Promise<Card | null>;
  findAllByCardsetId(cardsetId: number): Promise<Card[]>;
  save(card: Card, manager?: EntityManager): Promise<Card>;
  update(
    id: number,
    card: Partial<Card>,
    manager?: EntityManager,
  ): Promise<Card | null>;
  delete(id: number, manager?: EntityManager): Promise<void>;
  updateOrder(
    cardId: number,
    order: number,
    manager?: EntityManager,
  ): Promise<void>;
}
