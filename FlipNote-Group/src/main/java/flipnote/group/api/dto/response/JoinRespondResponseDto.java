package flipnote.group.api.dto.response;

import flipnote.group.application.port.in.result.JoinRespondResult;
import flipnote.group.domain.model.join.JoinStatus;

public record JoinRespondResponseDto(
	Long joinId,
	JoinStatus status
) {
	public static JoinRespondResponseDto of(JoinRespondResult result) {

		Long joinId = result.join().getId();
		JoinStatus status = result.join().getStatus();

		return new JoinRespondResponseDto(joinId, status);
	}
}
