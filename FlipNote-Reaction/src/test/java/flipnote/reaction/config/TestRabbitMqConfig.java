package flipnote.reaction.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestRabbitMqConfig {

	@Bean
	public RabbitTemplate rabbitTemplate() {
		return mock(RabbitTemplate.class);
	}
}
