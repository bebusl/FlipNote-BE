package flipnote.group.api.dto.response;

import flipnote.group.application.port.in.result.ApplicationFormResult;
import flipnote.group.domain.model.join.JoinStatus;

public record ApplicationFormResponseDto(
	Long groupJoinId,
	JoinStatus status
) {
	public static ApplicationFormResponseDto from(ApplicationFormResult result) {
		return new ApplicationFormResponseDto(result.join().getId(), result.join().getStatus());
	}
}
