package flipnote.reaction.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

	public static final String EXCHANGE = "reaction.exchange";

	public static final String ROUTING_KEY_LIKE_ADDED = "reaction.like.added";
	public static final String ROUTING_KEY_LIKE_REMOVED = "reaction.like.removed";
	public static final String ROUTING_KEY_BOOKMARK_ADDED = "reaction.bookmark.added";
	public static final String ROUTING_KEY_BOOKMARK_REMOVED = "reaction.bookmark.removed";

	@Bean
	public TopicExchange reactionExchange() {
		return new TopicExchange(EXCHANGE);
	}

	@Bean
	public MessageConverter jackson2JsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}
}
