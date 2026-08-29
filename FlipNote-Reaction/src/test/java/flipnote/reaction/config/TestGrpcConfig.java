package flipnote.reaction.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import flipnote.reaction.infrastructure.grpc.CardSetGrpcClient;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestGrpcConfig {

	@Bean
	public CardSetGrpcClient cardSetGrpcClient() {
		return mock(CardSetGrpcClient.class);
	}
}
