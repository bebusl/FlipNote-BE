import { ArgumentsHost, Catch, UnauthorizedException } from '@nestjs/common';
import { BaseWsExceptionFilter, WsException } from '@nestjs/websockets';
import { Socket } from 'socket.io';

@Catch(WsException, UnauthorizedException)
export class WsExceptionFilter extends BaseWsExceptionFilter {
  catch(exception: WsException | UnauthorizedException, host: ArgumentsHost) {
    const client = host.switchToWs().getClient<Socket>();
    const message =
      exception instanceof WsException
        ? exception.getError()
        : exception.message;

    client.emit('error', {
      message: typeof message === 'string' ? message : '인증 실패',
    });
    client.disconnect();
  }
}
