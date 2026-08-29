package flipnote.notification.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String EXCHANGE = "flipnote.notification";
	public static final String DLX_EXCHANGE = "flipnote.notification.dlx";
	public static final String DLQ = "flipnote.notification.dlq";

	public static final String GROUP_INVITE_QUEUE = "notification.group-invite.queue";
	public static final String GROUP_INVITE_ROUTING_KEY = "notification.group.invite";

	public static final String GROUP_JOIN_REQUEST_QUEUE = "notification.group-join-request.queue";
	public static final String GROUP_JOIN_REQUEST_ROUTING_KEY = "notification.group.join-request";

	@Bean
	public MessageConverter jackson2JsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	public TopicExchange notificationExchange() {
		return new TopicExchange(EXCHANGE, true, false);
	}

	@Bean
	public DirectExchange deadLetterExchange() {
		return new DirectExchange(DLX_EXCHANGE, true, false);
	}

	@Bean
	public Queue deadLetterQueue() {
		return QueueBuilder.durable(DLQ).build();
	}

	@Bean
	public Binding deadLetterBinding() {
		return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
	}

	@Bean
	public Queue groupInviteQueue() {
		return QueueBuilder.durable(GROUP_INVITE_QUEUE)
			.withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
			.withArgument("x-dead-letter-routing-key", DLQ)
			.build();
	}

	@Bean
	public Binding groupInviteBinding() {
		return BindingBuilder.bind(groupInviteQueue()).to(notificationExchange()).with(GROUP_INVITE_ROUTING_KEY);
	}

	@Bean
	public Queue groupJoinRequestQueue() {
		return QueueBuilder.durable(GROUP_JOIN_REQUEST_QUEUE)
			.withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
			.withArgument("x-dead-letter-routing-key", DLQ)
			.build();
	}

	@Bean
	public Binding groupJoinRequestBinding() {
		return BindingBuilder.bind(groupJoinRequestQueue()).to(notificationExchange())
			.with(GROUP_JOIN_REQUEST_ROUTING_KEY);
	}
}
