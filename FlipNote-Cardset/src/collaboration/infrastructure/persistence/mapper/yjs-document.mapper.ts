import { YjsDocument } from '../../../domain/model/yjs-document';
import { YjsDocumentOrmEntity } from '../orm/yjs-document.orm-entity';

export class YjsDocumentMapper {
  static toDomain(orm: YjsDocumentOrmEntity): YjsDocument {
    return new YjsDocument(
      orm.id,
      orm.cardsetId,
      orm.documentData,
      orm.version,
      orm.createdAt,
      orm.updatedAt,
    );
  }

  static toOrm(domain: YjsDocument): Partial<YjsDocumentOrmEntity> {
    return {
      id: domain.id || undefined,
      cardsetId: domain.cardsetId,
      documentData: domain.documentData,
      version: domain.version,
    };
  }
}
