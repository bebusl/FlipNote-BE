package flipnote.group.api.dto.response;

import java.time.LocalDateTime;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.application.port.in.result.ChangeGroupResult;
import flipnote.group.domain.model.group.Category;
import flipnote.group.domain.model.group.JoinPolicy;
import flipnote.group.domain.model.group.Visibility;

public record ChangeGroupResponseDto(
	Long groupId,

	String name,

	Category category,

	String description,

	JoinPolicy joinPolicy,

	Visibility visibility,

	Integer maxMember,

	Long imageRefId,

	LocalDateTime createdAt,

	LocalDateTime modifiedAt
) {
	public static ChangeGroupResponseDto from(ChangeGroupResult result) {

		return new ChangeGroupResponseDto(
			result.id(),
			result.name(),
			result.category(),
			result.description(),
			result.joinPolicy(),
			result.visibility(),
			result.maxMember(),
			result.imageRefId(),
			result.createdAt(),
			result.modifiedAt()
		);
	}
}
