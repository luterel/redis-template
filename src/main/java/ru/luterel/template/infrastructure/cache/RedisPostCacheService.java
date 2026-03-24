package ru.luterel.template.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.luterel.template.api.dto.PostResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisPostCacheService implements PostCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.post.redis-ttl}")
    private Duration postTtl;

    @Override
    public Optional<PostResponse> getById(Long id) {
        String json = stringRedisTemplate.opsForValue().get(buildKey(id));

        if (json == null) {
            return Optional.empty();
        }

        try {
            PostResponse post = objectMapper.readValue(json, PostResponse.class);
            return Optional.of(post);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to read post from redis cache", e);
        }
    }

    @Override
    public void put(PostResponse post) {
        try {
            String json = objectMapper.writeValueAsString(post);
            stringRedisTemplate.opsForValue().set(buildKey(post.id()), json, postTtl);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to write post to redis cache", e);
        }
    }

    @Override
    public void evictById(Long id) {
        stringRedisTemplate.delete(buildKey(id));
    }

    private String buildKey(Long id) {
        return "post:id:" + id;
    }
}
