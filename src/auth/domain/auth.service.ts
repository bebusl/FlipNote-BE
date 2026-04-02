import * as jwt from 'jsonwebtoken';
import { Inject, Injectable, UnauthorizedException } from '@nestjs/common';
import authConfig from '../../shared/config/auth.config';
import type { ConfigType } from '@nestjs/config';
import type { UserAuth } from '../../shared/types/user-auth.type';

interface JwtUser {
  user_id: string;
  role: string;
  token_version: number;
}

@Injectable()
export class AuthService {
  constructor(
    @Inject(authConfig.KEY) private config: ConfigType<typeof authConfig>,
  ) {}

  verify(token: string): UserAuth {
    try {
      const payload = jwt.verify(token, this.config.jwtSecret!) as unknown as (
        | jwt.JwtPayload
        | string
      ) &
        JwtUser;

      const { user_id } = payload;

      return {
        userId: user_id,
        nickname: '',
      };
    } catch (e) {
      throw new UnauthorizedException(e);
    }
  }
}
