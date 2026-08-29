import {
  ExceptionFilter,
  Catch,
  ArgumentsHost,
  HttpException,
  Logger,
} from '@nestjs/common';
import { Response } from 'express';
import { BusinessException } from './business.exception';
import { ErrorCode } from './error-code';

@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(GlobalExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost) {
    const contextType = host.getType();
    if (contextType === 'ws' || contextType === 'rpc') {
      this.logger.error(exception);
      throw exception;
    }

    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();

    if (exception instanceof BusinessException) {
      const { status, code, message } = exception.errorCode;
      return response
        .status(status)
        .json({ status, code, message, data: null });
    }

    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      return response.status(status).json({
        status,
        code: 'COMMON_001',
        message: exception.message,
        data: null,
      });
    }

    this.logger.error(exception);
    const { status, code, message } = ErrorCode.INTERNAL_SERVER_ERROR;
    return response.status(status).json({ status, code, message, data: null });
  }
}
