import { Module } from '@nestjs/common';
import { RabbitMQModule } from '@golevelup/nestjs-rabbitmq';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ReactionConsumer } from './reaction.consumer';
import { CardSetMetadataOrmEntity } from '../cardset/infrastructure/persistence/orm/cardset-metadata.orm-entity';
import { CardSetMetadataRepositoryImpl } from '../cardset/infrastructure/persistence/cardset-metadata.repository.impl';
import { CARDSET_METADATA_REPOSITORY } from '../cardset/domain/repository/cardset-metadata.repository';

@Module({
  imports: [
    RabbitMQModule.forRoot({
      exchanges: [{ name: 'reaction.exchange', type: 'topic' }],
      uri: process.env.RABBITMQ_URL ?? 'amqp://guest:guest@localhost:5672',
      connectionInitOptions: { wait: false },
    }),
    TypeOrmModule.forFeature([CardSetMetadataOrmEntity]),
  ],
  providers: [
    { provide: CARDSET_METADATA_REPOSITORY, useClass: CardSetMetadataRepositoryImpl },
    ReactionConsumer,
  ],
  exports: [CARDSET_METADATA_REPOSITORY],
})
export class ReactionModule {}
