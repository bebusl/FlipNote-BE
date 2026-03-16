import { Entity, PrimaryGeneratedColumn, Column, Unique } from 'typeorm';

@Entity('card_set_metadata')
@Unique(['cardSetId'])
export class CardSetMetadataOrmEntity {
  @PrimaryGeneratedColumn({ type: 'bigint' })
  id!: number;

  @Column({ name: 'card_set_id', type: 'bigint', nullable: false })
  cardSetId!: number;

  @Column({ name: 'like_count', type: 'bigint', default: 0 })
  likeCount!: number;

  @Column({ name: 'bookmark_count', type: 'bigint', default: 0 })
  bookmarkCount!: number;
}
