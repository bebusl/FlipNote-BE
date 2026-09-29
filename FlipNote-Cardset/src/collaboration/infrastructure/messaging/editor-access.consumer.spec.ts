import { Logger } from '@nestjs/common';
import { Repository } from 'typeorm';
import { EditorAccessConsumer } from './editor-access.consumer';
import { CardsetOrmEntity } from '../../../cardset/infrastructure/persistence/orm/cardset.orm-entity';
import { CardsetManagerOrmEntity } from '../../../cardset/infrastructure/persistence/orm/cardset-manager.orm-entity';
import { CollaborationGateway } from '../gateway/collaboration.gateway';

describe('EditorAccessConsumer', () => {
  function setup() {
    const cardsets = {
      find: jest.fn().mockResolvedValue([{ id: 42 }, { id: 43 }]),
    };
    const managers = { delete: jest.fn().mockResolvedValue({ affected: 0 }) };
    const gateway = { revokeEditor: jest.fn().mockResolvedValue(undefined) };
    const consumer = new EditorAccessConsumer(
      cardsets as unknown as Repository<CardsetOrmEntity>,
      managers as unknown as Repository<CardsetManagerOrmEntity>,
      gateway as unknown as CollaborationGateway,
    );
    return { cardsets, managers, gateway, consumer };
  }
  const event = {
    eventType: 'GROUP_MEMBER_KICKED' as const,
    groupId: 10,
    userId: 7,
    reason: 'GROUP_MEMBER_KICKED',
  };
  beforeEach(() => {
    jest.spyOn(Logger.prototype, 'log').mockImplementation(() => undefined);
    jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);
  });
  afterEach(() => jest.restoreAllMocks());

  it('retries socket revocation even when the manager row was already deleted', async () => {
    const f = setup();
    f.gateway.revokeEditor.mockRejectedValueOnce(
      new Error('temporary failure'),
    );
    await expect(f.consumer.handle(event)).rejects.toThrow('temporary failure');
    await f.consumer.handle(event);
    expect(f.gateway.revokeEditor.mock.calls).toEqual([
      ['42', '7', event.reason],
      ['42', '7', event.reason],
      ['43', '7', event.reason],
    ]);
    expect(f.managers.delete).toHaveBeenCalledWith({
      cardSetId: 42,
      userId: 7,
    });
    expect(f.managers.delete).toHaveBeenCalledWith({
      cardSetId: 43,
      userId: 7,
    });
    expect(f.cardsets.find).toHaveBeenCalledWith({ where: { groupId: 10 } });
  });

  it('does not acknowledge DB failure or revoke before manager deletion succeeds', async () => {
    const f = setup();
    f.managers.delete.mockRejectedValueOnce(new Error('DB unavailable'));
    await expect(f.consumer.handle(event)).rejects.toThrow('DB unavailable');
    expect(f.gateway.revokeEditor).not.toHaveBeenCalled();
  });

  it.each([
    null,
    {},
    { ...event, groupId: undefined },
    { ...event, userId: undefined },
    { ...event, eventType: 'UNKNOWN' },
  ])(
    'discards malformed messages without changing permissions: %j',
    async (message) => {
      const f = setup();
      await f.consumer.handle(message);
      expect(f.cardsets.find).not.toHaveBeenCalled();
      expect(f.managers.delete).not.toHaveBeenCalled();
      expect(f.gateway.revokeEditor).not.toHaveBeenCalled();
    },
  );

  it('routes manager removal directly to the target cardset and user', async () => {
    const f = setup();
    await f.consumer.handle({
      eventType: 'CARDSET_EDITOR_REVOKED',
      cardsetId: 42,
      userId: 7,
      reason: 'MANAGER_REMOVED',
    });
    expect(f.gateway.revokeEditor).toHaveBeenCalledWith(
      '42',
      '7',
      'MANAGER_REMOVED',
    );
    expect(f.cardsets.find).not.toHaveBeenCalled();
    expect(f.managers.delete).not.toHaveBeenCalled();
  });
});
