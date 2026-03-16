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
    return doc
      .getArray<{ id: string; question: string; answer: string }>('cards')
      .toArray();
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
    const doc = await this.yjsDocumentService.loadDocument(
      cardSetId.toString(),
    );
    if (!doc) {
      throw new NotFoundException('Cardset snapshot not found in Redis');
    }

    const jsonContent = JSON.stringify(doc.toJSON() ?? {});

    let content = await this.cardsetContentRepository.findOne({
      where: { cardsetId: cardSetId },
    });
    if (!content) {
      content = this.cardsetContentRepository.create({
        cardsetId: cardSetId,
        content: '',
      });
    }
    content.content = jsonContent;
    await this.cardsetContentRepository.save(content);

    await this.yjsDocumentService.flushIncrementalHistory(cardSetId.toString());
    this.logger.log(`Saved cardset ${cardSetId} content to DB`);
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
        const yMap = doc.getMap('content');
        for (const [key, value] of Object.entries(jsonContent)) {
          yMap.set(key, value);
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
