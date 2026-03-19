import { Inject, Injectable, Logger } from '@nestjs/common';
import { DataSource } from 'typeorm';
import { BusinessException } from '../../shared/common/business.exception';
import { ErrorCode } from '../../shared/common/error-code';
import { Cardset } from '../domain/model/cardset';
import { CardsetManager } from '../domain/model/cardset-manager';
import { Visibility } from '../domain/model/visibility';
import { CARDSET_REPOSITORY } from '../domain/repository/cardset.repository';
import type { ICardsetRepository } from '../domain/repository/cardset.repository';
import { CARD_REPOSITORY } from '../domain/repository/card.repository';
import type { ICardRepository } from '../domain/repository/card.repository';
import { CARDSET_MANAGER_REPOSITORY } from '../domain/repository/cardset-manager.repository';
import type { ICardsetManagerRepository } from '../domain/repository/cardset-manager.repository';
import { CARDSET_METADATA_REPOSITORY } from '../domain/repository/cardset-metadata.repository';
import type { ICardSetMetadataRepository } from '../domain/repository/cardset-metadata.repository';
import { CardsetCardDomainService } from '../domain/service/cardset-card.domain-service';
import { GroupGrpcClient } from '../infrastructure/grpc/group-grpc.client';
import { ImageGrpcClient } from '../infrastructure/grpc/image-grpc.client';
import { ReactionGrpcClient } from '../infrastructure/grpc/reaction-grpc.client';
import { UserGrpcClient } from '../infrastructure/grpc/user-grpc.client';
import type { UserInfo } from '../infrastructure/grpc/user-grpc.client';
import { CreateCardsetRequest } from './dto/request/create-cardset.request';
import { UpdateCardsetRequest } from './dto/request/update-cardset.request';
import { CollaborationUseCase } from '../../collaboration/application/collaboration.use-case';
import { CardsetSearchRequest } from './dto/request/cardset-search.request';

@Injectable()
export class CardsetUseCase {
  private readonly logger = new Logger(CardsetUseCase.name);

  constructor(
    @Inject(CARDSET_REPOSITORY)
    private readonly cardsetRepository: ICardsetRepository,
    @Inject(CARD_REPOSITORY)
    private readonly cardRepository: ICardRepository,
    @Inject(CARDSET_MANAGER_REPOSITORY)
    private readonly cardsetManagerRepository: ICardsetManagerRepository,
    private readonly cardsetCardDomainService: CardsetCardDomainService,
    private readonly groupGrpcClient: GroupGrpcClient,
    private readonly imageGrpcClient: ImageGrpcClient,
    private readonly reactionGrpcClient: ReactionGrpcClient,
    private readonly userGrpcClient: UserGrpcClient,
    private readonly dataSource: DataSource,
    @Inject(CARDSET_METADATA_REPOSITORY)
    private readonly metadataRepository: ICardSetMetadataRepository,
    private readonly collaborationUseCase: CollaborationUseCase,
  ) {}

  private async checkIsManager(
    cardSetId: number,
    userId: number,
  ): Promise<void> {
    const manager =
      await this.cardsetManagerRepository.findByUserIdAndCardSetId(
        userId,
        cardSetId,
      );
    if (!manager) {
      throw new BusinessException(ErrorCode.CARDSET_MANAGER_REQUIRED);
    }
  }

  async create(userId: number, dto: CreateCardsetRequest): Promise<Cardset> {
    await this.groupGrpcClient.checkUserInGroup(dto.groupId, userId);

    const additionalManagerIds: number[] = dto.managerIds ?? [];
    for (const managerId of additionalManagerIds) {
      await this.groupGrpcClient.checkUserInGroup(dto.groupId, managerId);
    }

    return this.dataSource.transaction(async (manager) => {
      const cardset = Cardset.create(dto);
      const savedCardset = await this.cardsetRepository.save(cardset, manager);

      const cardsToAdd = this.cardsetCardDomainService.buildCardsToAdd(
        savedCardset.id,
        0,
        10,
      );
      for (const card of cardsToAdd) {
        await this.cardRepository.save(card, manager);
      }

      const managerIds = [
        userId,
        ...additionalManagerIds.filter((id) => id !== userId),
      ];
      for (const managerId of managerIds) {
        const cardsetManager = CardsetManager.create({
          userId: managerId,
          cardSetId: savedCardset.id,
        });
        await this.cardsetManagerRepository.save(cardsetManager, manager);
      }

      if (dto.imageRefId) {
        await this.imageGrpcClient.activateImage(
          dto.imageRefId,
          savedCardset.id,
        );
      }

      return savedCardset;
    });
  }

