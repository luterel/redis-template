package ru.luterel.template.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RedisPostCacheService implements PostCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.post.redis-ttl}")
    private Duration postTtl;

    @Override
    public Optional<PostResponse> getById(Long id) {
        String key = buildKey(id);
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json == null) {
            return Optional.empty();
        }

        try {
            PostResponse post = objectMapper.readValue(json, PostResponse.class);
            return Optional.of(post);
        } catch (JacksonException e) {
            log.warn("Failed to deserialize cache value for key {}", key, e);
            stringRedisTemplate.delete(key);
            return Optional.empty();
        }
    }

    @Override
    public void put(PostResponse post) {
        String key = buildKey(post.id());
        try {
            String json = objectMapper.writeValueAsString(post);
            stringRedisTemplate.opsForValue().set(key, json, postTtl);
        } catch (JacksonException e) {
            log.warn("Failed to serialize cache value for key {}", key, e);
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
