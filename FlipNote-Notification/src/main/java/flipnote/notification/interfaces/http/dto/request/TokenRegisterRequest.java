package flipnote.notification.interfaces.http.dto.request;

import jakarta.validation.constraints.NotEmpty;

public record TokenRegisterRequest(
	@NotEmpty
	String token
) {
}
