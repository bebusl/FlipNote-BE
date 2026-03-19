import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as Y from 'yjs';
import { YjsDocumentService } from '../infrastructure/redis/yjs-document.service';
import { CardsetContentOrmEntity } from '../infrastructure/persistence/orm/cardset-content.orm-entity';

@Injectable()
export class CollaborationUseCase {
  private readonly logger = new Logger(CollaborationUseCase.name);

  constructor(
    private readonly yjsDocumentService: YjsDocumentService,
    @InjectRepository(CardsetContentOrmEntity)
    private readonly cardsetContentRepository: Repository<CardsetContentOrmEntity>,
  ) {}

  async getOrCreateDocument(cardsetId: number): Promise<Y.Doc> {
    const fromRedis = await this.yjsDocumentService.loadDocument(
      cardsetId.toString(),
    );
    if (fromRedis) return fromRedis;

    const fromDb = await this.loadCardsetContentFromDB(cardsetId);
    if (fromDb) {
      await this.yjsDocumentService.saveDocument(cardsetId.toString(), fromDb);
      return fromDb;
    }

    const doc = new Y.Doc();
    await this.yjsDocumentService.saveDocument(cardsetId.toString(), doc);
    return doc;
  }

  async applyUpdate(cardsetId: number, update: number[]): Promise<Uint8Array> {
    const doc = await this.getOrCreateDocument(cardsetId);
    const updateArr = new Uint8Array(update);
    Y.applyUpdate(doc, updateArr);
    await this.yjsDocumentService.saveUpdate(cardsetId.toString(), updateArr);
    return Y.encodeStateAsUpdate(doc);
  }

  async getState(cardsetId: number): Promise<Uint8Array> {
    const doc = await this.getOrCreateDocument(cardsetId);
    return Y.encodeStateAsUpdate(doc);
  }

  async getCards(
    cardsetId: number,
  ): Promise<{ id: string; question: string; answer: string }[]> {
    const doc = await this.getOrCreateDocument(cardsetId);
    const raw = doc.getArray('cards').toArray();
    return JSON.parse(JSON.stringify(raw)) as {
      id: string;
      question: string;
      answer: string;
    }[];
  }

  async syncCardsFromDB(
    cardsetId: number,
    cards: {
      id: number;
      content: string;
      order: number;
      cardsetId: number;
      createdAt: { toISOString(): string } | null;
      updatedAt: { toISOString(): string } | null;
    }[],
  ): Promise<void> {
    const doc = await this.getOrCreateDocument(cardsetId);
    const cardsArray = doc.getArray('cards');

    cardsArray.delete(0, cardsArray.length);

    const yjsCards = cards.map((card) => ({
      id: card.id,
      content: card.content,
      order: card.order,
      cardsetId: card.cardsetId,
      createdAt: card.createdAt?.toISOString(),
      updatedAt: card.updatedAt?.toISOString(),
    }));

    cardsArray.insert(0, yjsCards);
    await this.yjsDocumentService.saveDocument(cardsetId.toString(), doc);
    this.logger.log(
      `Synced ${cards.length} cards to Yjs for cardset ${cardsetId}`,
    );
  }

  async saveCardsetContent(cardSetId: number): Promise<void> {
    this.logger.log(`[saveCardsetContent 시작] cardSetId=${cardSetId}`);

    const doc = await this.yjsDocumentService.loadDocument(
      cardSetId.toString(),
    );
    if (!doc) {
      this.logger.warn(
        `[saveCardsetContent] Redis에 doc 없음 - cardSetId=${cardSetId}`,
      );
      throw new NotFoundException('Cardset snapshot not found in Redis');
    }

    const docJson = doc.toJSON() ?? {};
    const cardCount = Array.isArray(docJson['cards'])
      ? docJson['cards'].length
      : 0;
    const jsonContent = JSON.stringify(docJson);
    this.logger.log(
      `[saveCardsetContent] doc.toJSON() 완료 - cardSetId=${cardSetId}, cardCount=${cardCount}, contentLength=${jsonContent.length}`,
    );

    let content = await this.cardsetContentRepository.findOne({
      where: { cardsetId: cardSetId },
    });
    const isNew = !content;
    if (!content) {
      content = this.cardsetContentRepository.create({
        cardsetId: cardSetId,
        content: '',
      });
    }
    this.logger.log(
      `[saveCardsetContent] DB 레코드 ${isNew ? '신규 생성' : '기존 업데이트'} - cardSetId=${cardSetId}`,
    );

    content.content = jsonContent;
    this.logger.log(
      `[saveCardsetContent] DB 저장 시작 - cardSetId=${cardSetId}, contentLength=${jsonContent.length}`,
    );
    await this.cardsetContentRepository.save(content);
    this.logger.log(
      `[saveCardsetContent] DB 저장 완료 - cardSetId=${cardSetId}`,
    );

    await this.yjsDocumentService.flushIncrementalHistory(cardSetId.toString());
    this.logger.log(
      `[saveCardsetContent] incremental history flush 완료 - cardSetId=${cardSetId}`,
    );
  }

  async loadCardsetContentFromDB(cardSetId: number): Promise<Y.Doc | null> {
    try {
      const cardsetContent = await this.cardsetContentRepository.findOne({
        where: { cardsetId: cardSetId },
      });

      if (!cardsetContent || !cardsetContent.content) return null;

      const jsonContent = JSON.parse(cardsetContent.content) as Record<
        string,
        unknown
      >;
      const doc = new Y.Doc();
      if (jsonContent && typeof jsonContent === 'object') {
        const cards = jsonContent['cards'];
        if (Array.isArray(cards) && cards.length > 0) {
          doc.getArray('cards').insert(0, cards);
        }
      }
      return doc;
    } catch {
      return null;
    }
  }

  async deleteDocument(cardsetId: number): Promise<void> {
    await this.cardsetContentRepository.delete({ cardsetId });
    await this.yjsDocumentService.saveDocument(
      cardsetId.toString(),
      new Y.Doc(),
    );
    this.logger.log(`Deleted Yjs document for cardset ${cardsetId}`);
  }
}
