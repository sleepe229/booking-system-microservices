package com.hotel.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String PREFIX = "idempotency:booking:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String bookingId) {
        String key = PREFIX + bookingId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Idempotency acquired: {}", bookingId);
            return true;
        } else {
            log.warn("Duplicate detected: {}", bookingId);
            return false;
        }
    }

    public void release(String bookingId) {
        String key = PREFIX + bookingId;
        redisTemplate.delete(key);
        log.debug("Idempotency released: {}", bookingId);
    }
}
