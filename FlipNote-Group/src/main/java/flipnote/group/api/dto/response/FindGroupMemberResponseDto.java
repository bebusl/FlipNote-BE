package flipnote.group.api.dto.response;

import java.util.List;

import flipnote.group.application.port.in.result.FindGroupMemberResult;
import flipnote.group.domain.model.member.MemberInfo;

public record FindGroupMemberResponseDto(
	List<MemberInfo> memberInfoList
) {

	public static FindGroupMemberResponseDto from(FindGroupMemberResult result) {
		return new FindGroupMemberResponseDto(result.memberInfoList());
	}
}
