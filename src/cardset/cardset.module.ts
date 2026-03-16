import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';

import { CardsetOrmEntity } from './infrastructure/persistence/orm/cardset.orm-entity';
import { CardOrmEntity } from './infrastructure/persistence/orm/card.orm-entity';
import { CardsetManagerOrmEntity } from './infrastructure/persistence/orm/cardset-manager.orm-entity';
import { CardSetMetadataOrmEntity } from './infrastructure/persistence/orm/cardset-metadata.orm-entity';

import { CardsetRepositoryImpl } from './infrastructure/persistence/cardset.repository.impl';
import { CardRepositoryImpl } from './infrastructure/persistence/card.repository.impl';
import { CardsetManagerRepositoryImpl } from './infrastructure/persistence/cardset-manager.repository.impl';
import { CardSetMetadataRepositoryImpl } from './infrastructure/persistence/cardset-metadata.repository.impl';

import { CARDSET_REPOSITORY } from './domain/repository/cardset.repository';
import { CARD_REPOSITORY } from './domain/repository/card.repository';
import { CARDSET_MANAGER_REPOSITORY } from './domain/repository/cardset-manager.repository';
import { CARDSET_METADATA_REPOSITORY } from './domain/repository/cardset-metadata.repository';

import { CardsetCardDomainService } from './domain/service/cardset-card.domain-service';

import { CardsetUseCase } from './application/cardset.use-case';
import { CardUseCase } from './application/card.use-case';

import { CardsetController } from './infrastructure/http/cardset.controller';
import { CardController } from './infrastructure/http/card.controller';
import { GroupCardsetController } from './infrastructure/http/group-cardset.controller';
import { CardsetGrpcController } from './infrastructure/grpc/cardset.grpc-controller';
import { GroupGrpcClient } from './infrastructure/grpc/group-grpc.client';
import { ImageGrpcClient } from './infrastructure/grpc/image-grpc.client';
import { ReactionGrpcClient } from './infrastructure/grpc/reaction-grpc.client';
import { UserGrpcClient } from './infrastructure/grpc/user-grpc.client';
import { GrpcClientModule } from '../shared/grpc/grpc-client.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      CardsetOrmEntity,
      CardOrmEntity,
      CardsetManagerOrmEntity,
      CardSetMetadataOrmEntity,
    ]),
    GrpcClientModule,
  ],
  controllers: [CardsetController, CardController, GroupCardsetController, CardsetGrpcController],
  providers: [
    { provide: CARDSET_REPOSITORY, useClass: CardsetRepositoryImpl },
    { provide: CARD_REPOSITORY, useClass: CardRepositoryImpl },
    {
      provide: CARDSET_MANAGER_REPOSITORY,
      useClass: CardsetManagerRepositoryImpl,
    },
    {
      provide: CARDSET_METADATA_REPOSITORY,
      useClass: CardSetMetadataRepositoryImpl,
    },
    CardsetCardDomainService,
    GroupGrpcClient,
    ImageGrpcClient,
    ReactionGrpcClient,
    UserGrpcClient,
    CardsetUseCase,
    CardUseCase,
  ],
  exports: [CardsetUseCase, CardUseCase],
})
export class CardsetModule {}
