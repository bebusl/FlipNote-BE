import { ApiProperty } from '@nestjs/swagger';

export class ManagerInfoResponse {
  @ApiProperty({ example: 1 })
  id!: number;

  @ApiProperty({ example: 'user@example.com' })
  email!: string;

  @ApiProperty({ example: '닉네임' })
  nickname!: string;

  @ApiProperty({ example: 'https://example.com/profile.png' })
  profileImageUrl!: string;
}
