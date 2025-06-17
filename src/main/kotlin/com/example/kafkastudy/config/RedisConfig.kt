package com.example.kafkastudy.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.SimpleType
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {
    
    @Value("\${spring.data.redis.host}")
    private lateinit var redisHost: String
    
    @Value("\${spring.data.redis.port}")
    private var redisPort: Int = 6379
    
    @Bean
    fun lettuceConnectionFactory(): LettuceConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = redisHost
        redisStandaloneConfiguration.port = redisPort
        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }
    
    @Bean
    @Primary
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = connectionFactory
        
        // Use String serialization for keys and values
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        
        template.afterPropertiesSet()
        return template
    }
    
    @Bean
    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(KotlinModule.Builder().build())
        mapper.findAndRegisterModules()
        return mapper
    }
    
    @Bean
    fun jsonRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        
        // Use String serialization for keys and JSON for values
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()
        
        template.afterPropertiesSet()
        return template
    }
} 