package flipnote.group.api.dto.response;

public record CreateGroupResponseDto(
	Long groupId
) {
	public static CreateGroupResponseDto from(Long groupId) {
		return new CreateGroupResponseDto(groupId);
	}
}
