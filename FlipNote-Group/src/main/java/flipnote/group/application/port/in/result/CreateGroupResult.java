package flipnote.group.application.port.in.result;

public record CreateGroupResult(
	Long groupId
) {
	public static CreateGroupResult of(Long groupId) {
		return new CreateGroupResult(groupId);
	}
}
