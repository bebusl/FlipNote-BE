import { BaseDomainEntity } from '../../../shared/domain/base.entity';
import { Visibility } from './visibility';

export class Cardset extends BaseDomainEntity {
  constructor(
    public readonly id: number,
    public name: string,
    public groupId: number,
    public visibility: Visibility,
    public category: string,
    public hashtag: string | null,
    public readonly imageRefId: number | null,
    public cardCount: number,
    createdAt: Date,
    updatedAt: Date,
    deletedAt?: Date,
  ) {
    super(createdAt, updatedAt, deletedAt);
  }

  static create(props: {
    name: string;
    groupId: number;
    visibility: Visibility;
    category: string;
    hashtag?: string | null;
    imageRefId?: number | null;
    cardCount?: number;
  }): Cardset {
    return new Cardset(
      0,
      props.name,
      props.groupId,
      props.visibility,
      props.category,
      props.hashtag ?? null,
      props.imageRefId ?? null,
      props.cardCount ?? 10,
      new Date(),
      new Date(),
    );
  }

  updateInfo(
    props: Partial<{
      name: string;
      visibility: Visibility;
      category: string;
      hashtag: string | null;
    }>,
  ): Cardset {
    return new Cardset(
      this.id,
      props.name ?? this.name,
      this.groupId,
      props.visibility ?? this.visibility,
      props.category ?? this.category,
      props.hashtag !== undefined ? props.hashtag : this.hashtag,
      this.imageRefId,
      this.cardCount,
      this.createdAt,
      new Date(),
      this.deletedAt,
    );
  }

  changeCardCount(newCount: number): Cardset {
    return new Cardset(
      this.id,
      this.name,
      this.groupId,
      this.visibility,
      this.category,
      this.hashtag,
      this.imageRefId,
      newCount,
      this.createdAt,
      new Date(),
      this.deletedAt,
    );
  }
}
