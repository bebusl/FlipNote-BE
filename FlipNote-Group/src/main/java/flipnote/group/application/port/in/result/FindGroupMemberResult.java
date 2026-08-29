package flipnote.group.application.port.in.result;

import java.util.List;

import flipnote.group.domain.model.member.MemberInfo;

public record FindGroupMemberResult(
	List<MemberInfo> memberInfoList
) {
}
