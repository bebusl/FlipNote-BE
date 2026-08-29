package flipnote.group.application.port.in;

public interface CheckUserInGroupUseCase {
	boolean checkUserInGroup(Long userId, Long groupId);
}
