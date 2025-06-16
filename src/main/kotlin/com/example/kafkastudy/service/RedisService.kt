package com.example.kafkastudy.service

import com.example.kafkastudy.dto.UserResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(RedisService::class.java)
    
    companion object {
        private const val USER_CACHE_PREFIX = "user:"
        private const val USER_EMAIL_PREFIX = "user:email:"
        private val DEFAULT_EXPIRATION = Duration.ofMinutes(30)
    }
    
    fun cacheUser(user: UserResponse) {
        try {
            val userJson = objectMapper.writeValueAsString(user)
            val userKey = "$USER_CACHE_PREFIX${user.id}"
            val emailKey = "$USER_EMAIL_PREFIX${user.email}"
            
            redisTemplate.opsForValue().set(userKey, userJson, DEFAULT_EXPIRATION)
            redisTemplate.opsForValue().set(emailKey, user.id.toString(), DEFAULT_EXPIRATION)
            
            logger.info("Cached user with id {} and email {}", user.id, user.email)
        } catch (ex: Exception) {
            logger.error("Error caching user with id {}: {}", user.id, ex.message, ex)
        }
    }
    
    fun getUserById(id: Long): UserResponse? {
        return try {
            val userKey = "$USER_CACHE_PREFIX$id"
            val userJson = redisTemplate.opsForValue().get(userKey)
            
            if (userJson != null) {
                val user = objectMapper.readValue(userJson, UserResponse::class.java)
                logger.info("Retrieved user from cache with id: {}", id)
                user
            } else {
                logger.info("User with id {} not found in cache", id)
                null
            }
        } catch (ex: Exception) {
            logger.error("Error retrieving user from cache with id {}: {}", id, ex.message, ex)
            null
        }
    }
    
    fun getUserByEmail(email: String): UserResponse? {
        return try {
            val emailKey = "$USER_EMAIL_PREFIX$email"
            val userIdStr = redisTemplate.opsForValue().get(emailKey)
            
            if (userIdStr != null) {
                val userId = userIdStr.toLong()
                getUserById(userId)
            } else {
                logger.info("User with email {} not found in cache", email)
                null
            }
        } catch (ex: Exception) {
            logger.error("Error retrieving user from cache with email {}: {}", email, ex.message, ex)
            null
        }
    }
    
    fun evictUser(id: Long) {
        try {
            val userKey = "$USER_CACHE_PREFIX$id"
            redisTemplate.delete(userKey)
            logger.info("Evicted user from cache with id: {}", id)
        } catch (ex: Exception) {
            logger.error("Error evicting user from cache with id {}: {}", id, ex.message, ex)
        }
    }
    
    fun evictUserByEmail(email: String) {
        try {
            val emailKey = "$USER_EMAIL_PREFIX$email"
            redisTemplate.delete(emailKey)
            logger.info("Evicted user email from cache: {}", email)
        } catch (ex: Exception) {
            logger.error("Error evicting user email from cache {}: {}", email, ex.message, ex)
        }
    }
    
    fun setValue(key: String, value: String, expiration: Duration = DEFAULT_EXPIRATION) {
        try {
            redisTemplate.opsForValue().set(key, value, expiration)
            logger.debug("Set value in Redis: key={}", key)
        } catch (ex: Exception) {
            logger.error("Error setting value in Redis: key={}, error={}", key, ex.message, ex)
        }
    }
    
    fun getValue(key: String): String? {
        return try {
            val value = redisTemplate.opsForValue().get(key)
            logger.debug("Retrieved value from Redis: key={}, found={}", key, value != null)
            value
        } catch (ex: Exception) {
            logger.error("Error getting value from Redis: key={}, error={}", key, ex.message, ex)
            null
        }
    }
    
    fun deleteKey(key: String): Boolean {
        return try {
            val deleted = redisTemplate.delete(key)
            logger.debug("Deleted key from Redis: key={}, deleted={}", key, deleted)
            deleted
        } catch (ex: Exception) {
            logger.error("Error deleting key from Redis: key={}, error={}", key, ex.message, ex)
            false
        }
    }
    
    fun hasKey(key: String): Boolean {
        return try {
            val exists = redisTemplate.hasKey(key)
            logger.debug("Checked key existence in Redis: key={}, exists={}", key, exists)
            exists
        } catch (ex: Exception) {
            logger.error("Error checking key existence in Redis: key={}, error={}", key, ex.message, ex)
            false
        }
    }
} 