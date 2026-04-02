import { Inject, Injectable, OnModuleInit } from '@nestjs/common';
import type { ClientGrpc } from '@nestjs/microservices';
import { Observable, firstValueFrom } from 'rxjs';
import { BusinessException } from '../../../shared/common/business.exception';
import { ErrorCode } from '../../../shared/common/error-code';

interface GroupCommandService {
  checkUserInGroup(data: {
    groupId: number;
    userId: number;
  }): Observable<{ exists: boolean }>;
  getMyGroup(data: { userId: number }): Observable<{ groupId: number[] }>;
}

@Injectable()
export class GroupGrpcClient implements OnModuleInit {
  private groupService!: GroupCommandService;

  constructor(
    @Inject('GROUP_GRPC_CLIENT') private readonly client: ClientGrpc,
  ) {}

  onModuleInit() {
    this.groupService = this.client.getService<GroupCommandService>(
      'GroupCommandService',
    );
  }

  async checkUserInGroup(groupId: number, userId: number): Promise<void> {
    const result = await firstValueFrom(
      this.groupService.checkUserInGroup({ groupId, userId }),
    );
    if (!result.exists) {
      throw new BusinessException(ErrorCode.GROUP_NOT_IN);
    }
  }

  async isUserInGroup(groupId: number, userId: number): Promise<boolean> {
    const result = await firstValueFrom(
      this.groupService.checkUserInGroup({ groupId, userId }),
    );
    return result.exists;
  }

  async getMyGroupIds(userId: number): Promise<Set<number>> {
    console.log('[getMyGroupIds] request userId:', userId);
    const result = await firstValueFrom(
      this.groupService.getMyGroup({ userId }),
    );
    console.log('[getMyGroupIds] raw result:', JSON.stringify(result));
    return new Set((result.groupId ?? []).map(Number));
  }
}
