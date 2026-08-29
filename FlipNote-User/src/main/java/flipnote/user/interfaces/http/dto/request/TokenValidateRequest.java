package flipnote.user.interfaces.http.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenValidateRequest {

    @NotBlank(message = "토큰은 필수입니다")
    private String token;
}
