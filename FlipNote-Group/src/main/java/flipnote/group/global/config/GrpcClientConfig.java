package flipnote.group.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import flipnote.image.grpc.v1.ImageCommandServiceGrpc;
import flipnote.user.grpc.UserQueryServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Configuration
public class GrpcClientConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel imageCommandChannel(
        @Value("${spring.grpc.image.address:localhost:9092}") String target
    ) {
        return ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();
    }

    @Bean
    public ImageCommandServiceGrpc.ImageCommandServiceBlockingStub imageCommandServiceStub(
        ManagedChannel imageCommandChannel
    ) {
        return ImageCommandServiceGrpc.newBlockingStub(imageCommandChannel);
    }

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel userQueryChannel(
        @Value("${spring.grpc.user.address:localhost:9091}") String target
    ) {
        return ManagedChannelBuilder.forTarget(target)
            .usePlaintext()
            .build();
    }

    @Bean
    public UserQueryServiceGrpc.UserQueryServiceBlockingStub userQueryServiceStub(
        ManagedChannel userQueryChannel
    ) {
        return UserQueryServiceGrpc.newBlockingStub(userQueryChannel);
    }
}
