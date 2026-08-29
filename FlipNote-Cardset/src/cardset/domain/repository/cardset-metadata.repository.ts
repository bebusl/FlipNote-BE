export const CARDSET_METADATA_REPOSITORY = Symbol(
  'CARDSET_METADATA_REPOSITORY',
);

export interface ICardSetMetadataRepository {
  findByCardSetId(
    cardSetId: number,
  ): Promise<{ likeCount: number; bookmarkCount: number } | null>;
  findByCardSetIds(
    cardSetIds: number[],
  ): Promise<Map<number, { likeCount: number; bookmarkCount: number }>>;
  upsertAndIncrementLike(cardSetId: number): Promise<void>;
  upsertAndDecrementLike(cardSetId: number): Promise<void>;
  upsertAndIncrementBookmark(cardSetId: number): Promise<void>;
  upsertAndDecrementBookmark(cardSetId: number): Promise<void>;
}
