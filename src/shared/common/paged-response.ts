import { ApiProperty } from '@nestjs/swagger';

export class PagedResponse<T> {
  @ApiProperty({ isArray: true })
  items: T[];

  @ApiProperty({ example: 100 })
  total: number;

  @ApiProperty({ example: 1 })
  page: number;

  @ApiProperty({ example: 10 })
  size: number;

  constructor(items: T[], total: number, page: number, size: number) {
    this.items = items;
    this.total = total;
    this.page = page;
    this.size = size;
  }

  static of<T>(
    items: T[],
    total: number,
    page: number,
    size: number,
  ): PagedResponse<T> {
    return new PagedResponse(items, total, page, size);
  }
}
