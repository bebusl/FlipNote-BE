package flipnote.group.application.port.in.result;

import java.util.List;

import flipnote.group.adapter.out.entity.JoinEntity;
import flipnote.group.domain.model.join.JoinMyInfo;

public record FindMyJoinListResult(
	List<JoinMyInfo> joinList
) {
	public static FindMyJoinListResult of(List<JoinMyInfo> joinList) {
		return new FindMyJoinListResult(joinList);
	}
}
