import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { CardsetResponse } from './cardset.response';

export class CardsetPageResponse {
  @ApiProperty({ type: [CardsetResponse] })
  items!: CardsetResponse[];

  @ApiPropertyOptional({ example: 5, nullable: true })
  nextCursor!: number | null;

  static from(
    items: CardsetResponse[],
    nextCursor: number | null,
  ): CardsetPageResponse {
    const res = new CardsetPageResponse();
    res.items = items;
    res.nextCursor = nextCursor;
    return res;
  }
}
