import { Module, Logger } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { AmqpConnection } from '@golevelup/nestjs-rabbitmq';
import { MessagingModule } from './messaging.module';

@Module({ imports: [MessagingModule] })
class FirstConsumerModule {}
@Module({ imports: [MessagingModule] })
class SecondConsumerModule {}

describe('shared messaging connection', () => {
  it('shares one connection with both exchanges when imported by multiple consumers', async () => {
    const initialize = jest
      .spyOn(AmqpConnection.prototype, 'init')
      .mockResolvedValue(undefined);
    jest.spyOn(AmqpConnection.prototype, 'close').mockResolvedValue(undefined);
    jest.spyOn(Logger.prototype, 'log').mockImplementation(() => undefined);
    try {
      const module = await Test.createTestingModule({
        imports: [FirstConsumerModule, SecondConsumerModule],
      }).compile();
      try {
        expect(initialize).toHaveBeenCalledTimes(1);
        expect(module.get(AmqpConnection).configuration.exchanges).toEqual(
          expect.arrayContaining([
            { name: 'reaction.exchange', type: 'topic' },
            { name: 'reaction.dlx', type: 'direct' },
            { name: 'editor-access.exchange', type: 'topic' },
          ]),
        );
      } finally {
        await module.close();
      }
    } finally {
      jest.restoreAllMocks();
    }
  });
});
