package flipnote.group.domain.model.member;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberInfo {
	private Long memberId;
	private Long userId;
	private GroupMemberRole role;
	private String nickname;
	private String profileImage;

	@Builder
	private MemberInfo(Long memberId, Long userId, GroupMemberRole role) {
		this.memberId = memberId;
		this.userId = userId;
		this.role = role;
	}

	public void updateUserInfo(String nickname, String profileImage) {
		this.nickname = nickname;
		this.profileImage = profileImage;
	}

}
