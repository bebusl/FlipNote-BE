import {
  Column,
  CreateDateColumn,
  Entity,
  Index,
  PrimaryGeneratedColumn,
} from 'typeorm';

// Explicit saves append rows here. Existing snapshots are never updated.
@Entity('cardset_snapshot')
@Index('idx_cardset_snapshot_cardset_id_id', ['cardsetId', 'id'])
export class CardsetSnapshotOrmEntity {
  @PrimaryGeneratedColumn()
  id!: number;

  // Source identifier only: deleting the cardset must not cascade to snapshots.
  @Column({ name: 'cardset_id', type: 'int' })
  cardsetId!: number;

  @Column({ type: 'text' })
  content!: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;
}
