package flipnote.reaction.infrastructure.grpc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cardset.Cardset.GetCardSetsByIdsRequest;
import cardset.Cardset.GetCardSetsByIdsResponse;
import cardset.Cardset.IsCardSetViewableRequest;
import cardset.Cardset.IsCardSetViewableResponse;
import cardset.CardsetServiceGrpc;
import flipnote.reaction.application.common.CardSetSummaryResult;
import flipnote.reaction.domain.common.BizException;
import flipnote.reaction.domain.common.CommonErrorCode;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CardSetGrpcClient {

	private final CardsetServiceGrpc.CardsetServiceBlockingStub stub;

	public CardSetGrpcClient(CardsetServiceGrpc.CardsetServiceBlockingStub cardSetStub) {
		this.stub = cardSetStub;
	}

	public boolean isCardSetViewable(Long cardSetId, Long userId) {
		try {
			IsCardSetViewableRequest request = IsCardSetViewableRequest.newBuilder()
				.setCardSetId(cardSetId)
				.setUserId(userId)
				.build();

			IsCardSetViewableResponse response = stub.isCardSetViewable(request);
			return response.getViewable();
		} catch (StatusRuntimeException e) {
			log.error("gRPC call failed: IsCardSetViewable, cardSetId={}, userId={}", cardSetId, userId, e);
			throw new BizException(CommonErrorCode.GRPC_CALL_FAILED);
		}
	}

	public Map<Long, CardSetSummaryResult> getCardSetsByIds(List<Long> cardSetIds, Long userId) {
		try {
			GetCardSetsByIdsRequest request = GetCardSetsByIdsRequest.newBuilder()
				.addAllCardSetIds(cardSetIds)
				.setUserId(userId)
				.build();

			GetCardSetsByIdsResponse response = stub.getCardSetsByIds(request);
			return response.getCardSetsList().stream()
				.collect(Collectors.toMap(
					cs -> (long) cs.getId(),
					cs -> new CardSetSummaryResult(
						(long) cs.getId(),
						cs.getName(),
						(long) cs.getGroupId(),
						cs.getVisibility(),
						cs.getCategory(),
						cs.getHashtag(),
						(long) cs.getImageRefId(),
						(long) cs.getCardCount()
					)
				));
		} catch (StatusRuntimeException e) {
			log.error("gRPC call failed: GetCardSetsByIds, cardSetIds={}, userId={}", cardSetIds, userId, e);
			throw new BizException(CommonErrorCode.GRPC_CALL_FAILED);
		}
	}
}
