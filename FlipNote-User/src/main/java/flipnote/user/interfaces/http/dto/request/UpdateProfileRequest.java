package flipnote.user.interfaces.http.dto.request;

import flipnote.user.application.command.UpdateProfileCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    private String nickname;

    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;

    @NotNull(message = "SMS 수신 동의 여부는 필수입니다")
    private Boolean smsAgree;

    private Long imageRefId;

    public UpdateProfileCommand toCommand() {
        return new UpdateProfileCommand(nickname, phone, smsAgree, imageRefId);
    }
}
