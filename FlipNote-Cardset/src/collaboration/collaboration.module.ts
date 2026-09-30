import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MessagingModule } from '../shared/messaging/messaging.module';

import { CardsetContentOrmEntity } from './infrastructure/persistence/orm/cardset-content.orm-entity';
import { CardsetSnapshotOrmEntity } from './infrastructure/persistence/orm/cardset-snapshot.orm-entity';
import { CardsetIncrementalOrmEntity } from './infrastructure/persistence/orm/cardset-incremental.orm-entity';
import { CardsetManagerOrmEntity } from '../cardset/infrastructure/persistence/orm/cardset-manager.orm-entity';
import { YjsDocumentService } from './infrastructure/redis/yjs-document.service';
import { CollaborationUseCase } from './application/collaboration.use-case';
import { CardsetSnapshotBackfillService } from './application/cardset-snapshot-backfill.service';
import { CollaborationGateway } from './infrastructure/gateway/collaboration.gateway';
import { AuthModule } from '../auth/auth.module';
import { UserGrpcClient } from '../cardset/infrastructure/grpc/user-grpc.client';
import { GrpcClientModule } from '../shared/grpc/grpc-client.module';
import { CardsetOrmEntity } from '../cardset/infrastructure/persistence/orm/cardset.orm-entity';
import { EditorAccessConsumer } from './infrastructure/messaging/editor-access.consumer';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      CardsetOrmEntity,
      CardsetContentOrmEntity,
      CardsetSnapshotOrmEntity,
      CardsetIncrementalOrmEntity,
      CardsetManagerOrmEntity,
    ]),
    MessagingModule,
    AuthModule,
    GrpcClientModule,
  ],
  providers: [
    YjsDocumentService,
    CollaborationUseCase,
    CardsetSnapshotBackfillService,
    CollaborationGateway,
    UserGrpcClient,
    EditorAccessConsumer,
  ],
  exports: [CollaborationUseCase, MessagingModule],
})
export class CollaborationModule {}
