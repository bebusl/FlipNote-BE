package flipnote.notification.application;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.notification.domain.fcmtoken.FcmToken;
import flipnote.notification.domain.fcmtoken.FcmTokenRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class FcmTokenService {

	private final FcmTokenRepository fcmTokenRepository;

	@Transactional
	public void registerFcmToken(Long userId, String token) {
		Optional<FcmToken> existingToken = fcmTokenRepository.findByToken(token);

		if (existingToken.isPresent()) {
			FcmToken fcmToken = existingToken.get();

			if (Objects.equals(fcmToken.getUserId(), userId)) {
				fcmToken.updateLastUsedAt();
			} else {
				fcmTokenRepository.deleteById(fcmToken.getId());
				saveFcmToken(userId, token);
			}
		} else {
			saveFcmToken(userId, token);
		}
	}

	private void saveFcmToken(Long userId, String token) {
		FcmToken fcmToken = FcmToken.builder()
			.userId(userId)
			.token(token)
			.build();

		fcmTokenRepository.save(fcmToken);
	}
}
