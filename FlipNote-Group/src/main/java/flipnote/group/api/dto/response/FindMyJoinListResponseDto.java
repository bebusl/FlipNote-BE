package flipnote.group.api.dto.response;

import java.util.List;

import flipnote.group.application.port.in.result.FindMyJoinListResult;
import flipnote.group.domain.model.join.JoinMyInfo;

public record FindMyJoinListResponseDto(
	List<JoinMyInfo> joinList
) {
	public static FindMyJoinListResponseDto from(FindMyJoinListResult result) {

		return new FindMyJoinListResponseDto(result.joinList());
	}
}
