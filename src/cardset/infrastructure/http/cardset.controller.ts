import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Headers,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse as SwaggerApiResponse,
  ApiParam,
} from '@nestjs/swagger';
import { CardsetUseCase } from '../../application/cardset.use-case';
import { CreateCardsetRequest } from '../../application/dto/request/create-cardset.request';
import { UpdateCardsetRequest } from '../../application/dto/request/update-cardset.request';
import { CardsetCreateResponse } from '../../application/dto/response/cardset-create.response';
import { CardsetResponse } from '../../application/dto/response/cardset.response';
import { ApiResponse } from '../../../shared/common/api-response';

@ApiTags('card-sets')
@Controller('card-sets')
export class CardsetController {
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
  @ApiOperation({ summary: '카드셋 목록 조회' })
  @SwaggerApiResponse({
    status: 200,
    description: '조회 성공',
    type: [CardsetResponse],
  })
  async findAll(
    @Headers('X-USER-ID') userId: string,
  ): Promise<ApiResponse<CardsetResponse[]>> {
    const results = await this.cardsetUseCase.findAll(parseInt(userId));
    return ApiResponse.success(
      results.map(({ cardset, imageUrl, likeCount, bookmarkCount }) =>
        CardsetResponse.from(cardset, imageUrl, likeCount, bookmarkCount),
      ),
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

  @Put(':cardsetId/card-count')
  @ApiOperation({ summary: '카드 수 업데이트' })
  @ApiParam({ name: 'cardsetId', type: Number })
  @SwaggerApiResponse({
    status: 200,
    description: '업데이트 성공',
    type: CardsetResponse,
  })
  @SwaggerApiResponse({ status: 403, description: '매니저 권한 없음' })
  async updateCardCount(
    @Headers('X-USER-ID') userId: string,
    @Param('cardsetId') cardsetId: string,
    @Body() body: { cardCount: number },
  ): Promise<ApiResponse<CardsetResponse | null>> {
    const cardset = await this.cardsetUseCase.updateCardCount(
      parseInt(cardsetId),
      parseInt(userId),
      body.cardCount,
    );
    return ApiResponse.success(cardset ? CardsetResponse.from(cardset) : null);
  }
}
