package flipnote.user.infrastructure.config;

import flipnote.image.grpc.v1.ImageCommandServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Bean
    ImageCommandServiceGrpc.ImageCommandServiceBlockingStub imageCommandServiceBlockingStub(
            GrpcChannelFactory channelFactory) {
        return ImageCommandServiceGrpc.newBlockingStub(channelFactory.createChannel("image-service"));
    }
}
