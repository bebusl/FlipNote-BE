import { Module } from '@nestjs/common';
import { ClientsModule, Transport } from '@nestjs/microservices';
import { join } from 'path';

@Module({
  imports: [
    ClientsModule.register([
      {
        name: 'GROUP_GRPC_CLIENT',
        transport: Transport.GRPC,
        options: {
          package: 'group.v1',
          protoPath: join(__dirname, '../../proto/group.proto'),
          url: process.env.GROUP_GRPC_URL ?? 'localhost:9094',
          loader: { longs: Number },
        },
      },
      {
        name: 'IMAGE_GRPC_CLIENT',
        transport: Transport.GRPC,
        options: {
          package: 'image.v1',
          protoPath: join(__dirname, '../../proto/image.proto'),
          url: process.env.IMAGE_GRPC_URL ?? 'localhost:9092',
          loader: { longs: Number },
        },
      },
      {
        name: 'USER_GRPC_CLIENT',
        transport: Transport.GRPC,
        options: {
          package: 'user_query',
          protoPath: join(__dirname, '../../proto/user.proto'),
          url: process.env.USER_GRPC_URL ?? 'localhost:9091',
          loader: { longs: Number },
        },
      },
      {
        name: 'REACTION_GRPC_CLIENT',
        transport: Transport.GRPC,
        options: {
          package: 'reaction',
          protoPath: join(__dirname, '../../proto/reaction.proto'),
          url: process.env.GRPC_REACTION_URL ?? 'localhost:9093',
          loader: { longs: Number },
        },
      },
    ]),
  ],
  exports: [ClientsModule],
})
export class GrpcClientModule {}
