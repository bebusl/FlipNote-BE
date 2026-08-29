package flipnote.user.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class SocialLinkTokenRepository {

    private static final String KEY_PREFIX = "oauth:link:state:";
    private static final long TTL_MINUTES = 3;

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String state) {
        redisTemplate.opsForValue().set(KEY_PREFIX + state, userId.toString(), TTL_MINUTES, TimeUnit.MINUTES);
    }

    public Optional<Long> findUserIdByState(String state) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + state);
        return Optional.ofNullable(value).map(Long::parseLong);
    }

    public void delete(String state) {
        redisTemplate.delete(KEY_PREFIX + state);
    }
}
