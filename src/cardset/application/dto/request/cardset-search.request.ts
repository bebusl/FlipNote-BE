import { ApiPropertyOptional } from '@nestjs/swagger';

export class CardsetSearchRequest {
  @ApiPropertyOptional({ example: 1, description: '페이지 번호 (1부터 시작)', default: 1 })
  page?: number = 1;

  @ApiPropertyOptional({ example: 10, description: '페이지당 항목 수 (최대 30)', default: 10 })
  size?: number = 10;

  @ApiPropertyOptional({
    example: 'createdAt',
    description: '정렬 기준 (createdAt, name, cardCount)',
  })
  sortBy?: string;

  @ApiPropertyOptional({
    example: 'desc',
    enum: ['asc', 'desc'],
    description: '정렬 방향',
    default: 'desc',
  })
  order?: string = 'desc';

  @ApiPropertyOptional({ example: '영어', description: '카드셋 이름 검색 키워드' })
  keyword?: string;

  @ApiPropertyOptional({ example: '언어', description: '카테고리 필터' })
  category?: string;
}
