import { NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { randomUUID } from 'node:crypto';
import { createConnection, Connection } from 'mysql2/promise';
import { DataSource } from 'typeorm';
import * as Y from 'yjs';
import { CollaborationUseCase } from '../src/collaboration/application/collaboration.use-case';
import { CardsetContentOrmEntity } from '../src/collaboration/infrastructure/persistence/orm/cardset-content.orm-entity';
import { CardsetSnapshotOrmEntity } from '../src/collaboration/infrastructure/persistence/orm/cardset-snapshot.orm-entity';
import { CardsetManagerOrmEntity } from '../src/cardset/infrastructure/persistence/orm/cardset-manager.orm-entity';
import { YjsDocumentService } from '../src/collaboration/infrastructure/redis/yjs-document.service';
import { CardsetSnapshotBackfillService } from '../src/collaboration/application/cardset-snapshot-backfill.service';
import { CardsetOrmEntity } from '../src/cardset/infrastructure/persistence/orm/cardset.orm-entity';
import { CardOrmEntity } from '../src/cardset/infrastructure/persistence/orm/card.orm-entity';
import { CardsetUseCase } from '../src/cardset/application/cardset.use-case';
import { CardsetRepositoryImpl } from '../src/cardset/infrastructure/persistence/cardset.repository.impl';
import { CardsetManagerRepositoryImpl } from '../src/cardset/infrastructure/persistence/cardset-manager.repository.impl';
import { BusinessException } from '../src/shared/common/business.exception';

// Opt in with SNAPSHOT_TEST_MYSQL=true. The DB user needs CREATE/DROP DATABASE.
// Every run owns a new database; existing application tables are never touched.
const describeMySql =
  process.env.SNAPSHOT_TEST_MYSQL === 'true' ? describe : describe.skip;

describeMySql('Cardset snapshot persistence (MySQL)', () => {
  const database = `snapshot_test_${randomUUID().replaceAll('-', '')}`;
  let admin: Connection;
  let dataSource: DataSource;
  let module: TestingModule;
  let useCase: CollaborationUseCase;
  const documents: Y.Doc[] = [];
  const yjs = {
    loadDocument: jest.fn<Promise<Y.Doc | null>, [string]>(),
    saveDocument: jest.fn<Promise<void>, [string, Y.Doc]>(),
    flushIncrementalHistory: jest.fn<Promise<void>, [string]>(),
  };

  function documentWith(cards: unknown[]): Y.Doc {
    const doc = new Y.Doc();
    doc.getArray('cards').insert(0, cards);
    documents.push(doc);
    return doc;
  }

  const originalCards = [
    { id: 'card-a', question: '질문 A', answer: '정답 A' },
    { id: 'card-b', question: '질문 B', answer: '정답 B' },
  ];

  const snapshots = (cardsetId: number) =>
    dataSource.getRepository(CardsetSnapshotOrmEntity).find({
      where: { cardsetId },
      order: { id: 'ASC' },
    });

  beforeAll(async () => {
    const connection = {
      host: process.env.DB_HOST ?? '127.0.0.1',
      port: Number(process.env.DB_PORT ?? 3306),
      user: process.env.DB_USERNAME ?? 'root',
      password: process.env.DB_PASSWORD,
    };
    admin = await createConnection(connection);
    await admin.query(
      `CREATE DATABASE \`${database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`,
    );
    await admin.changeUser({ database });

    dataSource = new DataSource({
      type: 'mysql',
      host: connection.host,
      port: connection.port,
      username: connection.user,
      password: connection.password,
      database,
      entities: [
        CardsetOrmEntity,
        CardOrmEntity,
        CardsetContentOrmEntity,
        CardsetSnapshotOrmEntity,
        CardsetManagerOrmEntity,
      ],
      synchronize: true,
    });
    await dataSource.initialize();
    module = await Test.createTestingModule({
      providers: [
        CollaborationUseCase,
        CardsetSnapshotBackfillService,
        { provide: DataSource, useValue: dataSource },
        { provide: YjsDocumentService, useValue: yjs },
        ...[
          CardsetContentOrmEntity,
          CardsetManagerOrmEntity,
          CardsetSnapshotOrmEntity,
        ].map((entity) => ({
          provide: getRepositoryToken(entity),
          useValue: dataSource.getRepository(entity),
        })),
      ],
    }).compile();
    useCase = module.get(CollaborationUseCase);
  }, 30000);

  beforeEach(() => {
    yjs.loadDocument.mockReset();
    yjs.saveDocument.mockReset().mockResolvedValue(undefined);
    yjs.flushIncrementalHistory.mockReset().mockResolvedValue(undefined);
  });

  afterEach(() => {
    for (const doc of documents.splice(0)) doc.destroy();
  });

  afterAll(async () => {
    await module?.close();
    if (dataSource?.isInitialized) await dataSource.destroy();
    if (admin) {
      await admin.query(`DROP DATABASE IF EXISTS \`${database}\``);
      await admin.end();
    }
  });

  it('preserves the first snapshot after cards are edited or removed', async () => {
    const updatedCards = [
      { id: 'card-a', question: '수정한 질문', answer: '수정한 정답' },
    ];
    yjs.loadDocument
      .mockResolvedValueOnce(documentWith(originalCards))
      .mockResolvedValueOnce(documentWith(updatedCards));

    await useCase.saveCardsetContent(101);
    await useCase.saveCardsetContent(101);

    const rows = await snapshots(101);
    expect(rows).toHaveLength(2);
    expect(rows[0].content).toBe(JSON.stringify({ cards: originalCards }));
    expect(rows[1].content).toBe(JSON.stringify({ cards: updatedCards }));
    expect(rows[1].id).toBeGreaterThan(rows[0].id);
    expect(rows[0].createdAt).toBeInstanceOf(Date);
    // Readers must select the latest saved snapshot.
    expect(await useCase.getCardsFromDB(101)).toEqual(updatedCards);
    const restored = await useCase.loadCardsetContentFromDB(101);
    expect(restored?.getArray('cards').toJSON()).toEqual(updatedCards);
    restored?.destroy();
  });

  it('appends a snapshot on every save, including identical and empty content', async () => {
    yjs.loadDocument.mockResolvedValue(documentWith([]));
    await useCase.saveCardsetContent(102);
    await useCase.saveCardsetContent(102);
    const rows = await snapshots(102);
    expect(rows).toHaveLength(2);
    expect(rows[0].id).not.toBe(rows[1].id);
    expect(rows.map((row) => row.content)).toEqual([
      '{"cards":[]}',
      '{"cards":[]}',
    ]);
    expect(await useCase.getCardsFromDB(102)).toEqual([]);
    const restored = await useCase.loadCardsetContentFromDB(102);
    expect(restored).toBeInstanceOf(Y.Doc);
    expect(restored?.getArray('cards').toJSON()).toEqual([]);
    restored?.destroy();
  });

  it('leaves existing content intact when the Redis document is missing', async () => {
    await dataSource.getRepository(CardsetContentOrmEntity).insert({
      cardsetId: 103,
      content: JSON.stringify({ cards: originalCards }),
    });
    yjs.loadDocument.mockResolvedValue(null);
    await expect(useCase.saveCardsetContent(103)).rejects.toBeInstanceOf(
      NotFoundException,
    );
    expect(await snapshots(103)).toEqual([]);
    const legacy = await dataSource
      .getRepository(CardsetContentOrmEntity)
      .findOneByOrFail({ cardsetId: 103 });
    expect(legacy.content).toBe(JSON.stringify({ cards: originalCards }));
    // Legacy backfill is task 4; reads no longer fall back to this row.
    expect(await useCase.getCardsFromDB(103)).toEqual([]);
    expect(await useCase.loadCardsetContentFromDB(103)).toBeNull();
    expect(yjs.flushIncrementalHistory).not.toHaveBeenCalled();
  });

  it('rolls back the latest content if the snapshot insert fails', async () => {
    yjs.loadDocument.mockResolvedValue(documentWith(originalCards));
    await useCase.saveCardsetContent(104);
    const before = await snapshots(104);
    yjs.flushIncrementalHistory.mockClear();
    yjs.loadDocument.mockResolvedValue(documentWith([]));
    // A real MySQL error after the legacy upsert, not a mocked transaction.
    await admin.query(`
      CREATE TRIGGER reject_snapshot BEFORE INSERT ON cardset_snapshot
      FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'test insert failure'
    `);
    try {
      await expect(useCase.saveCardsetContent(104)).rejects.toThrow(
        'test insert failure',
      );
    } finally {
      await admin.query('DROP TRIGGER reject_snapshot');
    }
    expect(await snapshots(104)).toEqual(before);
    const legacy = await dataSource
      .getRepository(CardsetContentOrmEntity)
      .findOneByOrFail({ cardsetId: 104 });
    expect(legacy.content).toBe(JSON.stringify({ cards: originalCards }));
    expect(await useCase.getCardsFromDB(104)).toEqual(originalCards);
    expect(yjs.flushIncrementalHistory).not.toHaveBeenCalled();
  });

  it('serializes concurrent first saves and keeps latest content aligned with the highest ID', async () => {
    const contents = Array.from({ length: 8 }, (_, index) =>
      JSON.stringify({
        cards: [{ id: 'card-a', question: `질문 ${index}`, answer: '답변' }],
      }),
    );
    for (let index = 0; index < contents.length; index++) {
      yjs.loadDocument.mockResolvedValueOnce(
        documentWith([
          { id: 'card-a', question: `질문 ${index}`, answer: '답변' },
        ]),
      );
    }
    await Promise.all(contents.map(() => useCase.saveCardsetContent(105)));

    const rows = await snapshots(105);
    expect(rows).toHaveLength(contents.length);
    expect(rows.map((row) => row.content).sort()).toEqual(contents.sort());
    const current = await dataSource
      .getRepository(CardsetContentOrmEntity)
      .findOneByOrFail({ cardsetId: 105 });
    expect(current.content).toBe(rows[rows.length - 1].content);
  });

  it('commits both records before flushing incremental history', async () => {
    yjs.loadDocument.mockResolvedValue(documentWith(originalCards));
    yjs.flushIncrementalHistory.mockImplementation(async (id) => {
      expect(id).toBe('106');
      // Independent connection: only committed rows are visible here.
      const [rows] = await admin.query(
        'SELECT s.id FROM cardset_snapshot s JOIN cardset_content c ON c.cardset_id = s.cardset_id AND c.content = s.content WHERE s.cardset_id = 106',
      );
      expect(rows).toHaveLength(1);
    });
    await useCase.saveCardsetContent(106);
    expect(yjs.flushIncrementalHistory).toHaveBeenCalledTimes(1);
  });

  it('keeps snapshots when the legacy current-content row is deleted', async () => {
    yjs.loadDocument.mockResolvedValue(documentWith(originalCards));
    await useCase.saveCardsetContent(107);
    await dataSource.getRepository(CardsetContentOrmEntity).delete({
      cardsetId: 107,
    });
    expect(await snapshots(107)).toHaveLength(1);
    expect(await useCase.getCardsFromDB(107)).toEqual(originalCards);
    const restored = await useCase.loadCardsetContentFromDB(107);
    expect(restored?.getArray('cards').toJSON()).toEqual(originalCards);
    restored?.destroy();
    // Full cardset deletion and legacy backfill remain task 4.
  });

  it('reads the highest snapshot ID for this cardset despite conflicting legacy content and timestamps', async () => {
    const updatedCards = [
      { id: 'card-a', question: '최신 질문', answer: '최신 정답' },
    ];
    const snapshotRepository = dataSource.getRepository(
      CardsetSnapshotOrmEntity,
    );
    await snapshotRepository.insert({
      cardsetId: 108,
      content: JSON.stringify({ cards: originalCards }),
      createdAt: new Date('2026-09-22T12:00:00Z'),
    });
    await snapshotRepository.insert({
      cardsetId: 108,
      content: JSON.stringify({ cards: updatedCards }),
      createdAt: new Date('2026-09-21T12:00:00Z'),
    });
    // A different cardset has the globally highest ID.
    await snapshotRepository.insert({
      cardsetId: 109,
      content: JSON.stringify({ cards: [] }),
    });
    await dataSource.getRepository(CardsetContentOrmEntity).insert({
      cardsetId: 108,
      content: JSON.stringify({ cards: originalCards }),
    });

    expect(await useCase.getCardsFromDB(108)).toEqual(updatedCards);
    const restored = await useCase.loadCardsetContentFromDB(108);
    expect(restored?.getArray('cards').toJSON()).toEqual(updatedCards);
    restored?.destroy();
  });

  it('returns no saved content for a cardset without snapshots', async () => {
    expect(await useCase.getCardsFromDB(110)).toEqual([]);
    expect(await useCase.loadCardsetContentFromDB(110)).toBeNull();
  });

  it('restores a saved snapshot into Redis when the editing document is missing', async () => {
    await dataSource.getRepository(CardsetSnapshotOrmEntity).insert({
      cardsetId: 111,
      content: JSON.stringify({ cards: originalCards }),
    });
    yjs.loadDocument.mockResolvedValue(null);

    const restored = await useCase.getOrCreateDocument(111);
    expect(restored.getArray('cards').toJSON()).toEqual(originalCards);
    expect(yjs.saveDocument).toHaveBeenCalledWith('111', restored);
    restored.destroy();
  });

  describe('legacy backfill and cardset deletion', () => {
    const legacyRepository = () =>
      dataSource.getRepository(CardsetContentOrmEntity);
    const backfill = () =>
      module.get(CardsetSnapshotBackfillService).backfill();

    beforeEach(async () => {
      // Backfill scans the whole legacy table, so isolate each scenario.
      for (const entity of [
        CardsetSnapshotOrmEntity,
        CardsetContentOrmEntity,
      ]) {
        await dataSource
          .getRepository(entity)
          .createQueryBuilder()
          .delete()
          .execute();
      }
    });

    it('backfills at application bootstrap and preserves exact JSON and the last save time on restart', async () => {
      const content = JSON.stringify(
        { cards: originalCards, extra: 'preserved' },
        null,
        2,
      );
      const savedAt = new Date('2026-09-20T12:34:56Z');
      await legacyRepository().insert({
        cardsetId: 201,
        content,
        updatedAt: savedAt,
      });

      await module.init();
      const before = await snapshots(201);
      expect(before).toHaveLength(1);
      expect(before[0].content).toBe(content);
      expect(before[0].createdAt).toEqual(savedAt);
      expect(await useCase.getCardsFromDB(201)).toEqual(originalCards);
      const restored = await useCase.loadCardsetContentFromDB(201);
      expect(restored?.getArray('cards').toJSON()).toEqual(originalCards);
      restored?.destroy();
      expect(await backfill()).toBe(0);
      expect(await snapshots(201)).toEqual(before);
      expect(
        (await legacyRepository().findOneByOrFail({ cardsetId: 201 })).content,
      ).toBe(content);
    });

    it('does not overwrite existing snapshots with legacy content', async () => {
      await legacyRepository().insert({
        cardsetId: 202,
        content: JSON.stringify({ cards: originalCards }),
      });
      await dataSource.getRepository(CardsetSnapshotOrmEntity).insert({
        cardsetId: 202,
        content: '{"cards":[]}',
      });
      const before = await snapshots(202);
      expect(await backfill()).toBe(0);
      expect(await snapshots(202)).toEqual(before);
      expect(await useCase.getCardsFromDB(202)).toEqual([]);
    });

    it('migrates saved empty cards but skips NULL and empty legacy content', async () => {
      await legacyRepository().insert([
        { cardsetId: 203, content: '{"cards":[]}' },
        { cardsetId: 204 },
        { cardsetId: 205, content: '' },
      ]);
      expect(await backfill()).toBe(1);
      const restored = await useCase.loadCardsetContentFromDB(203);
      expect(restored).toBeInstanceOf(Y.Doc);
      expect(restored?.getArray('cards').toJSON()).toEqual([]);
      restored?.destroy();
      for (const id of [204, 205]) {
        expect(await snapshots(id)).toEqual([]);
        expect(await useCase.getCardsFromDB(id)).toEqual([]);
        expect(await useCase.loadCardsetContentFromDB(id)).toBeNull();
      }
    });

    it.each([
      ['malformed JSON', '{'],
      ['missing cards', '{}'],
      ['non-array cards', '{"cards":{}}'],
      ['non-object card', '{"cards":[null]}'],
      ['numeric card ID', '{"cards":[{"id":1,"question":"q","answer":"a"}]}'],
      ['empty card ID', '{"cards":[{"id":" ","question":"q","answer":"a"}]}'],
      ['invalid fields', '{"cards":[{"id":"a","question":1,"answer":"a"}]}'],
      [
        'duplicate IDs',
        JSON.stringify({ cards: [originalCards[0], originalCards[0]] }),
      ],
    ])(
      'stops on %s without changing source data and resumes without duplicates',
      async (_label, content) => {
        await legacyRepository().insert([
          { cardsetId: 206, content: JSON.stringify({ cards: originalCards }) },
          { cardsetId: 207, content },
        ]);
        await expect(
          module.get(CardsetSnapshotBackfillService).onApplicationBootstrap(),
        ).rejects.toThrow('cardsetId=207');
        const before = await snapshots(206);
        expect(before).toHaveLength(1);
        expect(await snapshots(207)).toEqual([]);
        expect(
          (await legacyRepository().findOneByOrFail({ cardsetId: 207 }))
            .content,
        ).toBe(content);

        await legacyRepository().update(
          { cardsetId: 207 },
          { content: '{"cards":[]}' },
        );
        expect(await backfill()).toBe(1);
        expect(await snapshots(206)).toEqual(before);
        expect(await snapshots(207)).toHaveLength(1);
      },
    );

    it('serializes concurrent backfills using the legacy row lock', async () => {
      await legacyRepository().insert({
        cardsetId: 208,
        content: JSON.stringify({ cards: originalCards }),
      });
      const counts = await Promise.all([backfill(), backfill()]);
      expect(counts.reduce((sum, count) => sum + count, 0)).toBe(1);
      expect(await snapshots(208)).toHaveLength(1);
    });

    it('keeps the explicit save as the latest snapshot when it races with backfill', async () => {
      await legacyRepository().insert({
        cardsetId: 209,
        content: JSON.stringify({ cards: originalCards }),
      });
      yjs.loadDocument.mockResolvedValue(documentWith([]));
      await Promise.all([backfill(), useCase.saveCardsetContent(209)]);
      const rows = await snapshots(209);
      expect(rows.length).toBeGreaterThanOrEqual(1);
      expect(rows.length).toBeLessThanOrEqual(2);
      expect(rows[rows.length - 1].content).toBe('{"cards":[]}');
      expect(await useCase.getCardsFromDB(209)).toEqual([]);
      expect(await backfill()).toBe(0);
    });

    it('continues beyond one batch and keeps orphaned legacy history', async () => {
      await legacyRepository().insert(
        Array.from({ length: 101 }, (_, index) => ({
          cardsetId: 300 + index,
          content: '{"cards":[]}',
        })),
      );
      expect(await backfill()).toBe(101);
      expect(await snapshots(400)).toHaveLength(1);
      expect(await backfill()).toBe(0);
    });

    it.each(['backfilled', 'explicitly saved'])(
      'preserves %s snapshots through the real cardset deletion path',
      async (mode) => {
        const cardsets = new CardsetRepositoryImpl(
          dataSource.getRepository(CardsetOrmEntity),
        );
        const managers = new CardsetManagerRepositoryImpl(
          dataSource.getRepository(CardsetManagerOrmEntity),
        );
        const cardsetUseCase = new CardsetUseCase(
          ...([
            cardsets,
            {},
            managers,
            {},
            {},
            {},
            {},
            {},
            dataSource,
            {},
            useCase,
            {},
          ] as unknown as ConstructorParameters<typeof CardsetUseCase>),
        );
        const cardset = await dataSource.getRepository(CardsetOrmEntity).save({
          name: 'snapshot retention',
          groupId: 1,
          category: 'test',
        });
        await dataSource
          .getRepository(CardOrmEntity)
          .insert({ cardsetId: cardset.id, content: 'legacy card' });
        await dataSource
          .getRepository(CardsetManagerOrmEntity)
          .insert({ cardSetId: cardset.id, userId: 1 });
        if (mode === 'backfilled') {
          await legacyRepository().insert({
            cardsetId: cardset.id,
            content: JSON.stringify({ cards: originalCards }),
          });
          await backfill();
        } else {
          yjs.loadDocument.mockResolvedValue(documentWith(originalCards));
          await useCase.saveCardsetContent(cardset.id);
        }
        const before = await snapshots(cardset.id);
        expect(before).toHaveLength(1);
        await expect(
          cardsetUseCase.remove(cardset.id, 999),
        ).rejects.toBeInstanceOf(BusinessException);
        expect(await cardsets.findById(cardset.id)).not.toBeNull();

        await cardsetUseCase.remove(cardset.id, 1);
        expect(await cardsets.findById(cardset.id)).toBeNull();
        expect(
          await dataSource
            .getRepository(CardOrmEntity)
            .countBy({ cardsetId: cardset.id }),
        ).toBe(0);
        expect(await snapshots(cardset.id)).toEqual(before);
        expect(await useCase.getCardsFromDB(cardset.id)).toEqual(originalCards);
        expect(await backfill()).toBe(0);
        expect(await snapshots(cardset.id)).toEqual(before);
        // Retaining history does not make a deleted cardset readable via its API.
        await expect(
          cardsetUseCase.findCardsFromYjs(cardset.id, 1),
        ).rejects.toBeInstanceOf(BusinessException);
      },
    );
  });
});
