package flipnote.group.domain.model.join;

import flipnote.group.adapter.out.entity.JoinEntity;

public record JoinMyInfo(
	Long groupJoinId,
	String joinIntro,
	JoinStatus status,
	Long groupId,
	String groupName
) {
	public static JoinMyInfo of(JoinEntity join, String groupName) {
		return new JoinMyInfo(join.getId(), join.getForm(), join.getStatus(), join.getGroupId(), groupName);
	}
}
