package flipnote.user.infrastructure.redis;

import flipnote.user.domain.VerificationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class EmailVerificationRepository {

    private static final String CODE_KEY_PREFIX = "email:verification:code:";
    private static final String VERIFIED_KEY_PREFIX = "email:verification:verified:";

    private final StringRedisTemplate redisTemplate;

    public void saveCode(String email, String code) {
        redisTemplate.opsForValue().set(
                CODE_KEY_PREFIX + email,
                code,
                VerificationConstants.CODE_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public String getCode(String email) {
        return redisTemplate.opsForValue().get(CODE_KEY_PREFIX + email);
    }

    public boolean hasCode(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CODE_KEY_PREFIX + email));
    }

    public void deleteCode(String email) {
        redisTemplate.delete(CODE_KEY_PREFIX + email);
    }

    public void markVerified(String email) {
        redisTemplate.opsForValue().set(
                VERIFIED_KEY_PREFIX + email,
                "verified",
                VerificationConstants.VERIFIED_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(VERIFIED_KEY_PREFIX + email));
    }

    public void deleteVerified(String email) {
        redisTemplate.delete(VERIFIED_KEY_PREFIX + email);
    }
}
