export class YjsDocument {
  constructor(
    public readonly id: number,
    public readonly cardsetId: number,
    public documentData: Buffer,
    public version: number,
    public readonly createdAt: Date,
    public readonly updatedAt: Date,
  ) {}

  static create(props: {
    cardsetId: number;
    documentData: Buffer;
  }): YjsDocument {
    return new YjsDocument(
      0,
      props.cardsetId,
      props.documentData,
      1,
      new Date(),
      new Date(),
    );
  }

  withNewData(documentData: Buffer): YjsDocument {
    return new YjsDocument(
      this.id,
      this.cardsetId,
      documentData,
      this.version + 1,
      this.createdAt,
      new Date(),
    );
  }
}
