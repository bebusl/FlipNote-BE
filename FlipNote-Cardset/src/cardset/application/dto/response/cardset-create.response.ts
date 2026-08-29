import { ApiProperty } from '@nestjs/swagger';

export class CardsetCreateResponse {
  @ApiProperty({ example: 1 })
  cardsetId!: number;

  static from(id: number): CardsetCreateResponse {
    const res = new CardsetCreateResponse();
    res.cardsetId = id;
    return res;
  }
}
