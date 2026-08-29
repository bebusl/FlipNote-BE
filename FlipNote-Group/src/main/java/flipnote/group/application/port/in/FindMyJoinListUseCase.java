package flipnote.group.application.port.in;

import flipnote.group.application.port.in.result.FindMyJoinListResult;

public interface FindMyJoinListUseCase {
	FindMyJoinListResult findMyJoinList(Long userId);
}
