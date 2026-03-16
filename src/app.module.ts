import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AuthModule } from './auth/auth.module';
import { CardsetModule } from './cardset/cardset.module';
import { CollaborationModule } from './collaboration/collaboration.module';
import { ReactionModule } from './reaction/reaction.module';

import { CardsetOrmEntity } from './cardset/infrastructure/persistence/orm/cardset.orm-entity';
import { CardOrmEntity } from './cardset/infrastructure/persistence/orm/card.orm-entity';
import { CardsetManagerOrmEntity } from './cardset/infrastructure/persistence/orm/cardset-manager.orm-entity';
import { CardSetMetadataOrmEntity } from './cardset/infrastructure/persistence/orm/cardset-metadata.orm-entity';
import { CardsetContentOrmEntity } from './collaboration/infrastructure/persistence/orm/cardset-content.orm-entity';
import { CardsetIncrementalOrmEntity } from './collaboration/infrastructure/persistence/orm/cardset-incremental.orm-entity';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    TypeOrmModule.forRoot({
      type: 'mysql',
      host: process.env.DB_HOST,
      port: Number(process.env.DB_PORT),
      username: process.env.DB_USERNAME,
      password: process.env.DB_PASSWORD,
      database: process.env.DB_DATABASE,
      entities: [
        CardsetOrmEntity,
        CardOrmEntity,
        CardsetManagerOrmEntity,
        CardSetMetadataOrmEntity,
        CardsetContentOrmEntity,
        CardsetIncrementalOrmEntity,
      ],
      synchronize: true,
    }),
    AuthModule,
    CardsetModule,
    CollaborationModule,
    ReactionModule,
  ],
})
export class AppModule {}
