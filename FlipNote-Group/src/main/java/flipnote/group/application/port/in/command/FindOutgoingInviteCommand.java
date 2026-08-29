package flipnote.group.application.port.in.command;

import org.springframework.data.domain.Pageable;

public record FindOutgoingInviteCommand(
	Long userId,
	Long groupId,
	Pageable pageable
) {
}
