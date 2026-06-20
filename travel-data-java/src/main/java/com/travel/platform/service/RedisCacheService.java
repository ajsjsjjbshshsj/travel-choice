package com.travel.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, typeReference));
        } catch (Exception e) {
            log.warn("Redis read failed for key={}", key, e);
            return Optional.empty();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("Redis write failed for key={}", key, e);
        }
    }

    public void evictByPrefix(String prefix) {
        try {
            var keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Redis evicted {} keys with prefix={}", keys.size(), prefix);
            }
        } catch (Exception e) {
            log.warn("Redis evict failed for prefix={}", prefix, e);
        }
    }

    public boolean isAvailable() {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping());
        } catch (Exception e) {
            return false;
        }
    }
}
