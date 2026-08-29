import { Controller } from '@nestjs/common';
import { GrpcMethod } from '@nestjs/microservices';
import { CardsetUseCase } from '../../application/cardset.use-case';
import type {
  IsCardSetViewableRequest,
  IsCardSetViewableResponse,
  GetCardSetsByIdsRequest,
  GetCardSetsByIdsResponse,
} from '../../../shared/grpc/grpc.types';

@Controller()
export class CardsetGrpcController {
  constructor(private readonly cardsetUseCase: CardsetUseCase) {}

  @GrpcMethod('CardsetService', 'IsCardSetViewable')
  async isCardSetViewable(
    data: IsCardSetViewableRequest,
  ): Promise<IsCardSetViewableResponse> {
    const viewable = await this.cardsetUseCase.isCardSetViewable(
      Number(data.cardSetId),
      Number(data.userId),
    );
    return { viewable };
  }

  @GrpcMethod('CardsetService', 'GetCardSetsByIds')
  async getCardSetsByIds(
    data: GetCardSetsByIdsRequest,
  ): Promise<GetCardSetsByIdsResponse> {
    const cardsets = await this.cardsetUseCase.getCardSetsByIds(
      data.cardSetIds.map(Number),
      Number(data.userId),
    );
    return {
      cardSets: cardsets.map((c) => ({
        id: c.id,
        name: c.name,
        groupId: c.groupId,
        visibility: c.visibility,
        category: c.category,
        hashtag: c.hashtag ?? '',
        imageRefId: c.imageRefId,
        cardCount: c.cardCount,
      })),
    };
  }
}
