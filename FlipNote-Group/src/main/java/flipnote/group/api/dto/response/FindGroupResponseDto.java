package flipnote.group.api.dto.response;

import java.time.LocalDateTime;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.application.port.in.result.FindGroupResult;
import flipnote.group.domain.model.group.Category;
import flipnote.group.domain.model.group.JoinPolicy;
import flipnote.group.domain.model.group.Visibility;

public record FindGroupResponseDto(
	Long groupId,

	String name,

	Category category,

	String description,

	JoinPolicy joinPolicy,

	Visibility visibility,

	Integer maxMember,

	Long imageRefId,

	String imageUrl,

	LocalDateTime createdAt,

	LocalDateTime modifiedAt
) {
	public static FindGroupResponseDto from(FindGroupResult result) {

		return new FindGroupResponseDto(
			result.id(),
			result.name(),
			result.category(),
			result.description(),
			result.joinPolicy(),
			result.visibility(),
			result.maxMember(),
			result.imageRefId(),
			result.imageUrl(),
			result.createdAt(),
			result.modifiedAt()
		);
	}
}