  private readonly defaultImageUrl =
    process.env.DEFAULT_CARDSET_IMAGE_URL ?? '';

  private readonly skipUserGrpc = process.env.SKIP_USER_GRPC === 'true';
  private readonly skipReactionGrpc = process.env.SKIP_REACTION_GRPC === 'true';

  private async getManagersForCardSets(
    cardSetIds: number[],
  ): Promise<Map<number, UserInfo[]>> {
    if (cardSetIds.length === 0 || this.skipUserGrpc) return new Map();

    const managers: CardsetManager[] =
      await this.cardsetManagerRepository.findByCardSetIds(cardSetIds);
    const userIds: number[] = [...new Set(managers.map((m) => m.userId))];

    const users = await this.userGrpcClient.getUsersByIds(userIds);
    const userMap = new Map(users.map((u) => [Number(u.id), u]));
    const result = new Map<number, UserInfo[]>();
    for (const m of managers) {
      const user = userMap.get(m.userId);
      if (!user) continue;
      const list = result.get(m.cardSetId) ?? [];
      list.push(user);
      result.set(m.cardSetId, list);
    }
    return result;
  }

  async findAllPaged(
    req: CardsetSearchRequest,
    userId: number,
  ): Promise<{
    items: {
      cardset: Cardset;
      imageUrl: string;
      likeCount: number;
      bookmarkCount: number;
      liked: boolean;
      bookmarked: boolean;
      managers: UserInfo[];
    }[];
    total: number;
    page: number;
    size: number;
  }> {
    const page = req.page ?? 1;
    const size = req.size ?? 10;

    const { items, total } = await this.cardsetRepository.findAllPaged({
      page: page - 1,
      size,
      sortBy: req.sortBy,
      order: (req.order?.toUpperCase() as 'ASC' | 'DESC') ?? 'DESC',
      keyword: req.keyword,
      category: req.category,
    });

    const visibleCardsets: Cardset[] = [];
    for (const cardset of items) {
      if (cardset.visibility === Visibility.PUBLIC) {
        visibleCardsets.push(cardset);
        continue;
      }
      if (isNaN(userId)) continue;
      try {
        const inGroup = await this.groupGrpcClient.isUserInGroup(
          cardset.groupId,
          userId,
        );
        if (inGroup) visibleCardsets.push(cardset);
      } catch {
        // 그룹 조회 실패 시 해당 카드셋 제외
      }
    }

    const ids = visibleCardsets.map((c) => c.id);

    if (ids.length === 0) {
      return { items: [], total, page, size };
    }

    const [metadataMap, likedMap, bookmarkedMap, managersMap] =
      await Promise.all([
        this.metadataRepository.findByCardSetIds(ids),
        this.skipReactionGrpc
          ? Promise.resolve(new Map<number, boolean>())
          : this.reactionGrpcClient.areLiked(ids, userId),
        this.skipReactionGrpc
          ? Promise.resolve(new Map<number, boolean>())
          : this.reactionGrpcClient.areBookmarked(ids, userId),
        this.getManagersForCardSets(ids),
      ]);

    const result: {
      cardset: Cardset;
      imageUrl: string;
      likeCount: number;
      bookmarkCount: number;
      liked: boolean;
      bookmarked: boolean;
      managers: UserInfo[];
    }[] = [];
    for (const cardset of visibleCardsets) {
      const imageUrl = cardset.imageRefId
        ? await this.imageGrpcClient.getImageUrl(cardset.id)
        : this.defaultImageUrl;
      const meta = metadataMap.get(cardset.id);
      result.push({
        cardset,
        imageUrl,
        likeCount: meta?.likeCount ?? 0,
        bookmarkCount: meta?.bookmarkCount ?? 0,
        liked: likedMap.get(cardset.id) ?? false,
        bookmarked: bookmarkedMap.get(cardset.id) ?? false,
        managers: managersMap.get(cardset.id) ?? [],
      });
    }

    return { items: result, total, page, size };
  }

