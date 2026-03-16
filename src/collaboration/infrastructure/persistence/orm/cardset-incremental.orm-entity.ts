import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('cardset_incrementals')
export class CardsetIncrementalOrmEntity {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column({ name: 'cardset_id', type: 'int' })
  cardsetId!: number;

  @Column({ name: 'incremental_value', type: 'blob' })
  incrementalValue!: Buffer;

  @Column({ name: 'is_flushed', type: 'boolean', default: false })
  isFlushed!: boolean;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;
}
