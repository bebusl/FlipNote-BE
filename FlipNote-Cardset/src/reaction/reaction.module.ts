import { Module } from '@nestjs/common';
import { MessagingModule } from '../shared/messaging/messaging.module';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ReactionConsumer } from './reaction.consumer';
import { CardSetMetadataOrmEntity } from '../cardset/infrastructure/persistence/orm/cardset-metadata.orm-entity';
import { CardSetMetadataRepositoryImpl } from '../cardset/infrastructure/persistence/cardset-metadata.repository.impl';
import { CARDSET_METADATA_REPOSITORY } from '../cardset/domain/repository/cardset-metadata.repository';

@Module({
  imports: [
    MessagingModule,
    TypeOrmModule.forFeature([CardSetMetadataOrmEntity]),
  ],
  providers: [
    {
      provide: CARDSET_METADATA_REPOSITORY,
      useClass: CardSetMetadataRepositoryImpl,
    },
    ReactionConsumer,
  ],
  exports: [CARDSET_METADATA_REPOSITORY],
})
export class ReactionModule {}
