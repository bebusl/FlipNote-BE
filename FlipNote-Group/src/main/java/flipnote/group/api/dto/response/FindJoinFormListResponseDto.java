package flipnote.group.api.dto.response;

import java.util.List;

import flipnote.group.application.port.in.result.FindJoinFormListResult;
import flipnote.group.domain.model.join.JoinInfo;

public record FindJoinFormListResponseDto(
	List<JoinInfo> joinList
) {
	public static FindJoinFormListResponseDto from(FindJoinFormListResult result) {
		return new FindJoinFormListResponseDto(result.joinInfoList());
	}
}
