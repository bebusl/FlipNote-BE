import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Cardset } from '../../../domain/model/cardset';
import { ManagerInfoResponse } from './manager-info.response';
import type { UserInfo } from '../../../infrastructure/grpc/user-grpc.client';

export class CardsetListItemResponse {
  @ApiProperty({ example: 1 })
  cardSetId!: number;

  @ApiProperty({ example: 1 })
  groupId!: number;

  @ApiProperty({ example: '영어 단어장' })
  name!: string;

  @ApiProperty({ example: '언어' })
  category!: string;

  @ApiPropertyOptional({ example: '#영어#단어' })
  hashtag!: string | null;

  @ApiProperty({ example: 'https://example.com/image.png' })
  imageUrl!: string;

  @ApiPropertyOptional({ example: 1001 })
  imageRefId!: number | null;

  @ApiProperty({ example: 0 })
  likeCount!: number;

  @ApiProperty({ example: 0 })
  bookmarkCount!: number;

  @ApiProperty({ example: false })
  liked!: boolean;

  @ApiProperty({ example: false })
  bookmarked!: boolean;

  @ApiProperty({ type: [ManagerInfoResponse] })
  managers!: ManagerInfoResponse[];

  static from(
    cardset: Cardset,
    imageUrl = '',
    liked = false,
    bookmarked = false,
    managers: UserInfo[] = [],
    likeCount = 0,
    bookmarkCount = 0,
  ): CardsetListItemResponse {
    const res = new CardsetListItemResponse();
    res.cardSetId = cardset.id;
    res.groupId = cardset.groupId;
    res.name = cardset.name;
    res.category = cardset.category;
    res.hashtag = cardset.hashtag;
    res.imageUrl = imageUrl;
    res.imageRefId = cardset.imageRefId;
    res.likeCount = likeCount;
    res.bookmarkCount = bookmarkCount;
    res.liked = liked;
    res.bookmarked = bookmarked;
    res.managers = managers.map((m) => {
      const info = new ManagerInfoResponse();
      info.id = m.id;
      info.email = m.email;
      info.nickname = m.nickname;
      info.profileImageUrl = m.profileImageUrl;
      return info;
    });
    return res;
  }
}
