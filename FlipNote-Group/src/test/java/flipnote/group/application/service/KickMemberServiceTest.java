package flipnote.group.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.application.port.in.command.KickMemberCommand;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.domain.event.GroupMemberKickedEvent;
import flipnote.group.domain.model.permission.GroupPermission;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;

class KickMemberServiceTest {
	private final GroupMemberRepositoryPort members = mock(GroupMemberRepositoryPort.class);
	private final GroupRoleRepositoryPort roles = mock(GroupRoleRepositoryPort.class);
	private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
	private final KickMemberService service = new KickMemberService(members, roles, events);
	private final KickMemberCommand command = new KickMemberCommand(1L, 10L, 99L);

	@BeforeEach
	void permitManager() {
		when(roles.checkPermission(1L, 10L, GroupPermission.MEMBER_MANAGE)).thenReturn(true);
	}

	@Test
	void publishesTargetUserIdAfterDeletion() {
		when(members.findById(99L)).thenReturn(GroupMemberEntity.create(10L, 7L, null));
		service.kickMember(command);
		var order = inOrder(members, events);
		order.verify(members).deleteGroupMember(99L);
		order.verify(events).publishEvent(new GroupMemberKickedEvent(10L, 7L));
	}

	@Test
	void rejectsMemberFromAnotherGroup() {
		when(members.findById(99L)).thenReturn(GroupMemberEntity.create(20L, 7L, null));
		var error = assertThrows(BusinessException.class, () -> service.kickMember(command));
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, error.getErrorCode());
		verify(members, never()).deleteGroupMember(anyLong());
		verifyNoInteractions(events);
	}

	@Test
	void rejectsCallerWithoutPermission() {
		when(roles.checkPermission(1L, 10L, GroupPermission.MEMBER_MANAGE)).thenReturn(false);
		var error = assertThrows(BusinessException.class, () -> service.kickMember(command));
		assertEquals(ErrorCode.PERMISSION_DENIED, error.getErrorCode());
		verifyNoInteractions(members, events);
	}

	@Test
	void doesNotPublishWhenDeletionFails() {
		when(members.findById(99L)).thenReturn(GroupMemberEntity.create(10L, 7L, null));
		doThrow(new IllegalStateException("DB failure")).when(members).deleteGroupMember(99L);
		assertThrows(IllegalStateException.class, () -> service.kickMember(command));
		verifyNoInteractions(events);
	}
}
