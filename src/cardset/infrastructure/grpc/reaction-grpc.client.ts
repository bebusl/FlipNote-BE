import { Inject, Injectable, OnModuleInit } from '@nestjs/common';
import type { ClientGrpc } from '@nestjs/microservices';
import { Observable, firstValueFrom } from 'rxjs';

interface ReactionService {
  isLiked(data: {
    targetType: string;
    targetId: number;
    userId: number;
  }): Observable<{ reacted: boolean }>;
  isBookmarked(data: {
    targetType: string;
    targetId: number;
    userId: number;
  }): Observable<{ reacted: boolean }>;
  areLiked(data: {
    targetType: string;
    targetIds: number[];
    userId: number;
  }): Observable<{ results: Record<number, boolean> }>;
  areBookmarked(data: {
    targetType: string;
    targetIds: number[];
    userId: number;
  }): Observable<{ results: Record<number, boolean> }>;
}

@Injectable()
export class ReactionGrpcClient implements OnModuleInit {
  private reactionService: ReactionService;

  constructor(
    @Inject('REACTION_GRPC_CLIENT') private readonly client: ClientGrpc,
  ) {}

  onModuleInit() {
    this.reactionService =
      this.client.getService<ReactionService>('ReactionService');
  }

  async isLiked(cardSetId: number, userId: number): Promise<boolean> {
    const result = await firstValueFrom(
      this.reactionService.isLiked({
        targetType: 'CARD_SET',
        targetId: cardSetId,
        userId,
      }),
    );
    return result.reacted;
  }

  async isBookmarked(cardSetId: number, userId: number): Promise<boolean> {
    const result = await firstValueFrom(
      this.reactionService.isBookmarked({
        targetType: 'CARD_SET',
        targetId: cardSetId,
        userId,
      }),
    );
    return result.reacted;
  }

  async areLiked(
    cardSetIds: number[],
    userId: number,
  ): Promise<Map<number, boolean>> {
    const result = await firstValueFrom(
      this.reactionService.areLiked({
        targetType: 'CARD_SET',
        targetIds: cardSetIds,
        userId,
      }),
    );
    return new Map(Object.entries(result.results).map(([k, v]) => [Number(k), v]));
  }

  async areBookmarked(
    cardSetIds: number[],
    userId: number,
  ): Promise<Map<number, boolean>> {
    const result = await firstValueFrom(
      this.reactionService.areBookmarked({
        targetType: 'CARD_SET',
        targetIds: cardSetIds,
        userId,
      }),
    );
    return new Map(Object.entries(result.results).map(([k, v]) => [Number(k), v]));
  }
}
