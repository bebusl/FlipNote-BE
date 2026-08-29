package flipnote.user.infrastructure.redis;

import flipnote.user.domain.PasswordResetConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class PasswordResetRepository {

    private static final String TOKEN_KEY_PREFIX = "password:reset:token:";
    private static final String EMAIL_KEY_PREFIX = "password:reset:email:";

    private final StringRedisTemplate redisTemplate;

    public boolean hasToken(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(EMAIL_KEY_PREFIX + email));
    }

    public void save(String token, String email) {
        redisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + token,
                email,
                PasswordResetConstants.TOKEN_TTL_MINUTES,
                TimeUnit.MINUTES
        );
        redisTemplate.opsForValue().set(
                EMAIL_KEY_PREFIX + email,
                token,
                PasswordResetConstants.TOKEN_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public String findEmailByToken(String token) {
        return redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
    }

    public void delete(String token, String email) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        redisTemplate.delete(EMAIL_KEY_PREFIX + email);
    }
}
