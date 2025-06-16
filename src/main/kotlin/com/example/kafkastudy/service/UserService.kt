package com.example.kafkastudy.service

import com.example.kafkastudy.dto.KafkaMessage
import com.example.kafkastudy.dto.UserCreateRequest
import com.example.kafkastudy.dto.UserResponse
import com.example.kafkastudy.entity.User
import com.example.kafkastudy.kafka.KafkaProducer
import com.example.kafkastudy.mapper.UserMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class UserService(
    private val userMapper: UserMapper,
    private val redisService: RedisService,
    private val kafkaProducer: KafkaProducer
) {
    
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    fun createUser(request: UserCreateRequest): UserResponse {
        logger.info("Creating user with email: {}", request.email)
        
        // Check if user already exists
        val existingUser = userMapper.findByEmail(request.email)
        if (existingUser != null) {
            throw IllegalArgumentException("User with email ${request.email} already exists")
        }
        
        // Create new user
        val user = User(
            email = request.email,
            name = request.name,
            age = request.age,
            createdAt = LocalDateTime.now()
        )
        
        userMapper.insert(user)
        
        val userResponse = user.toUserResponse()
        
        // Cache the user in Redis
        redisService.cacheUser(userResponse)
        
        // Send Kafka event
        val kafkaMessage = KafkaMessage(
            id = UUID.randomUUID().toString(),
            message = "User created: ${user.email}"
        )
        kafkaProducer.sendUserEvent(kafkaMessage)
        
        logger.info("Successfully created user with id: {}", user.id)
        return userResponse
    }
    
    fun getUserById(id: Long): UserResponse? {
        logger.info("Getting user by id: {}", id)
        
        // First, try to get from Redis cache
        val cachedUser = redisService.getUserById(id)
        if (cachedUser != null) {
            logger.info("User found in cache with id: {}", id)
            return cachedUser
        }
        
        // If not in cache, get from database
        logger.info("User not found in cache, querying database for id: {}", id)
        val user = userMapper.findById(id)
        
        return if (user != null) {
            val userResponse = user.toUserResponse()
            // Cache the user for future requests
            redisService.cacheUser(userResponse)
            logger.info("User found in database and cached with id: {}", id)
            userResponse
        } else {
            logger.info("User not found with id: {}", id)
            null
        }
    }
    
    fun getUserByEmail(email: String): UserResponse? {
        logger.info("Getting user by email: {}", email)
        
        // First, try to get from Redis cache
        val cachedUser = redisService.getUserByEmail(email)
        if (cachedUser != null) {
            logger.info("User found in cache with email: {}", email)
            return cachedUser
        }
        
        // If not in cache, get from database
        logger.info("User not found in cache, querying database for email: {}", email)
        val user = userMapper.findByEmail(email)
        
        return if (user != null) {
            val userResponse = user.toUserResponse()
            // Cache the user for future requests
            redisService.cacheUser(userResponse)
            logger.info("User found in database and cached with email: {}", email)
            userResponse
        } else {
            logger.info("User not found with email: {}", email)
            null
        }
    }
    
    fun getAllUsers(): List<UserResponse> {
        logger.info("Getting all users")
        val users = userMapper.findAll()
        return users.map { it.toUserResponse() }
    }
    
    fun updateUser(id: Long, request: UserCreateRequest): UserResponse? {
        logger.info("Updating user with id: {}", id)
        
        val existingUser = userMapper.findById(id)
            ?: throw IllegalArgumentException("User with id $id not found")
        
        val updatedUser = existingUser.copy(
            name = request.name,
            age = request.age,
            updatedAt = LocalDateTime.now()
        )
        
        userMapper.update(updatedUser)
        
        val userResponse = updatedUser.toUserResponse()
        
        // Update cache
        redisService.cacheUser(userResponse)
        
        // Send Kafka event
        val kafkaMessage = KafkaMessage(
            id = UUID.randomUUID().toString(),
            message = "User updated: ${updatedUser.email}"
        )
        kafkaProducer.sendUserEvent(kafkaMessage)
        
        logger.info("Successfully updated user with id: {}", id)
        return userResponse
    }
    
    fun deleteUser(id: Long): Boolean {
        logger.info("Deleting user with id: {}", id)
        
        val user = userMapper.findById(id)
            ?: throw IllegalArgumentException("User with id $id not found")
        
        val deleted = userMapper.deleteById(id) > 0
        
        if (deleted) {
            // Remove from cache
            redisService.evictUser(id)
            redisService.evictUserByEmail(user.email)
            
            // Send Kafka event
            val kafkaMessage = KafkaMessage(
                id = UUID.randomUUID().toString(),
                message = "User deleted: ${user.email}"
            )
            kafkaProducer.sendUserEvent(kafkaMessage)
            
            logger.info("Successfully deleted user with id: {}", id)
        }
        
        return deleted
    }
    
    // Extension function to convert User entity to UserResponse DTO
    private fun User.toUserResponse(): UserResponse {
        return UserResponse(
            id = this.id!!,
            email = this.email,
            name = this.name,
            age = this.age,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
} 