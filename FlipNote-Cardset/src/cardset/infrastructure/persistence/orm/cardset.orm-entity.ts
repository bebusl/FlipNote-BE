import { Entity, PrimaryGeneratedColumn, Column } from 'typeorm';
import { BaseOrmEntity } from '../../../../shared/infrastructure/persistence/base.orm-entity';
import { Visibility } from '../../../domain/model/visibility';

@Entity('card_sets')
export class CardsetOrmEntity extends BaseOrmEntity {
  @PrimaryGeneratedColumn({ type: 'int' })
  id!: number;

  @Column({ type: 'varchar', length: 50, nullable: false })
  name!: string;

  @Column({ name: 'group_id', type: 'int', nullable: false })
  groupId!: number;

  @Column({
    type: 'enum',
    enum: Visibility,
    nullable: false,
    default: Visibility.PRIVATE,
  })
  visibility!: Visibility;

  @Column({ type: 'varchar', length: 50, nullable: false })
  category!: string;

  @Column({ type: 'varchar', nullable: true })
  hashtag?: string | null;

  @Column({ name: 'image_ref_id', type: 'bigint', nullable: true })
  imageRefId!: number | null;

  @Column({ name: 'card_count', type: 'int', default: 10 })
  cardCount!: number;
}
