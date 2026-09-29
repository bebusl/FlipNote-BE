import { CardsetUseCase } from './cardset.use-case';
import { EntityManager } from 'typeorm';

function setup() {
  const manager = {} as EntityManager;
  let committed = false;
  const cardsets = {
    findById: jest.fn().mockResolvedValue({ id: 42, groupId: 10 }),
    update: jest.fn().mockResolvedValue({ id: 42 }),
  };
  const managers = {
    findByUserIdAndCardSetId: jest.fn().mockResolvedValue({ userId: 1 }),
    findAllByCardSetId: jest.fn().mockResolvedValue([
      { id: 11, userId: 1 },
      { id: 12, userId: 7 },
    ]),
    delete: jest.fn().mockResolvedValue(undefined),
    save: jest.fn().mockResolvedValue(undefined),
  };
  const dataSource = {
    transaction: jest.fn(
      async (work: (manager: EntityManager) => Promise<unknown>) => {
        const result = await work(manager);
        committed = true;
        return result;
      },
    ),
  };
  const amqp = {
    publish: jest.fn().mockImplementation(() => {
      expect(committed).toBe(true);
      return Promise.resolve();
    }),
  };
  const group = { checkUserInGroup: jest.fn().mockResolvedValue(undefined) };
  const useCase = new CardsetUseCase(
    ...([
      cardsets,
      {},
      managers,
      {},
      group,
      {},
      {},
      {},
      dataSource,
      {},
      {},
      amqp,
    ] as unknown as ConstructorParameters<typeof CardsetUseCase>),
  );
  return { useCase, manager, cardsets, managers, amqp };
}

describe('manager revocation publishing', () => {
  it('uses the same transaction for replacement and publishes only removed users after commit', async () => {
    const f = setup();
    await f.useCase.update(42, 1, { managerIds: [1] });
    expect(f.managers.findAllByCardSetId).toHaveBeenCalledWith(42, f.manager);
    expect(f.managers.delete).toHaveBeenCalledWith(12, f.manager);
    expect(f.managers.save).toHaveBeenCalledWith(
      expect.objectContaining({ userId: 1 }),
      f.manager,
    );
    expect(f.cardsets.update).toHaveBeenCalledWith(
      42,
      { managerIds: [1] },
      f.manager,
    );
    expect(f.amqp.publish).toHaveBeenCalledTimes(1);
    expect(f.amqp.publish).toHaveBeenCalledWith(
      'editor-access.exchange',
      'cardset.editor.revoked',
      {
        eventType: 'CARDSET_EDITOR_REVOKED',
        cardsetId: 42,
        userId: 7,
        reason: 'MANAGER_REMOVED',
      },
    );
  });

  it('does not publish if the transaction fails after manager deletion', async () => {
    const f = setup();
    f.cardsets.update.mockRejectedValueOnce(new Error('DB failure'));
    await expect(f.useCase.update(42, 1, { managerIds: [1] })).rejects.toThrow(
      'DB failure',
    );
    expect(f.amqp.publish).not.toHaveBeenCalled();
  });

  it('does not revoke unchanged managers', async () => {
    const f = setup();
    await f.useCase.update(42, 1, { managerIds: [1, 7] });
    expect(f.amqp.publish).not.toHaveBeenCalled();
  });
});
