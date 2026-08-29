package flipnote.reaction;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import flipnote.reaction.config.TestGrpcConfig;
import flipnote.reaction.config.TestRabbitMqConfig;

@SpringBootTest
@Import({TestRabbitMqConfig.class, TestGrpcConfig.class})
class ReactionApplicationTests {

	@Test
	void contextLoads() {
	}

}
