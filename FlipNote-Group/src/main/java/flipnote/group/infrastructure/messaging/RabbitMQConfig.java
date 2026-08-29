package flipnote.group.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String EXCHANGE = "flipnote.notification";
	public static final String GROUP_INVITE_ROUTING_KEY = "notification.group.invite";
	public static final String GROUP_JOIN_REQUEST_ROUTING_KEY = "notification.group.join-request";

	@Bean
	public TopicExchange notificationExchange() {
		return new TopicExchange(EXCHANGE, true, false);
	}

	@Bean
	public MessageConverter jackson2JsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jackson2JsonMessageConverter());
		return template;
	}
}
