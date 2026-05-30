package com.errortracker.queue;

import com.errortracker.dto.kafka.ErrorEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageQueue {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String QUEUE_KEY = "error:queue";

    // In-memory fallback if Redis is down
    private final LinkedBlockingQueue<ErrorEventMessage> fallbackQueue
            = new LinkedBlockingQueue<>(1000);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private boolean redisAvailable = true;

    public void push(ErrorEventMessage message) {
        if (!redisAvailable) {
            // Try to reconnect
            redisAvailable = checkRedis();
        }

        if (redisAvailable) {
            try {
                String json = objectMapper.writeValueAsString(message);
                redisTemplate.opsForList().rightPush(QUEUE_KEY, json);
                log.debug("Error pushed to Redis queue");
                return;
            } catch (Exception e) {
                log.warn("Redis push failed — using in-memory fallback: {}",
                        e.getMessage());
                redisAvailable = false;
            }
        }

        // Fallback to in-memory queue
        boolean added = fallbackQueue.offer(message);
        if (!added) {
            log.error("In-memory fallback queue is full — dropping error");
        } else {
            log.debug("Error pushed to in-memory fallback queue");
        }
    }

    public ErrorEventMessage pop() {
        // First drain any fallback queue items
        ErrorEventMessage fallback = fallbackQueue.poll();
        if (fallback != null) return fallback;

        // Then try Redis
        try {
            String json = redisTemplate.opsForList()
                    .leftPop(QUEUE_KEY);
            if (json == null) return null;
            redisAvailable = true;
            return objectMapper.readValue(json, ErrorEventMessage.class);
        } catch (Exception e) {
            if (redisAvailable) {
                log.warn("Redis pop failed: {}", e.getMessage());
                redisAvailable = false;
            }
            return null;
        }
    }

    public Long size() {
        try {
            Long redisSize = redisTemplate.opsForList().size(QUEUE_KEY);
            long fallbackSize = fallbackQueue.size();
            return (redisSize != null ? redisSize : 0) + fallbackSize;
        } catch (Exception e) {
            return (long) fallbackQueue.size();
        }
    }

    private boolean checkRedis() {
        try {
            redisTemplate.opsForValue().get("health:check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}