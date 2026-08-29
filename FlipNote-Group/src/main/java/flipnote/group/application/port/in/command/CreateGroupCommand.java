package flipnote.group.application.port.in.command;

import flipnote.group.domain.model.group.Category;
import flipnote.group.domain.model.group.JoinPolicy;
import flipnote.group.domain.model.group.Visibility;

public record CreateGroupCommand(
	Long userId,
	String name,
	Category category,
	String description,
	JoinPolicy joinPolicy,
	Visibility visibility,
	int maxMember,
	Long imageRefId
) {
}
