import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateCardRequest {
  @ApiPropertyOptional({ example: '안녕하세요' })
  content?: string;

  @ApiProperty({ example: 1 })
  order!: number;

  @ApiProperty({ example: 1 })
  cardsetId!: number;
}
