import { HttpException } from '@nestjs/common';
import { ErrorCode } from './error-code';

export class BusinessException extends HttpException {
  constructor(public readonly errorCode: ErrorCode) {
    super(
      {
        status: errorCode.status,
        code: errorCode.code,
        message: errorCode.message,
        data: null,
      },
      errorCode.status,
    );
  }
}
