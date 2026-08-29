package flipnote.group.api.dto.response;

import flipnote.group.application.port.in.result.CreateInviteResult;

public record CreateInviteResponseDto(
	Long invitationId
) {
	public static CreateInviteResponseDto from(CreateInviteResult result) {
		return new CreateInviteResponseDto(result.invitationId());
	}
}
