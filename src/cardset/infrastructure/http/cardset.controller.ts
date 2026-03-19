import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  Headers,
  Logger,
} from '@nestjs/common';

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
import { CreateCardsetRequest } from '../../application/dto/request/create-cardset.request';
import { UpdateCardsetRequest } from '../../application/dto/request/update-cardset.request';
import { CardsetCreateResponse } from '../../application/dto/response/cardset-create.response';
import { CardsetListItemResponse } from '../../application/dto/response/cardset-list-item.response';
import { CardsetResponse } from '../../application/dto/response/cardset.response';
import { ManagerInfoResponse } from '../../application/dto/response/manager-info.response';
import { YjsCardResponse } from '../../application/dto/response/yjs-card.response';
import { ApiResponse } from '../../../shared/common/api-response';
import { PagedResponse } from '../../../shared/common/paged-response';

@ApiExtraModels(CardsetResponse, ManagerInfoResponse)
@ApiTags('card-sets')
@Controller('card-sets')
export class CardsetController {
  private readonly logger = new Logger(CardsetController.name);

  constructor(private readonly cardsetUseCase: CardsetUseCase) {}

  @Post()
  @ApiOperation({ summary: '카드셋 생성' })
  @SwaggerApiResponse({
    status: 201,
    description: '생성 성공',
    type: CardsetCreateResponse,
  })
  @SwaggerApiResponse({ status: 403, description: '그룹 멤버 아님' })
  async create(
    @Headers('X-USER-ID') userId: string,
    @Body() dto: CreateCardsetRequest,
  ): Promise<ApiResponse<CardsetCreateResponse>> {
    const cardset = await this.cardsetUseCase.create(parseInt(userId), dto);
    return ApiResponse.created(CardsetCreateResponse.from(cardset.id));
  }

  @Get()
  @ApiOperation({ summary: '카드셋 목록 조회 (페이징)' })
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
  async findAll(
    @Headers('X-USER-ID') userId: string,
    @Query() query: CardsetSearchRequest,
  ): Promise<ApiResponse<PagedResponse<CardsetListItemResponse>>> {
    const { items, total, page, size } = await this.cardsetUseCase.findAllPaged(
      query,
      parseInt(userId),
    );
    const responseItems = items.map(
      ({
        cardset,
        imageUrl,
        likeCount,
        bookmarkCount,
        liked,
        bookmarked,
        managers,
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
    return ApiResponse.success(
      PagedResponse.of(responseItems, total, page, size),
    );
  }

  @Get(':cardsetId')
  @ApiOperation({ summary: '카드셋 단건 조회' })
  @ApiParam({ name: 'cardsetId', type: Number })
  @SwaggerApiResponse({
    status: 200,
    description: '조회 성공',
    type: CardsetResponse,
  })
  @SwaggerApiResponse({ status: 403, description: '접근 권한 없음' })
  async findOne(
    @Headers('X-USER-ID') userId: string,
    @Param('cardsetId') cardsetId: string,
  ): Promise<ApiResponse<CardsetResponse | null>> {
    const result = await this.cardsetUseCase.findOne(
      parseInt(cardsetId),
      parseInt(userId),
    );
    return ApiResponse.success(
      result
        ? CardsetResponse.from(
            result.cardset,
            result.imageUrl,
            result.likeCount,
            result.bookmarkCount,
            result.liked,
            result.bookmarked,
            result.managers,
          )
        : null,
    );
  }

  @Put(':cardsetId')
  @ApiOperation({ summary: '카드셋 수정' })
  @ApiParam({ name: 'cardsetId', type: Number })
  @SwaggerApiResponse({
    status: 200,
    description: '수정 성공',
    type: CardsetResponse,
  })
  @SwaggerApiResponse({ status: 403, description: '매니저 권한 없음' })
  async update(
    @Headers('X-USER-ID') userId: string,
    @Param('cardsetId') cardsetId: string,
    @Body() dto: UpdateCardsetRequest,
  ): Promise<ApiResponse<CardsetResponse | null>> {
    const cardset = await this.cardsetUseCase.update(
      parseInt(cardsetId),
      parseInt(userId),
      dto,
    );
    return ApiResponse.success(cardset ? CardsetResponse.from(cardset) : null);
  }

  @Delete(':cardsetId')
  @ApiOperation({ summary: '카드셋 삭제' })
  @ApiParam({ name: 'cardsetId', type: Number })
  @SwaggerApiResponse({ status: 200, description: '삭제 성공' })
  @SwaggerApiResponse({ status: 403, description: '매니저 권한 없음' })
  async remove(
    @Headers('X-USER-ID') userId: string,
    @Param('cardsetId') cardsetId: string,
  ): Promise<ApiResponse<null>> {
    await this.cardsetUseCase.remove(parseInt(cardsetId), parseInt(userId));
    return ApiResponse.success(null, '삭제되었습니다.');
  }

  @Get(':cardsetId/cards')
  @ApiOperation({ summary: '카드셋의 카드 목록 조회' })
  @ApiParam({ name: 'cardsetId', type: Number })
  @SwaggerApiResponse({
    status: 200,
    description: '조회 성공',
    type: [YjsCardResponse],
  })
  async findCards(
    @Headers('X-USER-ID') _userId: string,
    @Param('cardsetId') cardsetId: string,
  ): Promise<ApiResponse<YjsCardResponse[]>> {
    const cards = await this.cardsetUseCase.findCardsFromYjs(
      parseInt(cardsetId),
    );
    return ApiResponse.success(cards.map((c) => YjsCardResponse.from(c)));
  }

  @Post(':cardsetId')
  @ApiOperation({ summary: '카드 편집 저장' })
  @ApiParam({ name: 'cardsetId', type: Number })
  @SwaggerApiResponse({ status: 200, description: '저장 성공' })
  @SwaggerApiResponse({ status: 403, description: '매니저 권한 없음' })
  async saveCards(
    @Headers('X-USER-ID') userId: string,
    @Param('cardsetId') cardsetId: string,
  ): Promise<ApiResponse<null>> {
    this.logger.log(
      `[카드 저장 요청] cardsetId=${cardsetId}, userId=${userId}`,
    );
    await this.cardsetUseCase.saveCards(parseInt(cardsetId), parseInt(userId));
    this.logger.log(
      `[카드 저장 완료] cardsetId=${cardsetId}, userId=${userId}`,
    );
    return ApiResponse.success(null);
  }

  // @Put(':cardsetId/card-count')
  // @ApiOperation({ summary: '카드 수 업데이트' })
  // @ApiParam({ name: 'cardsetId', type: Number })
  // @SwaggerApiResponse({
  //   status: 200,
  //   description: '업데이트 성공',
  //   type: CardsetResponse,
  // })
  // @SwaggerApiResponse({ status: 403, description: '매니저 권한 없음' })
  // async updateCardCount(
  //   @Headers('X-USER-ID') userId: string,
  //   @Param('cardsetId') cardsetId: string,
  //   @Body() body: { cardCount: number },
  // ): Promise<ApiResponse<CardsetResponse | null>> {
  //   const cardset = await this.cardsetUseCase.updateCardCount(
  //     parseInt(cardsetId),
  //     parseInt(userId),
  //     body.cardCount,
  //   );
  //   return ApiResponse.success(cardset ? CardsetResponse.from(cardset) : null);
  // }
}
