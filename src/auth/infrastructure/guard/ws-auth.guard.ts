import {
  CanActivate,
  ExecutionContext,
  Injectable,
  Logger,
} from '@nestjs/common';
import { Socket } from 'socket.io';
import { UserGrpcClient } from '../../../cardset/infrastructure/grpc/user-grpc.client';

@Injectable()
export class WsAuthGuard implements CanActivate {
  private readonly logger = new Logger(WsAuthGuard.name);

  constructor(private readonly userGrpcClient: UserGrpcClient) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const client: Socket = context.switchToWs().getClient<Socket>();

    const rawAuth: unknown = client.handshake.auth?.token;
    const rawHeader = client.handshake.headers?.authorization;
    const rawCookie = client.handshake.headers?.cookie;

    const fromCookie =
      typeof rawCookie === 'string'
        ? rawCookie
            .split(';')
            .map((c) => c.trim())
            .find((c) => c.startsWith('accessToken='))
            ?.slice('accessToken='.length)
        : undefined;

    const bearer =
      (typeof rawAuth === 'string' ? rawAuth : undefined) ??
      (typeof rawHeader === 'string' ? rawHeader : undefined);

    const token =
      fromCookie ??
      (bearer && bearer.startsWith('Bearer ') ? bearer.slice(7) : bearer);

    this.logger.log(`[WsAuthGuard] cookie: ${String(rawCookie)}`);
    this.logger.log(`[WsAuthGuard] fromCookie: ${String(fromCookie)}`);
    this.logger.log(`[WsAuthGuard] token: ${String(token)}`);

    if (!token) {
      this.logger.warn(`No token provided for client ${client.id}`);
      return false;
    }

    try {
      const { userId, nickname } = await this.userGrpcClient.getUserByToken(token);
      (client.data as { user: unknown }).user = {
        userId: String(userId),
        nickname,
      };
      return true;
    } catch (err: unknown) {
      this.logger.warn(
        `Token verification failed for client ${client.id}: ${
          err instanceof Error ? err.message : String(err)
        }`,
      );
      return false;
    }
  }
}
