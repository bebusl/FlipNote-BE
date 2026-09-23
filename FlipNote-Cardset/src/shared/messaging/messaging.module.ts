import { Module } from '@nestjs/common';
import { RabbitMQModule } from '@golevelup/nestjs-rabbitmq';

// One connection owns both reaction and editor-access topology and consumers.
@Module({
  imports: [
    RabbitMQModule.forRoot({
      exchanges: [
        { name: 'reaction.exchange', type: 'topic' },
        { name: 'editor-access.exchange', type: 'topic' },
        { name: 'reaction.dlx', type: 'direct' },
      ],
      queues: [
        {
          name: 'cardset.reaction.queue',
          options: {
            durable: true,
            arguments: {
              'x-dead-letter-exchange': 'reaction.dlx',
              'x-dead-letter-routing-key': 'cardset.reaction.dead',
            },
          },
        },
        {
          name: 'cardset.reaction.dlq',
          options: { durable: true },
          exchange: 'reaction.dlx',
          routingKey: 'cardset.reaction.dead',
        },
      ],
      uri: process.env.RABBITMQ_URL ?? 'amqp://guest:guest@localhost:5672',
      connectionInitOptions: { wait: false },
    }),
  ],
  exports: [RabbitMQModule],
})
export class MessagingModule {}
