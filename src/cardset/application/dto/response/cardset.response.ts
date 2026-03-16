import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Visibility } from '../../../domain/model/visibility';
import { Cardset } from '../../../domain/model/cardset';

export class CardsetResponse {
  @ApiProperty({ example: 1 })
  id!: number;

  @ApiProperty({ example: '영어 단어장' })
  name!: string;

  @ApiProperty({ example: 1 })
  groupId!: number;

  @ApiProperty({ enum: Visibility, example: Visibility.PRIVATE })
  visibility!: Visibility;

  @ApiProperty({ example: '언어' })
  category!: string;

  @ApiPropertyOptional({ example: '#영어#단어' })
  hashtag!: string | null;

  @ApiPropertyOptional({ example: 1001 })
  imageRefId!: number | null;

  @ApiProperty({ example: 'https://example.com/image.png' })
  imageUrl!: string;

  @ApiProperty({ example: 10 })
  cardCount!: number;

  @ApiProperty({ example: 0 })
  likeCount!: number;

  @ApiProperty({ example: 0 })
  bookmarkCount!: number;

  @ApiProperty()
  createdAt!: Date;

  @ApiProperty()
  updatedAt!: Date;

  static from(
    cardset: Cardset,
    imageUrl = '',
    likeCount = 0,
    bookmarkCount = 0,
  ): CardsetResponse {
    const res = new CardsetResponse();
    res.id = cardset.id;
    res.name = cardset.name;
    res.groupId = cardset.groupId;
    res.visibility = cardset.visibility;
    res.category = cardset.category;
    res.hashtag = cardset.hashtag;
    res.imageRefId = cardset.imageRefId;
    res.imageUrl = imageUrl;
    res.cardCount = cardset.cardCount;
    res.likeCount = likeCount;
    res.bookmarkCount = bookmarkCount;
    res.createdAt = cardset.createdAt;
    res.updatedAt = cardset.updatedAt;
    return res;
  }
}
