import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { IYjsDocumentRepository } from '../../domain/repository/yjs-document.repository';
import { YjsDocument } from '../../domain/model/yjs-document';
import { YjsDocumentOrmEntity } from './orm/yjs-document.orm-entity';
import { YjsDocumentMapper } from './mapper/yjs-document.mapper';

@Injectable()
export class YjsDocumentRepositoryImpl implements IYjsDocumentRepository {
  constructor(
    @InjectRepository(YjsDocumentOrmEntity)
    private readonly ormRepository: Repository<YjsDocumentOrmEntity>,
  ) {}

  async findByCardsetId(cardsetId: number): Promise<YjsDocument | null> {
    const orm = await this.ormRepository.findOne({ where: { cardsetId } });
    return orm ? YjsDocumentMapper.toDomain(orm) : null;
  }

  async save(document: YjsDocument): Promise<YjsDocument> {
    const ormData = YjsDocumentMapper.toOrm(document);
    const created = this.ormRepository.create(ormData);
    const saved = await this.ormRepository.save(created);
    return YjsDocumentMapper.toDomain(saved);
  }

  async update(id: number, document: Partial<YjsDocument>): Promise<void> {
    await this.ormRepository.update(id, {
      documentData: document.documentData,
      version: document.version,
    });
  }

  async deleteByCardsetId(cardsetId: number): Promise<void> {
    await this.ormRepository.delete({ cardsetId });
  }
}
