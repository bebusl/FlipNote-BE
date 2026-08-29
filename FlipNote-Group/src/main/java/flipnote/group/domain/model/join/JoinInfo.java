package flipnote.group.domain.model.join;

import flipnote.group.adapter.out.entity.JoinEntity;

public record JoinInfo(
	Long groupJoinId,
	Long userId,
	String nickname,
	String joinIntro,
	JoinStatus status
	) {
	public static JoinInfo of(JoinEntity join, String nickname) {
		return new JoinInfo(join.getId(), join.getUserId(), nickname, join.getForm(), join.getStatus());
	}
}
