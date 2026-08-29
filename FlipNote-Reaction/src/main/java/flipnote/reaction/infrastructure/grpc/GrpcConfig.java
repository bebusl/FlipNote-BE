package flipnote.reaction.infrastructure.grpc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

import cardset.CardsetServiceGrpc;

@Configuration
public class GrpcConfig {

	@Bean
	public CardsetServiceGrpc.CardsetServiceBlockingStub cardSetStub(GrpcChannelFactory grpcChannelFactory) {
		return CardsetServiceGrpc.newBlockingStub(grpcChannelFactory.createChannel("cardset"));
	}
}
