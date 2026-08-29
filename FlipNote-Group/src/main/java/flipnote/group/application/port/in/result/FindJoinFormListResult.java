package flipnote.group.application.port.in.result;

import java.util.List;
import java.util.Map;

import flipnote.group.adapter.out.entity.JoinEntity;
import flipnote.group.domain.model.join.JoinInfo;
import flipnote.user.grpc.GetUserResponse;

public record FindJoinFormListResult(
	List<JoinInfo> joinInfoList
) {
	public static FindJoinFormListResult of(List<JoinEntity> joinList, Map<Long, GetUserResponse> userMap) {

		List<JoinInfo> results = joinList.stream()
			.map(join -> {
				GetUserResponse user = userMap.get(join.getUserId());
				return JoinInfo.of(join, user.getNickname());
			})
			.toList();
		return new FindJoinFormListResult(results);
	}
}
