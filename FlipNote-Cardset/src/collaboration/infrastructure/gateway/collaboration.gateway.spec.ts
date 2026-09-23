import { Logger } from '@nestjs/common';
import { Server, Socket } from 'socket.io';
import * as Y from 'yjs';
import { CollaborationGateway } from './collaboration.gateway';
import { YjsDocumentService } from '../redis/yjs-document.service';
import { CollaborationUseCase } from '../../application/collaboration.use-case';

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

const user = { userId: '7', nickname: 'tester' };
function socket(id = 'socket-1', userId = '7') {
  const rooms = new Set<string>();
  return {
    id,
    data: { user: { ...user, userId } },
    rooms,
    join: jest.fn((room: string) => {
      rooms.add(room);
    }),
    leave: jest.fn((room: string) => {
      rooms.delete(room);
    }),
    emit: jest.fn(),
    to: jest.fn(() => ({ emit: jest.fn() })),
  };
}
function setup() {
  const client = socket();
  const service = {
    registerClient: jest.fn().mockResolvedValue(undefined),
    unregisterClient: jest.fn().mockResolvedValue(undefined),
    getUserClientIds: jest.fn().mockResolvedValue([]),
    getActiveClientCount: jest.fn().mockResolvedValue(1),
    loadDocument: jest.fn().mockResolvedValue(new Y.Doc()),
    saveUpdate: jest.fn().mockResolvedValue(new Uint8Array([0, 0])),
  };
  const useCase = { isManager: jest.fn().mockResolvedValue(true) };
  const gateway = new CollaborationGateway(
    service as unknown as YjsDocumentService,
    useCase as unknown as CollaborationUseCase,
  );
  const server = {
    sockets: { sockets: new Map([[client.id, client]]) },
    to: jest.fn(() => ({ emit: jest.fn() })),
  };
  gateway.server = server as unknown as Server;
  return { gateway, client, service, useCase, server };
}
const asSocket = (client: ReturnType<typeof socket>) =>
  client as unknown as Socket;

