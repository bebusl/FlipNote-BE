import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Visibility } from '../../../domain/model/visibility';

export class CreateCardsetRequest {
  @ApiProperty({ example: '영어 단어장' })
  name!: string;

  @ApiProperty({ example: 1 })
  groupId!: number;

  @ApiProperty({ enum: Visibility, example: Visibility.PRIVATE })
  visibility!: Visibility;

  @ApiProperty({ example: '언어' })
  category!: string;

  @ApiPropertyOptional({ example: '#영어#단어' })
  hashtag?: string | null;

  @ApiPropertyOptional({ example: 1001 })
  imageRefId?: number | null;

  @ApiPropertyOptional({ example: [1, 2], type: [Number] })
  managerIds?: number[];
}
