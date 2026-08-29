import { BaseDomainEntity } from '../../../shared/domain/base.entity';

export class Card extends BaseDomainEntity {
  constructor(
    public readonly id: number,
    public content: string,
    public order: number,
    public readonly cardsetId: number,
    createdAt: Date,
    updatedAt: Date,
    deletedAt?: Date,
  ) {
    super(createdAt, updatedAt, deletedAt);
  }

  static create(props: {
    content?: string;
    order: number;
    cardsetId: number;
  }): Card {
    return new Card(
      0,
      props.content ?? '',
      props.order,
      props.cardsetId,
      new Date(),
      new Date(),
    );
  }

  updateContent(content: string): Card {
    return new Card(
      this.id,
      content,
      this.order,
      this.cardsetId,
      this.createdAt,
      new Date(),
      this.deletedAt,
    );
  }

  changeOrder(order: number): Card {
    return new Card(
      this.id,
      this.content,
      order,
      this.cardsetId,
      this.createdAt,
      new Date(),
      this.deletedAt,
    );
  }
}
