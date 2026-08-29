package flipnote.user.interfaces.http;

import flipnote.user.application.UserService;
import flipnote.user.application.result.MyInfoResult;
import flipnote.user.application.result.UserInfoResult;
import flipnote.user.application.result.UserUpdateResult;
import flipnote.user.interfaces.http.common.HttpConstants;
import flipnote.user.interfaces.http.dto.request.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<MyInfoResult> getMyInfo(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(userService.getMyInfo(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResult> getUserInfo(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

    @PutMapping
    public ResponseEntity<UserUpdateResult> updateProfile(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request.toCommand()));
    }

    @DeleteMapping
    public ResponseEntity<Void> withdraw(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }
}