  async findAll(userId: number): Promise<
    {
      cardset: Cardset;
      imageUrl: string;
      likeCount: number;
      bookmarkCount: number;
      liked: boolean;
      bookmarked: boolean;
      managers: UserInfo[];
    }[]
  > {
    const cardsets = await this.cardsetRepository.findAll();
    const visibleCardsets: Cardset[] = [];
    for (const cardset of cardsets) {
      if (cardset.visibility === Visibility.PUBLIC) {
        visibleCardsets.push(cardset);
        continue;
      }
      if (isNaN(userId)) continue;
      try {
        const inGroup = await this.groupGrpcClient.isUserInGroup(
          cardset.groupId,
          userId,
        );
        if (inGroup) visibleCardsets.push(cardset);
      } catch {
        // 그룹 조회 실패 시 해당 카드셋 제외
      }
    }

    const ids = visibleCardsets.map((c) => c.id);
    const [metadataMap, likedMap, bookmarkedMap, managersMap] =
      await Promise.all([
        this.metadataRepository.findByCardSetIds(ids),
        this.skipReactionGrpc
          ? Promise.resolve(new Map<number, boolean>())
          : this.reactionGrpcClient.areLiked(ids, userId),
        this.skipReactionGrpc
          ? Promise.resolve(new Map<number, boolean>())
          : this.reactionGrpcClient.areBookmarked(ids, userId),
        this.getManagersForCardSets(ids),
      ]);

    const result: {
      cardset: Cardset;
      imageUrl: string;
      likeCount: number;
      bookmarkCount: number;
      liked: boolean;
      bookmarked: boolean;
      managers: UserInfo[];
    }[] = [];
    for (const cardset of visibleCardsets) {
      const imageUrl = cardset.imageRefId
        ? await this.imageGrpcClient.getImageUrl(cardset.id)
        : this.defaultImageUrl;
      const meta = metadataMap.get(cardset.id);
      result.push({
        cardset,
        imageUrl,
        likeCount: meta?.likeCount ?? 0,
        bookmarkCount: meta?.bookmarkCount ?? 0,
        liked: likedMap.get(cardset.id) ?? false,
        bookmarked: bookmarkedMap.get(cardset.id) ?? false,
        managers: managersMap.get(cardset.id) ?? [],
      });
    }
    return result;
  }

  async findByGroupIdPaged(
    groupId: number,
    userId: number,
    req: CardsetSearchRequest,
  ): Promise<{
    items: {
      cardset: Cardset;
      imageUrl: string;
      likeCount: number;
      bookmarkCount: number;
      liked: boolean;
      bookmarked: boolean;
      managers: UserInfo[];
    }[];
    total: number;
    page: number;
    size: number;
  }> {
    await this.groupGrpcClient.checkUserInGroup(groupId, userId);

    const page = req.page ?? 1;
    const size = req.size ?? 10;

    const { items, total } = await this.cardsetRepository.findAllPaged({
      page: page - 1,
      size,
      sortBy: req.sortBy,
      order: (req.order?.toUpperCase() as 'ASC' | 'DESC') ?? 'DESC',
      keyword: req.keyword,
      category: req.category,
      groupId,
    });

    const ids = items.map((c) => c.id);

    if (ids.length === 0) {
      return { items: [], total, page, size };
    }

    const [metadataMap, likedMap, bookmarkedMap, managersMap] =
      await Promise.all([
        this.metadataRepository.findByCardSetIds(ids),
        this.skipReactionGrpc
          ? Promise.resolve(new Map<number, boolean>())
          : this.reactionGrpcClient.areLiked(ids, userId),
        this.skipReactionGrpc
          ? Promise.resolve(new Map<number, boolean>())
          : this.reactionGrpcClient.areBookmarked(ids, userId),
        this.getManagersForCardSets(ids),
      ]);

    const result: {
      cardset: Cardset;
      imageUrl: string;
      likeCount: number;
      bookmarkCount: number;
      liked: boolean;
      bookmarked: boolean;
      managers: UserInfo[];
    }[] = [];
    for (const cardset of items) {
      const imageUrl = cardset.imageRefId
        ? await this.imageGrpcClient.getImageUrl(cardset.id)
        : this.defaultImageUrl;
      const meta = metadataMap.get(cardset.id);
      result.push({
        cardset,
        imageUrl,
        likeCount: meta?.likeCount ?? 0,
        bookmarkCount: meta?.bookmarkCount ?? 0,
        liked: likedMap.get(cardset.id) ?? false,
        bookmarked: bookmarkedMap.get(cardset.id) ?? false,
        managers: managersMap.get(cardset.id) ?? [],
      });
    }

    return { items: result, total, page, size };
  }

