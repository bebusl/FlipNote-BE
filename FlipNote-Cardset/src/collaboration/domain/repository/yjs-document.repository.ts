import { YjsDocument } from '../model/yjs-document';

export const YJS_DOCUMENT_REPOSITORY = Symbol('YJS_DOCUMENT_REPOSITORY');

export interface IYjsDocumentRepository {
  findByCardsetId(cardsetId: number): Promise<YjsDocument | null>;
  save(document: YjsDocument): Promise<YjsDocument>;
  update(id: number, document: Partial<YjsDocument>): Promise<void>;
  deleteByCardsetId(cardsetId: number): Promise<void>;
}
