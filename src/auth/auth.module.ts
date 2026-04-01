import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { AuthService } from './domain/auth.service';
import { WsAuthGuard } from './infrastructure/guard/ws-auth.guard';
import { UserGrpcClient } from '../cardset/infrastructure/grpc/user-grpc.client';
import { GrpcClientModule } from '../shared/grpc/grpc-client.module';
import authConfig from '../shared/config/auth.config';

@Module({
  imports: [ConfigModule.forFeature(authConfig), GrpcClientModule],
  providers: [AuthService, WsAuthGuard, UserGrpcClient],
  exports: [AuthService, WsAuthGuard],
})
export class AuthModule {}
