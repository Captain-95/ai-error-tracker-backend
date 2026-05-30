package com.errortracker.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    public boolean isAllowed(String key, int limit, long windowSeconds) {
        // If rate limiting disabled — always allow
        if (!rateLimitEnabled) return true;

        String redisKey = "rate_limit:" + key;

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == null) return true; // Redis error — fail open

            if (count == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
            }

            if (count > limit) {
                log.warn("Rate limit exceeded for key: {}", redisKey);
                return false;
            }

            return true;

        } catch (Exception e) {
            // Redis is down — fail open (allow request)
            // Never block users because Redis is unavailable
            log.warn("Redis unavailable for rate limiting — failing open: {}",
                    e.getMessage());
            return true;
        }
    }

    public long getRemainingRequests(String key, int limit) {
        String redisKey = "rate_limit:" + key;
        try {
            String val = redisTemplate.opsForValue().get(redisKey);
            long used = val != null ? Long.parseLong(val) : 0;
            return Math.max(0, limit - used);
        } catch (Exception e) {
            return limit;
        }
    }
}