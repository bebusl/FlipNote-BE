package flipnote.notification.domain.fcmtoken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

	List<FcmToken> findByUserId(Long userId);

	Optional<FcmToken> findByToken(String token);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM FcmToken f WHERE f.userId = :userId AND f.token IN :tokens")
	void deleteByUserIdAndTokenIn(@Param("userId") Long userId, @Param("tokens") List<String> tokens);

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE FcmToken f SET f.lastUsedAt = :now WHERE f.token IN :tokens")
	int bulkUpdateLastUsedAt(@Param("tokens") List<String> tokens, @Param("now") LocalDateTime now);
}
