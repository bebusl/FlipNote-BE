import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { BaseOrmEntity } from '../../../../shared/infrastructure/persistence/base.orm-entity';
import { CardsetOrmEntity } from './cardset.orm-entity';

@Entity('cards')
export class CardOrmEntity extends BaseOrmEntity {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column('text')
  content!: string;

  @Column({ default: 0 })
  order!: number;

  @Column()
  cardsetId!: number;

  @ManyToOne(() => CardsetOrmEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'cardsetId' })
  cardset!: CardsetOrmEntity;
}
