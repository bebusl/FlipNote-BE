import { Controller, Get, Headers, Param, Query } from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse as SwaggerApiResponse,
  ApiParam,
  ApiQuery,
  ApiExtraModels,
} from '@nestjs/swagger';
import { CardsetUseCase } from '../../application/cardset.use-case';
import { CardsetSearchRequest } from '../../application/dto/request/cardset-search.request';
import { CardsetListItemResponse } from '../../application/dto/response/cardset-list-item.response';
import { ManagerInfoResponse } from '../../application/dto/response/manager-info.response';
import { ApiResponse } from '../../../shared/common/api-response';
import { PagedResponse } from '../../../shared/common/paged-response';

@ApiExtraModels(CardsetListItemResponse, ManagerInfoResponse)
@ApiTags('groups')
@Controller('groups')
export class GroupCardsetController {
  constructor(private readonly cardsetUseCase: CardsetUseCase) {}

  @Get(':groupId/card-sets')
  @ApiOperation({ summary: '그룹의 카드셋 목록 조회 (페이징)' })
  @ApiParam({ name: 'groupId', type: Number })
  @ApiQuery({ name: 'page', required: false, example: 1 })
  @ApiQuery({ name: 'size', required: false, example: 10 })
  @ApiQuery({
    name: 'sortBy',
    required: false,
    example: 'id',
    description: 'id | like | book',
  })
  @ApiQuery({
    name: 'order',
    required: false,
    example: 'desc',
    enum: ['asc', 'desc'],
  })
  @ApiQuery({ name: 'keyword', required: false, example: '영어' })
  @ApiQuery({ name: 'category', required: false, example: '언어' })
  @SwaggerApiResponse({ status: 200, description: '조회 성공' })
  @SwaggerApiResponse({ status: 403, description: '그룹 멤버 아님' })
  async findByGroupId(
    @Headers('X-USER-ID') userId: string,
    @Param('groupId') groupId: string,
    @Query() query: CardsetSearchRequest,
  ): Promise<ApiResponse<PagedResponse<CardsetListItemResponse>>> {
    const { items, total, page, size } =
      await this.cardsetUseCase.findByGroupIdPaged(
        parseInt(groupId),
        parseInt(userId),
        query,
      );
    const responseItems = items.map(
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
    return ApiResponse.success(PagedResponse.of(responseItems, total, page, size));
  }
}
