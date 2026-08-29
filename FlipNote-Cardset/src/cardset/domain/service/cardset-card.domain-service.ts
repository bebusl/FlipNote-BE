import { Injectable } from '@nestjs/common';
import { Card } from '../model/card';

/**
 * 카드셋과 카드 사이의 도메인 규칙을 담당
 * - 카드 수 조정 시 어떤 카드를 추가/삭제할지 결정
 */
@Injectable()
export class CardsetCardDomainService {
  buildCardsToAdd(
    cardsetId: number,
    currentCount: number,
    targetCount: number,
  ): Card[] {
    const cards: Card[] = [];
    for (let i = currentCount; i < targetCount; i++) {
      cards.push(Card.create({ content: '', order: i, cardsetId }));
    }
    return cards;
  }

  selectCardsToRemove(cards: Card[], targetCount: number): Card[] {
    return cards.slice(targetCount);
  }
}
