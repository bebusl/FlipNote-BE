package flipnote.group.domain.model.member;

public enum GroupMemberRole {

	OWNER(4),
	HEAD_MANAGER(3),
	MANAGER(2),
	MEMBER(1);

	private final int priority;

	GroupMemberRole(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}

	//권한 초과
	public boolean isHigherThan(GroupMemberRole other) {
		return this.priority > other.priority;
	}
}
