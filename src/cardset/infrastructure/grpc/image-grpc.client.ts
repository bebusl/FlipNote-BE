import { Inject, Injectable, OnModuleInit } from '@nestjs/common';
import type { ClientGrpc } from '@nestjs/microservices';
import { Observable, firstValueFrom } from 'rxjs';
import { BusinessException } from '../../../shared/common/business.exception';
import { ErrorCode } from '../../../shared/common/error-code';

enum ReferenceType {
  CARD_SET = 3,
}

interface ImageCommandService {
  getUrlByReference(data: {
    referenceType: ReferenceType;
    referenceId: number;
  }): Observable<{ imageUrl: string }>;

  activateImage(data: {
    imageRefId: number;
    referenceType: ReferenceType;
    referenceId: number;
  }): Observable<Record<string, never>>;

  changeImage(data: {
    referenceType: ReferenceType;
    referenceId: number;
    imageRefId: number;
  }): Observable<{ imageRefId: number; url: string }>;
}

@Injectable()
export class ImageGrpcClient implements OnModuleInit {
  private imageService!: ImageCommandService;

  constructor(
    @Inject('IMAGE_GRPC_CLIENT') private readonly client: ClientGrpc,
  ) {}

  onModuleInit() {
    this.imageService = this.client.getService<ImageCommandService>(
      'ImageCommandService',
    );
  }

  async getImageUrl(cardSetId: number): Promise<string> {
    try {
      const result = await firstValueFrom(
        this.imageService.getUrlByReference({
          referenceType: ReferenceType.CARD_SET,
          referenceId: cardSetId,
        }),
      );
      return result.imageUrl;
    } catch {
      throw new BusinessException(ErrorCode.IMAGE_SERVICE_ERROR);
    }
  }

  async activateImage(imageRefId: number, cardSetId: number): Promise<void> {
    try {
      await firstValueFrom(
        this.imageService.activateImage({
          imageRefId,
          referenceType: ReferenceType.CARD_SET,
          referenceId: cardSetId,
        }),
      );
    } catch {
      throw new BusinessException(ErrorCode.IMAGE_SERVICE_ERROR);
    }
  }

  async changeImage(imageRefId: number, cardSetId: number): Promise<void> {
    try {
      await firstValueFrom(
        this.imageService.changeImage({
          referenceType: ReferenceType.CARD_SET,
          referenceId: cardSetId,
          imageRefId,
        }),
      );
    } catch {
      throw new BusinessException(ErrorCode.IMAGE_SERVICE_ERROR);
    }
  }
}
