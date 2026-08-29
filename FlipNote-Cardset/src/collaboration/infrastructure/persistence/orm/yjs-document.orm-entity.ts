import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('yjs_documents')
export class YjsDocumentOrmEntity {
  @PrimaryGeneratedColumn()
  id!: number;

  @Column({ name: 'cardset_id', type: 'int', unique: true })
  cardsetId!: number;

  @Column({ name: 'document_data', type: 'blob' })
  documentData!: Buffer;

  @Column({ name: 'version', type: 'int', default: 1 })
  version!: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