describe('editor session revocation', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.spyOn(Logger.prototype, 'log').mockImplementation(() => undefined);
    jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);
    jest.spyOn(Logger.prototype, 'error').mockImplementation(() => undefined);
  });
  afterEach(() => {
    jest.clearAllTimers();
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('rejects updates and awareness before joining', async () => {
    const f = setup();
    await f.gateway.handleUpdate(user, asSocket(f.client), {
      cardsetId: '42',
      update: [0, 0],
    });
    f.gateway.handleAwareness(user, asSocket(f.client), {
      cardsetId: '42',
      awareness: [0],
    });
    expect(f.service.saveUpdate).not.toHaveBeenCalled();
    expect(f.client.to).not.toHaveBeenCalled();
  });

  it('preserves authenticated user information in awareness after joining', async () => {
    const f = setup();
    await f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    f.gateway.handleAwareness(user, asSocket(f.client), {
      cardsetId: '42',
      awareness: [1, 2],
    });
    expect(f.client.to).toHaveBeenCalledWith('cardset:42');
    const broadcast = f.client.to.mock.results[0].value as ReturnType<
      typeof f.client.to
    >;
    expect(broadcast.emit).toHaveBeenCalledWith('awareness', {
      data: {
        cardsetId: '42',
        awareness: new Uint8Array([1, 2]),
        userId: user.userId,
        userName: user.nickname,
      },
    });
  });

  it('does not admit a non-manager', async () => {
    const f = setup();
    f.useCase.isManager.mockResolvedValue(false);
    await f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    expect(f.client.join).not.toHaveBeenCalled();
    expect(f.service.registerClient).not.toHaveBeenCalled();
    expect(f.client.emit).not.toHaveBeenCalledWith('sync', expect.anything());
  });

  it('revokes every tab, blocks further edits, and preserves other users and rooms', async () => {
    const f = setup();
    const tab = socket('socket-2');
    const other = socket('socket-3', '8');
    f.server.sockets.sockets.set(tab.id, tab);
    f.server.sockets.sockets.set(other.id, other);
    for (const client of [f.client, tab, other]) {
      await f.gateway.handleJoinCardset(client.data.user, asSocket(client), {
        cardsetId: '42',
      });
    }
    await f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '43',
    });
    await f.gateway.revokeEditor('42', '7', 'MANAGER_REMOVED');
    for (const client of [f.client, tab]) {
      expect(client.emit).toHaveBeenCalledWith('kicked', {
        cardsetId: '42',
        reason: 'MANAGER_REMOVED',
      });
      expect(client.rooms.has('cardset:42')).toBe(false);
    }
    expect(f.client.rooms.has('cardset:43')).toBe(true);
    expect(other.rooms.has('cardset:42')).toBe(true);
    await f.gateway.handleUpdate(user, asSocket(f.client), {
      cardsetId: '42',
      update: [0, 0],
    });
    f.gateway.handleAwareness(user, asSocket(f.client), {
      cardsetId: '42',
      awareness: [0],
    });
    expect(f.service.saveUpdate).not.toHaveBeenCalled();
    expect(f.client.to).not.toHaveBeenCalled();
    await f.gateway.handleUpdate(user, asSocket(f.client), {
      cardsetId: '43',
      update: [0, 0],
    });
    expect(f.service.saveUpdate).toHaveBeenCalledWith(
      '43',
      new Uint8Array([0, 0]),
    );
  });

  it('discards buffered updates and initial sync when kicked during document loading', async () => {
    const f = setup(),
      loading = deferred<Y.Doc>(),
      started = deferred<void>();
    f.service.loadDocument.mockImplementation(() => {
      started.resolve();
      return loading.promise;
    });
    const join = f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    await started.promise;
    await f.gateway.handleUpdate(user, asSocket(f.client), {
      cardsetId: '42',
      update: [0, 0],
    });
    await f.gateway.revokeEditor('42', '7', 'MANAGER_REMOVED');
    loading.resolve(new Y.Doc());
    await join;
    expect(f.service.saveUpdate).not.toHaveBeenCalled();
    expect(f.client.emit).not.toHaveBeenCalledWith('sync', expect.anything());
  });

  it('cancels a join whose permission lookup was in flight at revocation', async () => {
    const f = setup(),
      permission = deferred<boolean>();
    f.useCase.isManager.mockReturnValue(permission.promise);
    const join = f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    await f.gateway.revokeEditor('42', '7', 'MANAGER_REMOVED');
    permission.resolve(true);
    await join;
    expect(f.client.join).not.toHaveBeenCalled();
    expect(f.service.registerClient).not.toHaveBeenCalled();
  });

  it('cancels pending joins on disconnect', async () => {
    const f = setup(),
      permission = deferred<boolean>();
    f.useCase.isManager.mockReturnValue(permission.promise);
    const join = f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    await f.gateway.handleDisconnect(asSocket(f.client));
    permission.resolve(true);
    await join;
    expect(f.client.join).not.toHaveBeenCalled();
  });

  it('cleans up a late Redis registration after revocation', async () => {
    const f = setup(),
      registration = deferred<void>(),
      started = deferred<void>();
    f.service.registerClient.mockImplementation(() => {
      started.resolve();
      return registration.promise;
    });
    const join = f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    await started.promise;
    await f.gateway.revokeEditor('42', '7', 'MANAGER_REMOVED');
    f.service.unregisterClient.mockClear();
    registration.resolve();
    await join;
    expect(f.service.unregisterClient).toHaveBeenCalledWith(
      '42',
      f.client.id,
      '7',
    );
    expect(f.service.loadDocument).not.toHaveBeenCalled();
  });

  it('stops draining queued edits when revoked during a save', async () => {
    const f = setup(),
      loading = deferred<Y.Doc>(),
      loaded = deferred<void>();
    const saving = deferred<Uint8Array>(),
      saveStarted = deferred<void>();
    f.service.loadDocument.mockImplementation(() => {
      loaded.resolve();
      return loading.promise;
    });
    f.service.saveUpdate.mockImplementation(() => {
      saveStarted.resolve();
      return saving.promise;
    });
    const join = f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    await loaded.promise;
    for (let i = 0; i < 2; i++)
      await f.gateway.handleUpdate(user, asSocket(f.client), {
        cardsetId: '42',
        update: [0, 0],
      });
    loading.resolve(new Y.Doc());
    await saveStarted.promise;
    await f.gateway.revokeEditor('42', '7', 'MANAGER_REMOVED');
    saving.resolve(new Uint8Array([0, 0]));
    await join;
    expect(f.service.saveUpdate).toHaveBeenCalledTimes(1);
    expect(f.server.to).not.toHaveBeenCalled();
  });

  it('revokes even if the Redis user index has expired', async () => {
    const f = setup();
    await f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    await f.gateway.revokeEditor('42', '7', 'GROUP_MEMBER_KICKED');
    expect(f.client.rooms.has('cardset:42')).toBe(false);
    expect(f.service.getUserClientIds).not.toHaveBeenCalled();
  });

  it('keeps buffers separate for simultaneous joins to different cardsets', async () => {
    const f = setup(),
      loading = deferred<Y.Doc>(),
      started = deferred<void>();
    f.service.loadDocument.mockImplementation((id: string) => {
      if (id === '42') {
        started.resolve();
        return loading.promise;
      }
      return Promise.resolve(new Y.Doc());
    });
    const join = f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    await started.promise;
    await f.gateway.handleUpdate(user, asSocket(f.client), {
      cardsetId: '42',
      update: [0, 0],
    });
    await f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '43',
    });
    expect(f.service.saveUpdate).not.toHaveBeenCalled();
    loading.resolve(new Y.Doc());
    await join;
    expect(f.service.saveUpdate).toHaveBeenCalledTimes(1);
    expect(f.service.saveUpdate).toHaveBeenCalledWith(
      '42',
      new Uint8Array([0, 0]),
    );
  });

  it('cleans up editing state when joining fails', async () => {
    const f = setup();
    f.service.loadDocument.mockRejectedValue(new Error('unavailable'));
    await f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '42',
    });
    await f.gateway.handleUpdate(user, asSocket(f.client), {
      cardsetId: '42',
      update: [0, 0],
    });
    expect(f.client.rooms.has('cardset:42')).toBe(false);
    expect(f.service.saveUpdate).not.toHaveBeenCalled();
  });

  it('canonicalizes cardset IDs so aliases cannot evade revocation', async () => {
    const f = setup();
    await f.gateway.handleJoinCardset(user, asSocket(f.client), {
      cardsetId: '042',
    });
    await f.gateway.revokeEditor('42', '7', 'MANAGER_REMOVED');
    expect(f.client.rooms.size).toBe(0);
    expect(f.client.emit).toHaveBeenCalledWith('kicked', {
      cardsetId: '42',
      reason: 'MANAGER_REMOVED',
    });
  });
});
