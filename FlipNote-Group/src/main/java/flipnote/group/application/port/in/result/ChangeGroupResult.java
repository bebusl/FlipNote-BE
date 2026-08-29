package flipnote.group.application.port.in.result;

import java.time.LocalDateTime;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.domain.model.group.Category;
import flipnote.group.domain.model.group.JoinPolicy;
import flipnote.group.domain.model.group.Visibility;

public record ChangeGroupResult(
	Long id,
	String name,
	Category category,
	String description,
	JoinPolicy joinPolicy,
	Visibility visibility,
	Integer maxMember,
	String imageUrl,
	Long imageRefId,
	Integer memberCount,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {
	public static ChangeGroupResult of(GroupEntity group, String imageUrl) {
		return new ChangeGroupResult(
			group.getId(),
			group.getName(),
			group.getCategory(),
			group.getDescription(),
			group.getJoinPolicy(),
			group.getVisibility(),
			group.getMaxMember(),
			imageUrl,
			group.getImageRefId(),
			group.getMemberCount(),
			group.getCreatedAt(),
			group.getModifiedAt()
		);
	}
}
