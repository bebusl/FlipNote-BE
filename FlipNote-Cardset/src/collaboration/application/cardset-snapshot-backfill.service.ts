import { Injectable, Logger, OnApplicationBootstrap } from '@nestjs/common';
import { DataSource, MoreThan } from 'typeorm';
import { CardsetContentOrmEntity } from '../infrastructure/persistence/orm/cardset-content.orm-entity';
import { CardsetSnapshotOrmEntity } from '../infrastructure/persistence/orm/cardset-snapshot.orm-entity';

@Injectable()
export class CardsetSnapshotBackfillService implements OnApplicationBootstrap {
  private readonly logger = new Logger(CardsetSnapshotBackfillService.name);

  constructor(private readonly dataSource: DataSource) {}

  async onApplicationBootstrap(): Promise<void> {
    const count = await this.backfill();
    this.logger.log(`Backfilled ${count} cardset snapshots`);
  }

  // Each row commits independently, so a failed run can safely resume.
  async backfill(): Promise<number> {
    let afterId = 0;
    let inserted = 0;
    const repository = this.dataSource.getRepository(CardsetContentOrmEntity);

    for (;;) {
      const batch = await repository.find({
        select: { id: true },
        where: { id: MoreThan(afterId) },
        order: { id: 'ASC' },
        take: 100,
      });
      if (batch.length === 0) return inserted;

      for (const row of batch) {
        inserted += await this.dataSource.transaction(async (manager) => {
          // Use the same lock as explicit saves. Other instances may backfill
          // concurrently, so check for a snapshot only after obtaining the lock.
          const legacy = await manager.findOne(CardsetContentOrmEntity, {
            where: { id: row.id },
            lock: { mode: 'pessimistic_write' },
          });
          if (!legacy?.content) return 0;

          const existing = await manager.findOne(CardsetSnapshotOrmEntity, {
            where: { cardsetId: legacy.cardsetId },
            order: { id: 'DESC' },
          });
          if (existing) return 0;

          this.validate(legacy);
          await manager.insert(CardsetSnapshotOrmEntity, {
            cardsetId: legacy.cardsetId,
            content: legacy.content,
            createdAt: legacy.updatedAt,
          });
          return 1;
        });
        afterId = row.id;
      }
    }
  }

  private validate(legacy: CardsetContentOrmEntity): void {
    const fail = (reason: string): never => {
      throw new Error(
        `Cannot backfill cardset_content id=${legacy.id}, cardsetId=${legacy.cardsetId}: ${reason}. Repair the source row and restart; it has not been modified.`,
      );
    };
    let value: unknown;
    try {
      value = JSON.parse(legacy.content);
    } catch {
      fail('invalid JSON');
    }
    if (!value || typeof value !== 'object' || !('cards' in value)) {
      fail('expected an object with a cards array');
    }
    const cards = (value as { cards: unknown }).cards;
    if (!Array.isArray(cards)) fail('expected a cards array');

    const ids = new Set<string>();
    for (const [index, card] of (cards as unknown[]).entries()) {
      if (!card || typeof card !== 'object')
        fail(`card ${index} is not an object`);
      const fields = card as Record<string, unknown>;
      if (typeof fields.id !== 'string' || fields.id.trim().length === 0) {
        fail(`card ${index} requires a nonempty string id`);
      }
      if (
        typeof fields.question !== 'string' ||
        typeof fields.answer !== 'string'
      ) {
        fail(`card ${index} requires string question and answer`);
      }
      const id = fields.id as string;
      if (ids.has(id)) fail(`card ${index} has a duplicate id`);
      ids.add(id);
    }
  }
}
