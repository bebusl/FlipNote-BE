import { ApiPropertyOptional } from '@nestjs/swagger';
import { Visibility } from '../../../domain/model/visibility';

export class UpdateCardsetRequest {
  @ApiPropertyOptional({ example: '수정된 단어장' })
  name?: string;

  @ApiPropertyOptional({ enum: Visibility, example: Visibility.PUBLIC })
  visibility?: Visibility;

  @ApiPropertyOptional({ example: '수학' })
  category?: string;

  @ApiPropertyOptional({ example: '#수학 #공식' })
  hashtag?: string | null;

  @ApiPropertyOptional({ example: 1001 })
  imageRefId?: number;

  @ApiPropertyOptional({ example: [1, 2], type: [Number] })
  managerIds?: number[];
}
