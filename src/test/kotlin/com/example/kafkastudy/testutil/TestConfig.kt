package com.example.kafkastudy.testutil

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@TestConfiguration
@Profile("test")
class TestRedisConfig {
    
    @Bean("testRedisConnectionFactory")
    @Primary
    fun testRedisConnectionFactory(): RedisConnectionFactory {
        // Use in-memory Redis mock for faster tests
        val factory = JedisConnectionFactory()
        factory.hostName = "localhost"
        factory.port = 6370
        factory.timeout = 1000
        factory.usePool = false // Disable pooling for test speed
        return factory
    }
    
    @Bean("testRedisTemplate")
    @Primary
    fun testRedisTemplate(): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = testRedisConnectionFactory()
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.valueSerializer = GenericToStringSerializer(Any::class.java)
        template.hashValueSerializer = GenericToStringSerializer(Any::class.java)
        template.afterPropertiesSet()
        return template
    }
} 