  async findOne(
    id: number,
    userId: number,
  ): Promise<{
    cardset: Cardset;
    imageUrl: string;
    likeCount: number;
    bookmarkCount: number;
    liked: boolean;
    bookmarked: boolean;
    managers: UserInfo[];
  } | null> {
    const cardset = await this.cardsetRepository.findById(id);
    if (!cardset) return null;
    if (cardset.visibility !== Visibility.PUBLIC) {
      const inGroup =
        !isNaN(userId) &&
        (await this.groupGrpcClient.isUserInGroup(cardset.groupId, userId));
      if (!inGroup)
        throw new BusinessException(ErrorCode.CARDSET_ACCESS_DENIED);
    }
    const [imageUrl, meta, liked, bookmarked, managersMap] = await Promise.all([
      cardset.imageRefId
        ? this.imageGrpcClient.getImageUrl(cardset.id)
        : Promise.resolve(this.defaultImageUrl),
      this.metadataRepository.findByCardSetId(id),
      this.skipReactionGrpc
        ? Promise.resolve(false)
        : this.reactionGrpcClient.isLiked(id, userId),
      this.skipReactionGrpc
        ? Promise.resolve(false)
        : this.reactionGrpcClient.isBookmarked(id, userId),
      this.getManagersForCardSets([id]),
    ]);
    return {
      cardset,
      imageUrl,
      likeCount: meta?.likeCount ?? 0,
      bookmarkCount: meta?.bookmarkCount ?? 0,
      liked,
      bookmarked,
      managers: managersMap.get(id) ?? [],
    };
  }

  async findByGroupId(
    groupId: number,
    userId: number,
  ): Promise<
    {
      cardset: Cardset;
      imageUrl: string;
      likeCount: number;
      bookmarkCount: number;
      liked: boolean;
      bookmarked: boolean;
      managers: UserInfo[];
    }[]
  > {
    await this.groupGrpcClient.checkUserInGroup(groupId, userId);

    const cardsets: Cardset[] =
      await this.cardsetRepository.findByGroupId(groupId);
    const ids: number[] = cardsets.map((c) => c.id);

    const [metadataMap, likedMap, bookmarkedMap, managersMap] =
      await Promise.all([
        this.metadataRepository.findByCardSetIds(ids),
        this.skipReactionGrpc
          ? Promise.resolve(new Map<number, boolean>())
          : this.reactionGrpcClient.areLiked(ids, userId),
        this.skipReactionGrpc
          ? Promise.resolve(new Map<number, boolean>())
          : this.reactionGrpcClient.areBookmarked(ids, userId),
        this.getManagersForCardSets(ids),
      ]);
    const result: {
      cardset: Cardset;
      imageUrl: string;
      likeCount: number;
      bookmarkCount: number;
      liked: boolean;
      bookmarked: boolean;
      managers: UserInfo[];
    }[] = [];
    for (const cardset of cardsets) {
      const imageUrl = cardset.imageRefId
        ? await this.imageGrpcClient.getImageUrl(cardset.id)
        : this.defaultImageUrl;
      const meta = metadataMap.get(cardset.id);
      result.push({
        cardset,
        imageUrl,
        likeCount: meta?.likeCount ?? 0,
        bookmarkCount: meta?.bookmarkCount ?? 0,
        liked: likedMap.get(cardset.id) ?? false,
        bookmarked: bookmarkedMap.get(cardset.id) ?? false,
        managers: managersMap.get(cardset.id) ?? [],
      });
    }
    return result;
  }

