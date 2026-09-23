package flipnote.group.application.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import flipnote.group.domain.event.GroupMemberKickedEvent;
import flipnote.group.infrastructure.messaging.EditorAccessMessageProducer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GroupMemberKickedEventListener {

	private final EditorAccessMessageProducer editorAccessMessageProducer;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(GroupMemberKickedEvent event) {
		editorAccessMessageProducer.sendGroupMemberKicked(event.groupId(), event.userId());
	}
}
