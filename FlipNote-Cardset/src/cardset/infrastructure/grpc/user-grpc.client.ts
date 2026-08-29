import { Inject, Injectable, OnModuleInit } from '@nestjs/common';
import type { ClientGrpc } from '@nestjs/microservices';
import { Observable, firstValueFrom } from 'rxjs';

export interface UserInfo {
  id: number;
  email: string;
  nickname: string;
  profileImageUrl: string;
}

interface UserQueryService {
  getUsers(data: { userIds: number[] }): Observable<{ users: UserInfo[] }>;
  getUserByToken(data: { accessToken: string }): Observable<{
    userId: number;
    nickname: string;
  }>;
}

@Injectable()
export class UserGrpcClient implements OnModuleInit {
  private userService!: UserQueryService;

  constructor(
    @Inject('USER_GRPC_CLIENT') private readonly client: ClientGrpc,
  ) { }

  onModuleInit() {
    this.userService =
      this.client.getService<UserQueryService>('UserQueryService');
  }

  async getUsersByIds(userIds: number[]): Promise<UserInfo[]> {
    if (userIds.length === 0) return [];
    const result = await firstValueFrom(this.userService.getUsers({ userIds }));
    return result.users;
  }

  async getUserByToken(
    accessToken: string,
  ): Promise<{ userId: number; nickname: string }> {
    console.log('[getUserByToken] accessToken length:', accessToken?.length);
    console.log('[getUserByToken] accessToken:', accessToken);
    const result = await firstValueFrom(
      this.userService.getUserByToken({ accessToken }),
    );
    return { userId: result.userId, nickname: result.nickname };
  }
}
