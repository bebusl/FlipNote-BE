package flipnote.group.api.dto.request;

import flipnote.group.domain.model.join.JoinStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record JoinRespondRequestDto(
	@NotNull(message = "그룹 신청 상태를 선택해주세요.")
	@Pattern(regexp = "ACCEPT|REJECT", message = "ACCEPT 또는 REJECT만 입력 가능합니다.")
	String status
) {
	public JoinStatus joinStatus() {
		return JoinStatus.valueOf(status);
	}
}