  async update(
    id: number,
    userId: number,
    dto: UpdateCardsetRequest,
  ): Promise<Cardset | null> {
    const cardset = await this.cardsetRepository.findById(id);
    if (!cardset) throw new BusinessException(ErrorCode.CARDSET_NOT_FOUND);

    await this.checkIsManager(id, userId);

    if (dto.imageRefId !== undefined) {
      await this.imageGrpcClient.changeImage(dto.imageRefId, id);
    }

    return this.dataSource.transaction(async (manager) => {
      if (dto.managerIds !== undefined) {
        const newManagerIds: number[] = dto.managerIds;
        for (const managerId of newManagerIds) {
          await this.groupGrpcClient.checkUserInGroup(
            cardset.groupId,
            managerId,
          );
        }

        const existing =
          await this.cardsetManagerRepository.findAllByCardSetId(id);
        for (const m of existing) {
          await this.cardsetManagerRepository.delete(m.id);
        }
        for (const managerId of newManagerIds) {
          const cardsetManager = CardsetManager.create({
            userId: managerId,
            cardSetId: id,
          });
          await this.cardsetManagerRepository.save(cardsetManager, manager);
        }
      }

      return this.cardsetRepository.update(id, dto);
    });
  }

  async remove(id: number, userId: number): Promise<void> {
    await this.checkIsManager(id, userId);
    return this.cardsetRepository.delete(id);
  }

  async isCardSetViewable(cardSetId: number, userId: number): Promise<boolean> {
    const cardset = await this.cardsetRepository.findById(cardSetId);
    if (!cardset) return false;
    if (cardset.visibility === Visibility.PUBLIC) return true;
    return this.groupGrpcClient.isUserInGroup(cardset.groupId, userId);
  }

  async getCardSetsByIds(
    cardSetIds: number[],
    userId: number,
  ): Promise<Cardset[]> {
    const cardsets = await this.cardsetRepository.findByIds(cardSetIds);
    const viewable: Cardset[] = [];
    for (const cardset of cardsets) {
      if (cardset.visibility === Visibility.PUBLIC) {
        viewable.push(cardset);
      } else {
        const inGroup = await this.groupGrpcClient.isUserInGroup(
          cardset.groupId,
          userId,
        );
        if (inGroup) viewable.push(cardset);
      }
    }
    return viewable;
  }

  async updateCardCount(
    id: number,
    userId: number,
    newCardCount: number,
  ): Promise<Cardset | null> {
    await this.checkIsManager(id, userId);

    return this.dataSource.transaction(async (manager) => {
      const cardset = await this.cardsetRepository.findById(id);
      if (!cardset) return null;

      const currentCards = await this.cardRepository.findAllByCardsetId(id);
      const currentCount = currentCards.length;

      if (newCardCount > currentCount) {
        const cardsToAdd = this.cardsetCardDomainService.buildCardsToAdd(
          id,
          currentCount,
          newCardCount,
        );
        for (const card of cardsToAdd) {
          await this.cardRepository.save(card, manager);
        }
      } else if (newCardCount < currentCount) {
        const cardsToRemove = this.cardsetCardDomainService.selectCardsToRemove(
          currentCards,
          newCardCount,
        );
        for (const card of cardsToRemove) {
          await this.cardRepository.delete(card.id, manager);
        }
      }

      const updatedCardset = cardset.changeCardCount(newCardCount);
      return this.cardsetRepository.update(id, updatedCardset);
    });
  }

  async saveCards(cardSetId: number, userId: number): Promise<void> {
    this.logger.log(
      `[saveCards] 권한 확인 시작 - cardSetId=${cardSetId}, userId=${userId}`,
    );
    await this.checkIsManager(cardSetId, userId);
    this.logger.log(
      `[saveCards] 권한 확인 완료 - cardSetId=${cardSetId}, userId=${userId}`,
    );
    await this.collaborationUseCase.saveCardsetContent(cardSetId);
    this.logger.log(`[saveCards] Yjs → DB 저장 완료 - cardSetId=${cardSetId}`);
  }

  async findCardsFromYjs(
    cardSetId: number,
  ): Promise<{ id: string; question: string; answer: string }[]> {
    const cards = await this.collaborationUseCase.getCards(cardSetId);
    this.logger.log(`[cardset:${cardSetId}] cards: ${JSON.stringify(cards)}`);
    return cards;
  }
}
