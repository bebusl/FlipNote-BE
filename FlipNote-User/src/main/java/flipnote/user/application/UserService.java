package flipnote.user.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.image.grpc.v1.ActivateImageRequest;
import flipnote.image.grpc.v1.ActivateImageResponse;
import flipnote.image.grpc.v1.ChangeImageRequest;
import flipnote.image.grpc.v1.ChangeImageResponse;
import flipnote.image.grpc.v1.ImageCommandServiceGrpc;
import flipnote.image.grpc.v1.Type;
import flipnote.user.application.command.UpdateProfileCommand;
import flipnote.user.application.result.MyInfoResult;
import flipnote.user.application.result.TokenValidateResult;
import flipnote.user.application.result.UserInfoResult;
import flipnote.user.application.result.UserResult;
import flipnote.user.application.result.UserUpdateResult;
import flipnote.user.domain.ImageErrorCode;
import flipnote.user.domain.UserErrorCode;
import flipnote.user.domain.common.BizException;
import flipnote.user.domain.entity.User;
import flipnote.user.domain.repository.UserRepository;
import flipnote.user.infrastructure.jwt.JwtProvider;
import flipnote.user.infrastructure.redis.SessionInvalidationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SessionInvalidationRepository sessionInvalidationRepository;
    private final JwtProvider jwtProvider;
    private final AuthService authService;
    private final ImageCommandServiceGrpc.ImageCommandServiceBlockingStub imageCommandServiceStub;

    public MyInfoResult getMyInfo(Long userId) {
        User user = findActiveUser(userId);
        return MyInfoResult.from(user);
    }

    public UserInfoResult getUserInfo(Long userId) {
        User user = userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
        return UserInfoResult.from(user);
    }

    @Transactional
    public UserUpdateResult updateProfile(Long userId, UpdateProfileCommand command) {
        User user = findActiveUser(userId);

        String profileImageUrl = null;
        if (command.imageRefId() != null) {
            try {
                if (User.DEFAULT_PROFILE_IMAGE_URL.equals(user.getProfileImageUrl())) {
                    ActivateImageResponse activateImageResponse = imageCommandServiceStub.activateImage(
                            ActivateImageRequest.newBuilder()
                                    .setReferenceType(Type.USER)
                                    .setReferenceId(userId)
                                    .setImageRefId(command.imageRefId())
                                    .build());
                    profileImageUrl = activateImageResponse.getUrl();
                } else {
                    ChangeImageResponse changeImageResponse = imageCommandServiceStub.changeImage(
                            ChangeImageRequest.newBuilder()
                                    .setReferenceType(Type.USER)
                                    .setReferenceId(userId)
                                    .setImageRefId(command.imageRefId())
                                    .build());
                    profileImageUrl = changeImageResponse.getUrl();
                }
            } catch (Exception ex) {
                log.error("updateProfile", ex);
                throw new BizException(ImageErrorCode.IMAGE_SERVICE_ERROR);
            }
        }

        user.updateProfile(command.nickname(), command.phone(), Boolean.TRUE.equals(command.smsAgree()), profileImageUrl);
        return UserUpdateResult.from(user, command.imageRefId());
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = findActiveUser(userId);
        user.withdraw();
        sessionInvalidationRepository.invalidate(userId, jwtProvider.getRefreshTokenExpiration());
    }

    public Optional<UserResult> findActiveUserById(Long userId) {
        return userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
                .map(UserResult::from);
    }

    public List<UserResult> findActiveUsersByIds(List<Long> userIds) {
        return userRepository.findByIdInAndStatus(userIds, User.Status.ACTIVE)
                .stream().map(UserResult::from).toList();
    }

    public Optional<UserResult> findActiveUserByEmail(String email) {
        return userRepository.findByEmailAndStatus(email, User.Status.ACTIVE)
                .map(UserResult::from);
    }

    public UserResult findUserByToken(String token) {
        TokenValidateResult tokenResult = authService.validateToken(token);
        return UserResult.from(findActiveUser(tokenResult.userId()));
    }

    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
    }
}
