import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import type { UserAuth } from '../types/user-auth.type';
import { Socket } from 'socket.io';

export const WsUser = createParamDecorator(
  (_data: unknown, ctx: ExecutionContext): UserAuth => {
    const client = ctx.switchToWs().getClient<Socket>();
    return (client.data as { user: UserAuth }).user;
  },
);
