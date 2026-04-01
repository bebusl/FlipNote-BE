import {
  Injectable,
  OnModuleInit,
  OnModuleDestroy,
  Logger,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import Redis from 'ioredis';
import * as Y from 'yjs';
import { Repository } from 'typeorm';
import { CardsetIncrementalOrmEntity } from '../persistence/orm/cardset-incremental.orm-entity';

@Injectable()
export class YjsDocumentService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(YjsDocumentService.name);
  private redisClient!: Redis;
  private readonly mysqlFlushDelayMs: number;
  private readonly flushTimeouts = new Map<string, NodeJS.Timeout>();

  constructor(
    private readonly configService: ConfigService,
    @InjectRepository(CardsetIncrementalOrmEntity)
    private readonly cardsetIncrementalRepository: Repository<CardsetIncrementalOrmEntity>,
  ) {
    this.mysqlFlushDelayMs =
      this.configService.get<number>('YJS_MYSQL_FLUSH_DELAY_MS') ?? 60000;
  }

  onModuleInit() {
    this.redisClient = new Redis({
      host: this.configService.get<string>('REDIS_HOST') ?? 'localhost',
      port: this.configService.get<number>('REDIS_PORT') ?? 6379,
      password: this.configService.get<string>('REDIS_PASSWORD'),
      retryStrategy: (times) => Math.min(times * 50, 2000),
    });

    this.redisClient.on('connect', () => {
      this.logger.log('Redis connected successfully');
    });

    this.redisClient.on('error', (error) => {
      this.logger.error('Redis connection error:', error);
    });
  }

  onModuleDestroy() {
    if (this.redisClient) {
      this.redisClient.disconnect();
      this.logger.log('Redis disconnected');
    }
    this.flushTimeouts.forEach((timeout) => clearTimeout(timeout));
    this.flushTimeouts.clear();
  }

  async saveDocument(cardsetId: string, doc: Y.Doc): Promise<void> {
    try {
      if (!this.redisClient) return;
      const state = Y.encodeStateAsUpdate(doc);
      const key = `yjs:cardset:${cardsetId}`;
      this.logger.log(
        `Saving document to Redis, key: ${key}, state size: ${state.byteLength} bytes`,
      );
      await this.redisClient.set(key, Buffer.from(state));
      await this.redisClient.expire(key, 86400 * 7);
      this.logger.log(`Saved document to Redis successfully, key: ${key}`);
    } catch (error) {
      this.logger.error(
        `Failed to save document for cardset ${cardsetId}:`,
        error,
      );
    }
  }

  async loadDocument(cardsetId: string): Promise<Y.Doc | null> {
    try {
      if (!this.redisClient) return null;
      const data = await this.redisClient.getBuffer(`yjs:cardset:${cardsetId}`);
      if (!data) return null;
      const doc = new Y.Doc();
      Y.applyUpdate(doc, data);
      return doc;
    } catch (error) {
      this.logger.error(
        `Failed to load document for cardset ${cardsetId}:`,
        error,
      );
      return null;
    }
  }

  async saveUpdate(cardsetId: string, update: Uint8Array): Promise<Uint8Array> {
    try {
      if (!this.redisClient) return update;
      const key = `yjs:cardset:${cardsetId}`;
      const historyKey = `yjs:cardset:${cardsetId}:updates`;

      const doc = new Y.Doc();
      const existingData = await this.redisClient.getBuffer(key);
      if (existingData) {
        Y.applyUpdate(doc, existingData);
      }
      Y.applyUpdate(doc, update);
      const newState = Y.encodeStateAsUpdate(doc);
      this.logger.log(
        `[saveUpdate] Redis 저장 전 doc 내용 - cardsetId=${cardsetId}, cards=${JSON.stringify(doc.getArray('cards').toJSON())}`,
      );
      await this.redisClient.set(key, Buffer.from(newState));
      await this.redisClient.expire(key, 86400 * 7);
      await this.redisClient.rpush(historyKey, Buffer.from(update).toString('base64'));

      this.scheduleMySqlPersistence(cardsetId);
      return newState;
    } catch (error) {
      this.logger.error(
        `Failed to save update for cardset ${cardsetId}:`,
        error,
      );
      throw error;
    }
  }

  async flushIncrementalHistory(cardsetId: string): Promise<void> {
    await this.persistCardsetIncrementals(cardsetId, true);
  }

  async registerClient(cardsetId: string, clientId: string): Promise<void> {
    try {
      const cardsetKey = `yjs:cardset:${cardsetId}:clients`;
      const clientKey = `yjs:client:${clientId}:cardsets`;
      await Promise.all([
        this.redisClient.sadd(cardsetKey, clientId),
        this.redisClient.sadd(clientKey, cardsetId),
        this.redisClient.expire(cardsetKey, 86400),
        this.redisClient.expire(clientKey, 86400),
      ]);
    } catch (error) {
      this.logger.error(
        `Failed to register client ${clientId} for cardset ${cardsetId}:`,
        error,
      );
    }
  }

  async unregisterClient(cardsetId: string, clientId: string): Promise<void> {
    try {
      await Promise.all([
        this.redisClient.srem(`yjs:cardset:${cardsetId}:clients`, clientId),
        this.redisClient.srem(`yjs:client:${clientId}:cardsets`, cardsetId),
      ]);
    } catch (error) {
      this.logger.error(
        `Failed to unregister client ${clientId} from cardset ${cardsetId}:`,
        error,
      );
    }
  }

  async getActiveClientCount(cardsetId: string): Promise<number> {
    try {
      return await this.redisClient.scard(`yjs:cardset:${cardsetId}:clients`);
    } catch {
      return 0;
    }
  }

  async getClientCardsets(clientId: string): Promise<string[]> {
    try {
      return await this.redisClient.smembers(`yjs:client:${clientId}:cardsets`);
    } catch {
      return [];
    }
  }

  async deleteDocument(cardsetId: string): Promise<void> {
    try {
      await this.redisClient.del(`yjs:cardset:${cardsetId}`);
    } catch (error) {
      this.logger.error(
        `Failed to delete document for cardset ${cardsetId}:`,
        error,
      );
      throw error;
    }
  }

  private scheduleMySqlPersistence(cardsetId: string) {
    const existing = this.flushTimeouts.get(cardsetId);
    if (existing) clearTimeout(existing);

    const timeout = setTimeout(() => {
      void this.persistCardsetIncrementals(cardsetId, false);
    }, this.mysqlFlushDelayMs);
    this.flushTimeouts.set(cardsetId, timeout);
  }

  private async persistCardsetIncrementals(
    cardsetId: string,
    markAsFlushed: boolean,
  ): Promise<void> {
    this.flushTimeouts.delete(cardsetId);

    const numericCardsetId = Number(cardsetId);
    if (Number.isNaN(numericCardsetId)) {
      this.logger.error(
        `Cannot persist cardset ${cardsetId}: invalid numeric id`,
      );
      return;
    }

    const historyKey = `yjs:cardset:${cardsetId}:updates`;
    try {
      const updates = await this.redisClient.lrange(historyKey, 0, -1);
      if (updates.length === 0) return;

      const entities = updates.map((base64Value) =>
        this.cardsetIncrementalRepository.create({
          cardsetId: numericCardsetId,
          incrementalValue: Buffer.from(base64Value, 'base64'),
          isFlushed: markAsFlushed,
        }),
      );
      await this.cardsetIncrementalRepository.save(entities);
      await this.redisClient.del(historyKey);

      this.logger.log(
        `Persisted ${entities.length} incremental updates for cardset ${cardsetId}`,
      );
    } catch (error) {
      this.logger.error(
        `Failed to persist incremental updates for cardset ${cardsetId}:`,
        error,
      );
    }
  }
}
