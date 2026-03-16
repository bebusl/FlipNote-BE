import { Controller, Get, Headers, Param } from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse as SwaggerApiResponse,
  ApiParam,
  ApiExtraModels,
} from '@nestjs/swagger';
import { CardsetUseCase } from '../../application/cardset.use-case';
import { CardsetListItemResponse } from '../../application/dto/response/cardset-list-item.response';
import { ManagerInfoResponse } from '../../application/dto/response/manager-info.response';
import { ApiResponse } from '../../../shared/common/api-response';

@ApiExtraModels(CardsetListItemResponse, ManagerInfoResponse)
@ApiTags('groups')
@Controller('groups')
export class GroupCardsetController {
  constructor(private readonly cardsetUseCase: CardsetUseCase) {}

  @Get(':groupId/card-sets')
  @ApiOperation({ summary: '그룹의 카드셋 목록 조회' })
  @ApiParam({ name: 'groupId', type: Number })
  @SwaggerApiResponse({
    status: 200,
    description: '조회 성공',
    type: CardsetListItemResponse,
    isArray: true,
  })
  @SwaggerApiResponse({ status: 403, description: '그룹 멤버 아님' })
  async findByGroupId(
    @Headers('X-USER-ID') userId: string,
    @Param('groupId') groupId: string,
  ): Promise<ApiResponse<CardsetListItemResponse[]>> {
    const items = await this.cardsetUseCase.findByGroupId(
      parseInt(groupId),
      parseInt(userId),
    );
    const content = items.map(
      ({
        cardset,
        imageUrl,
        liked,
        bookmarked,
        managers,
        likeCount,
        bookmarkCount,
      }) =>
        CardsetListItemResponse.from(
          cardset,
          imageUrl,
          liked,
          bookmarked,
          managers,
          likeCount,
          bookmarkCount,
        ),
    );
    return ApiResponse.success(content);
  }
}
