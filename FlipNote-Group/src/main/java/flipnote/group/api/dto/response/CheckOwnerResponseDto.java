package flipnote.group.api.dto.response;

import flipnote.group.application.port.in.result.CheckOwnerResult;

public record CheckOwnerResponseDto(
	boolean isOwner
) {
	public static CheckOwnerResponseDto from(CheckOwnerResult result) {
		return new CheckOwnerResponseDto(result.isOwner());
	}
}
