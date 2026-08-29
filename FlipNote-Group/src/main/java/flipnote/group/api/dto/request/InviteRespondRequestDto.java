package flipnote.group.api.dto.request;

import flipnote.group.domain.model.invite.InviteResponseStatus;
import jakarta.validation.constraints.NotNull;

public record InviteRespondRequestDto(
	@NotNull(message = "응답 상태를 선택해주세요.")
	InviteResponseStatus status
) {
}
