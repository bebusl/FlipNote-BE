import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';

import { CardsetContentOrmEntity } from './infrastructure/persistence/orm/cardset-content.orm-entity';
import { CardsetIncrementalOrmEntity } from './infrastructure/persistence/orm/cardset-incremental.orm-entity';
import { YjsDocumentService } from './infrastructure/redis/yjs-document.service';
import { CollaborationUseCase } from './application/collaboration.use-case';
import { CollaborationGateway } from './infrastructure/gateway/collaboration.gateway';
import { AuthModule } from '../auth/auth.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      CardsetContentOrmEntity,
      CardsetIncrementalOrmEntity,
    ]),
    AuthModule,
  ],
  providers: [YjsDocumentService, CollaborationUseCase, CollaborationGateway],
  exports: [CollaborationUseCase],
})
export class CollaborationModule {}
