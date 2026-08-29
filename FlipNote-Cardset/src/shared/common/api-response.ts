import { ApiProperty } from '@nestjs/swagger';

export class ApiResponse<T> {
  @ApiProperty({ example: 200 })
  status: number;

  @ApiProperty({ example: 'SUCCESS' })
  code: string;

  @ApiProperty({ example: '요청이 성공적으로 처리되었습니다.' })
  message: string;

  @ApiProperty()
  data: T;

  private constructor(status: number, code: string, message: string, data: T) {
    this.status = status;
    this.code = code;
    this.message = message;
    this.data = data;
  }

  static success<T>(
    data: T,
    message = '요청이 성공적으로 처리되었습니다.',
  ): ApiResponse<T> {
    return new ApiResponse(200, 'SUCCESS', message, data);
  }

  static created<T>(data: T, message = '생성되었습니다.'): ApiResponse<T> {
    return new ApiResponse(201, 'CREATED', message, data);
  }
}
