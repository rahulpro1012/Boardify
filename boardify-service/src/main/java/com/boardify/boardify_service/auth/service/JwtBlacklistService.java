package com.boardify.boardify_service.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class JwtBlacklistService {
    private static final Logger logger = LoggerFactory.getLogger(JwtBlacklistService.class);
    private static final String PREFIX = "blacklisted_token:";
    
    private final RedisTemplate<String, String> redisTemplate;

    public JwtBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, long expirationMillis) {
        try {
            long ttl = expirationMillis - System.currentTimeMillis();
            if (ttl > 0) {
                String key = PREFIX + token;
                redisTemplate.opsForValue().set(
                    key, 
                    "true", 
                    Duration.ofMillis(ttl)
                );
                logger.info("Token blacklisted successfully. Key: {}, TTL: {}ms", key, ttl);
                
                // Verify the key was set
                Boolean exists = redisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(exists)) {
                    logger.debug("Successfully verified blacklisted token in Redis");
                } else {
                    logger.error("Failed to verify blacklisted token in Redis");
                }
            } else {
                logger.warn("Token already expired, not blacklisting");
            }
        } catch (Exception e) {
            logger.error("Failed to blacklist token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            Boolean exists = redisTemplate.hasKey(PREFIX + token);
            if (exists == null) {
                logger.warn("Redis returned null for key existence check");
                return false;
            }
            return exists;
        } catch (Exception e) {
            logger.error("Failed to check token blacklist: {}", e.getMessage());
            // If Redis is down, assume token is not blacklisted to avoid locking users out
            // This is a security trade-off - adjust based on your requirements
            return false;
        }
    }
}
