import { ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { IsIn, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';

export class CardsetSearchRequest {
  @ApiPropertyOptional({ example: 1, description: '페이지 번호 (1부터 시작)' })
  @IsOptional()
  @IsInt()
  @Min(1)
  @Type(() => Number)
  page?: number = 1;

  @ApiPropertyOptional({ example: 10, description: '페이지당 항목 수' })
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(30)
  @Type(() => Number)
  size?: number = 10;

  @ApiPropertyOptional({
    example: 'createdAt',
    description: '정렬 기준 (createdAt, name, cardCount)',
  })
  @IsOptional()
  @IsString()
  sortBy?: string;

  @ApiPropertyOptional({
    example: 'desc',
    enum: ['asc', 'desc'],
    description: '정렬 방향',
  })
  @IsOptional()
  @IsIn(['asc', 'desc', 'ASC', 'DESC'])
  order?: string = 'desc';

  @ApiPropertyOptional({ example: '영어', description: '카드셋 이름 검색 키워드' })
  @IsOptional()
  @IsString()
  keyword?: string;

  @ApiPropertyOptional({ example: '언어', description: '카테고리 필터' })
  @IsOptional()
  @IsString()
  category?: string;
}
