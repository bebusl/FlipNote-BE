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
import { CardsetUseCase } from '../../application/cardset.use-case';
import { CreateCardsetRequest } from '../../application/dto/request/create-cardset.request';
import { UpdateCardsetRequest } from '../../application/dto/request/update-cardset.request';
import { CardsetCreateResponse } from '../../application/dto/response/cardset-create.response';
import { CardsetResponse } from '../../application/dto/response/cardset.response';
import { ApiResponse } from '../../../shared/common/api-response';

@Controller('card-sets')
export class CardsetController {
  constructor(private readonly cardsetUseCase: CardsetUseCase) { }

  @Post()
  async create(
    @Headers('X-USER-ID') userId: string,
    @Body() dto: CreateCardsetRequest,
  ): Promise<ApiResponse<CardsetCreateResponse>> {
    const cardset = await this.cardsetUseCase.create(parseInt(userId), dto);
    return ApiResponse.created(CardsetCreateResponse.from(cardset.id));
  }

  @Get()
  async findAll(
    @Headers('X-USER-ID') userId: string,
  ): Promise<ApiResponse<CardsetResponse[]>> {
    const results = await this.cardsetUseCase.findAll(parseInt(userId));
    return ApiResponse.success(
      results.map(({ cardset, imageUrl }) =>
        CardsetResponse.from(cardset, imageUrl),
      ),
    );
  }

  @Get(':cardsetId')
  async findOne(
    @Headers('X-USER-ID') userId: string,
    @Param('cardsetId') cardsetId: string,
  ): Promise<ApiResponse<CardsetResponse | null>> {
    const result = await this.cardsetUseCase.findOne(
      parseInt(cardsetId),
      parseInt(userId),
    );
    return ApiResponse.success(
      result ? CardsetResponse.from(result.cardset, result.imageUrl) : null,
    );
  }

  @Put(':cardsetId')
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
  async remove(
    @Headers('X-USER-ID') userId: string,
    @Param('cardsetId') cardsetId: string,
  ): Promise<ApiResponse<null>> {
    await this.cardsetUseCase.remove(parseInt(cardsetId), parseInt(userId));
    return ApiResponse.success(null, '삭제되었습니다.');
  }

  @Put(':cardsetId/card-count')
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
