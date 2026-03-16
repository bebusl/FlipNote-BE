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
import { CardUseCase } from '../../application/card.use-case';
import { CreateCardRequest } from '../../application/dto/request/create-card.request';
import { UpdateCardRequest } from '../../application/dto/request/update-card.request';
import { ReorderCardsRequest } from '../../application/dto/request/reorder-cards.request';
import { CardCreateResponse } from '../../application/dto/response/card-create.response';
import { CardResponse } from '../../application/dto/response/card.response';
import { ApiResponse } from '../../../shared/common/api-response';

@ApiTags('cards')
@Controller('cards')
export class CardController {
  constructor(private readonly cardUseCase: CardUseCase) { }

  // @Post()
  // @ApiOperation({ summary: '카드 생성' })
  // @SwaggerApiResponse({
  //   status: 201,
  //   description: '생성 성공',
  //   type: CardCreateResponse,
  // })
  // async create(
  //   @Headers('X-USER-ID') _userId: string,
  //   @Body() dto: CreateCardRequest,
  // ): Promise<ApiResponse<CardCreateResponse>> {
  //   const card = await this.cardUseCase.create(dto);
  //   return ApiResponse.created(CardCreateResponse.from(card.id));
  // }

  // @Get('cardset/:cardsetId')
  // @ApiOperation({ summary: '카드셋의 카드 목록 조회' })
  // @ApiParam({ name: 'cardsetId', type: Number })
  // @SwaggerApiResponse({
  //   status: 200,
  //   description: '조회 성공',
  //   type: [CardResponse],
  // })
  // async findByCardsetId(
  //   @Headers('X-USER-ID') _userId: string,
  //   @Param('cardsetId') cardsetId: string,
  // ): Promise<ApiResponse<CardResponse[]>> {
  //   const cards = await this.cardUseCase.findAllByCardsetId(
  //     parseInt(cardsetId),
  //   );
  //   return ApiResponse.success(cards.map((c) => CardResponse.from(c)));
  // }

  // @Get(':cardId')
  // @ApiOperation({ summary: '카드 단건 조회' })
  // @ApiParam({ name: 'cardId', type: Number })
  // @SwaggerApiResponse({
  //   status: 200,
  //   description: '조회 성공',
  //   type: CardResponse,
  // })
  // async findOne(
  //   @Headers('X-USER-ID') _userId: string,
  //   @Param('cardId') cardId: string,
  // ): Promise<ApiResponse<CardResponse | null>> {
  //   const card = await this.cardUseCase.findOne(parseInt(cardId));
  //   return ApiResponse.success(card ? CardResponse.from(card) : null);
  // }

  // @Put('reorder')
  // @ApiOperation({ summary: '카드 순서 변경' })
  // @SwaggerApiResponse({ status: 200, description: '순서 변경 성공' })
  // async reorderCards(
  //   @Headers('X-USER-ID') _userId: string,
  //   @Body() dto: ReorderCardsRequest,
  // ): Promise<ApiResponse<null>> {
  //   await this.cardUseCase.reorderCards(dto.cardOrders);
  //   return ApiResponse.success(null);
  // }

  // @Put(':cardId')
  // @ApiOperation({ summary: '카드 수정' })
  // @ApiParam({ name: 'cardId', type: Number })
  // @SwaggerApiResponse({
  //   status: 200,
  //   description: '수정 성공',
  //   type: CardResponse,
  // })
  // async update(
  //   @Headers('X-USER-ID') _userId: string,
  //   @Param('cardId') cardId: string,
  //   @Body() dto: UpdateCardRequest,
  // ): Promise<ApiResponse<CardResponse | null>> {
  //   const card = await this.cardUseCase.update(parseInt(cardId), dto);
  //   return ApiResponse.success(card ? CardResponse.from(card) : null);
  // }

  // @Delete(':cardId')
  // @ApiOperation({ summary: '카드 삭제' })
  // @ApiParam({ name: 'cardId', type: Number })
  // @SwaggerApiResponse({ status: 200, description: '삭제 성공' })
  // async remove(
  //   @Headers('X-USER-ID') _userId: string,
  //   @Param('cardId') cardId: string,
  // ): Promise<ApiResponse<null>> {
  //   await this.cardUseCase.remove(parseInt(cardId));
  //   return ApiResponse.success(null, '삭제되었습니다.');
  // }
}
