package flipnote.user.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class SessionInvalidationRepository {

    private static final String KEY_PREFIX = "session:invalidated:";

    private final StringRedisTemplate redisTemplate;

    public void invalidate(Long userId, long ttlMillis) {
        if (ttlMillis <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                KEY_PREFIX + userId,
                String.valueOf(System.currentTimeMillis()),
                ttlMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public Optional<Long> getInvalidatedAtMillis(Long userId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return Optional.ofNullable(value).map(Long::parseLong);
    }
}
