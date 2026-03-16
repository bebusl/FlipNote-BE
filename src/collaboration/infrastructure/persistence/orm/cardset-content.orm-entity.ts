import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('cardset_content')
export class CardsetContentOrmEntity {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column({ name: 'cardset_id', type: 'int', unique: true })
  cardsetId!: number;

  @Column({ type: 'text', nullable: true })
  content!: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
