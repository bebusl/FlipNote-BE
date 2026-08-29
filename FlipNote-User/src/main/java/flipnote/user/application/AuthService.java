package flipnote.user.application;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.user.application.command.ChangePasswordCommand;
import flipnote.user.application.command.LoginCommand;
import flipnote.user.application.command.SignupCommand;
import flipnote.user.application.result.SocialLinksResult;
import flipnote.user.application.result.TokenValidateResult;
import flipnote.user.application.result.UserRegisterResult;
import flipnote.user.domain.AuthErrorCode;
import flipnote.user.domain.TokenClaims;
import flipnote.user.domain.TokenPair;
import flipnote.user.domain.UserErrorCode;
import flipnote.user.domain.common.BizException;
import flipnote.user.domain.entity.OAuthLink;
import flipnote.user.domain.entity.User;
import flipnote.user.domain.event.EmailVerificationSendEvent;
import flipnote.user.domain.event.PasswordResetCreateEvent;
import flipnote.user.domain.repository.OAuthLinkRepository;
import flipnote.user.domain.repository.UserRepository;
import flipnote.user.infrastructure.config.ClientProperties;
import flipnote.user.infrastructure.jwt.JwtProvider;
import flipnote.user.infrastructure.mail.PasswordResetTokenGenerator;
import flipnote.user.infrastructure.mail.VerificationCodeGenerator;
import flipnote.user.infrastructure.redis.EmailVerificationRepository;
import flipnote.user.infrastructure.redis.PasswordResetRepository;
import flipnote.user.infrastructure.redis.SessionInvalidationRepository;
import flipnote.user.infrastructure.redis.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final TokenBlacklistRepository tokenBlacklistRepository;
	private final EmailVerificationRepository emailVerificationRepository;
	private final PasswordResetRepository passwordResetRepository;
	private final OAuthLinkRepository oAuthLinkRepository;
	private final SessionInvalidationRepository sessionInvalidationRepository;
	private final VerificationCodeGenerator verificationCodeGenerator;
	private final PasswordResetTokenGenerator passwordResetTokenGenerator;
	private final ClientProperties clientProperties;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public UserRegisterResult register(SignupCommand command) {
		if (!emailVerificationRepository.isVerified(command.email())) {
			throw new BizException(AuthErrorCode.UNVERIFIED_EMAIL);
		}

		if (userRepository.existsByEmail(command.email())) {
			throw new BizException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}

		User user = User.builder()
			.email(command.email())
			.password(passwordEncoder.encode(command.password()))
			.name(command.name())
			.nickname(command.nickname())
			.phone(command.phone())
			.smsAgree(command.smsAgree())
			.build();

		User savedUser = userRepository.save(user);
		return UserRegisterResult.from(savedUser);
	}

	public TokenPair login(LoginCommand command) {
		User user = userRepository.findByEmailAndStatus(command.email(), User.Status.ACTIVE)
			.orElseThrow(() -> new BizException(AuthErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(command.password(), user.getPassword())) {
			throw new BizException(AuthErrorCode.INVALID_CREDENTIALS);
		}

		return jwtProvider.generateTokenPair(user);
	}

	public void logout(String refreshToken) {
		if (refreshToken != null && jwtProvider.isTokenValid(refreshToken)) {
			long remaining = jwtProvider.getRemainingExpiration(refreshToken);
			if (remaining > 0) {
				tokenBlacklistRepository.add(refreshToken, remaining);
			}
		}
	}

	public TokenPair refreshToken(String refreshToken) {
		if (refreshToken == null || !jwtProvider.isTokenValid(refreshToken)) {
			throw new BizException(AuthErrorCode.INVALID_TOKEN);
		}

		if (tokenBlacklistRepository.isBlacklisted(refreshToken)) {
			throw new BizException(AuthErrorCode.BLACKLISTED_TOKEN);
		}

		TokenClaims claims = jwtProvider.extractClaims(refreshToken);

		sessionInvalidationRepository.getInvalidatedAtMillis(claims.userId()).ifPresent(invalidatedAtMillis -> {
			if (jwtProvider.getIssuedAt(refreshToken).getTime() < invalidatedAtMillis) {
				throw new BizException(AuthErrorCode.INVALIDATED_SESSION);
			}
		});

		User user = findActiveUser(claims.userId());

		long remaining = jwtProvider.getRemainingExpiration(refreshToken);
		if (remaining > 0) {
			tokenBlacklistRepository.add(refreshToken, remaining);
		}

		return jwtProvider.generateTokenPair(user);
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordCommand command) {
		User user = findActiveUser(userId);

		if (!passwordEncoder.matches(command.currentPassword(), user.getPassword())) {
			throw new BizException(AuthErrorCode.PASSWORD_MISMATCH);
		}

		user.changePassword(passwordEncoder.encode(command.newPassword()));
		sessionInvalidationRepository.invalidate(user.getId(), jwtProvider.getRefreshTokenExpiration());
	}

	public TokenValidateResult validateToken(String token) {
		if (!jwtProvider.isTokenValid(token)) {
			throw new BizException(AuthErrorCode.INVALID_TOKEN);
		}

		if (tokenBlacklistRepository.isBlacklisted(token)) {
			throw new BizException(AuthErrorCode.BLACKLISTED_TOKEN);
		}

		TokenClaims claims = jwtProvider.extractClaims(token);

		sessionInvalidationRepository.getInvalidatedAtMillis(claims.userId()).ifPresent(invalidatedAtMillis -> {
			if (jwtProvider.getIssuedAt(token).getTime() < invalidatedAtMillis) {
				throw new BizException(AuthErrorCode.INVALIDATED_SESSION);
			}
		});

		return new TokenValidateResult(claims.userId(), claims.email(), claims.role());
	}

	public void sendEmailVerificationCode(String email) {
		if (emailVerificationRepository.hasCode(email)) {
			throw new BizException(AuthErrorCode.ALREADY_ISSUED_VERIFICATION_CODE);
		}

		String code = verificationCodeGenerator.generate();
		emailVerificationRepository.saveCode(email, code);
		eventPublisher.publishEvent(new EmailVerificationSendEvent(email, code));
	}

	public void verifyEmail(String email, String code) {
		if (!emailVerificationRepository.hasCode(email)) {
			throw new BizException(AuthErrorCode.NOT_ISSUED_VERIFICATION_CODE);
		}

		String savedCode = emailVerificationRepository.getCode(email);
		if (!code.equals(savedCode)) {
			throw new BizException(AuthErrorCode.INVALID_VERIFICATION_CODE);
		}

		emailVerificationRepository.deleteCode(email);
		emailVerificationRepository.markVerified(email);
	}

	public void requestPasswordReset(String email) {
		if (!userRepository.existsByEmail(email)) {
			return;
		}

		if (passwordResetRepository.hasToken(email)) {
			throw new BizException(AuthErrorCode.ALREADY_SENT_PASSWORD_RESET_LINK);
		}

		String token = passwordResetTokenGenerator.generate();
		passwordResetRepository.save(token, email);

		String link = clientProperties.getUrl() + clientProperties.getPaths().getPasswordReset()
			+ "?token=" + token;
		eventPublisher.publishEvent(new PasswordResetCreateEvent(email, link));
	}

	@Transactional
	public void resetPassword(String token, String newPassword) {
		String email = passwordResetRepository.findEmailByToken(token);
		if (email == null) {
			throw new BizException(AuthErrorCode.INVALID_PASSWORD_RESET_TOKEN);
		}

		User user = userRepository.findByEmailAndStatus(email, User.Status.ACTIVE)
			.orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));

		user.changePassword(passwordEncoder.encode(newPassword));
		sessionInvalidationRepository.invalidate(user.getId(), jwtProvider.getRefreshTokenExpiration());
		passwordResetRepository.delete(token, email);
	}

	public SocialLinksResult getSocialLinks(Long userId) {
		List<OAuthLink> links = oAuthLinkRepository.findByUser_Id(userId);
		return SocialLinksResult.from(links);
	}

	@Transactional
	public void deleteSocialLink(Long userId, Long socialLinkId) {
		if (!oAuthLinkRepository.existsByIdAndUser_Id(socialLinkId, userId)) {
			throw new BizException(AuthErrorCode.NOT_REGISTERED_SOCIAL_ACCOUNT);
		}
		oAuthLinkRepository.deleteById(socialLinkId);
	}

	private User findActiveUser(Long userId) {
		return userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
			.orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
	}
}
