import { ApiProperty } from '@nestjs/swagger';

export class YjsCardResponse {
  @ApiProperty({ example: '1' })
  id!: string;

  @ApiProperty({ example: '질문 내용' })
  question!: string;

  @ApiProperty({ example: '답변 내용' })
  answer!: string;

  static from(card: {
    id: string;
    question: string;
    answer: string;
  }): YjsCardResponse {
    const res = new YjsCardResponse();
    res.id = card.id;
    res.question = card.question;
    res.answer = card.answer;
    return res;
  }
}
