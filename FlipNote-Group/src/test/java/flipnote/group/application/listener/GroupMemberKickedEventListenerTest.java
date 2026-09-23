package flipnote.group.application.listener;

import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

import flipnote.group.domain.event.GroupMemberKickedEvent;
import flipnote.group.infrastructure.messaging.EditorAccessMessageProducer;

class GroupMemberKickedEventListenerTest {
	@Test
	void sendsMessageOnlyAfterCommitAndNeverAfterRollback() {
		var rabbit = mock(RabbitTemplate.class);
		try (var context = new AnnotationConfigApplicationContext()) {
			context.registerBean(TransactionalEventListenerFactory.class);
			context.registerBean(EditorAccessMessageProducer.class, () -> new EditorAccessMessageProducer(rabbit));
			context.registerBean(GroupMemberKickedEventListener.class);
			context.refresh();
			var transactions = new TestTransactionManager();
			var committed = transactions.getTransaction(new DefaultTransactionDefinition());
			context.publishEvent(new GroupMemberKickedEvent(10L, 7L));
			verifyNoInteractions(rabbit);
			transactions.commit(committed);
			verify(rabbit).convertAndSend("editor-access.exchange", "group.member.kicked", Map.of(
				"eventType", "GROUP_MEMBER_KICKED", "groupId", 10L, "userId", 7L, "reason", "GROUP_MEMBER_KICKED"));
			clearInvocations(rabbit);
			var rolledBack = transactions.getTransaction(new DefaultTransactionDefinition());
			context.publishEvent(new GroupMemberKickedEvent(10L, 7L));
			transactions.rollback(rolledBack);
			verifyNoInteractions(rabbit);
		}
	}

	private static class TestTransactionManager extends AbstractPlatformTransactionManager {
		@Override protected Object doGetTransaction() { return new Object(); }
		@Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
		@Override protected void doCommit(DefaultTransactionStatus status) { }
		@Override protected void doRollback(DefaultTransactionStatus status) { }
	}
}
