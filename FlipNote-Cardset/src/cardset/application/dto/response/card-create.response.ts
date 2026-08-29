import { ApiProperty } from '@nestjs/swagger';

export class CardCreateResponse {
  @ApiProperty({ example: 1 })
  cardId!: number;

  static from(id: number): CardCreateResponse {
    const res = new CardCreateResponse();
    res.cardId = id;
    return res;
  }
}
