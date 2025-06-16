package com.example.kafkastudy.controller

import com.example.kafkastudy.dto.KafkaMessage
import com.example.kafkastudy.dto.UserCreateRequest
import com.example.kafkastudy.dto.UserResponse
import com.example.kafkastudy.kafka.KafkaProducer
import com.example.kafkastudy.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val kafkaProducer: KafkaProducer
) {
    
    private val logger = LoggerFactory.getLogger(UserController::class.java)
    
    @PostMapping
    fun createUser(@RequestBody request: UserCreateRequest): ResponseEntity<UserResponse> {
        return try {
            logger.info("API: Creating user with email: {}", request.email)
            val user = userService.createUser(request)
            ResponseEntity.status(HttpStatus.CREATED).body(user)
        } catch (ex: IllegalArgumentException) {
            logger.warn("API: Invalid request for user creation: {}", ex.message)
            ResponseEntity.badRequest().build()
        } catch (ex: Exception) {
            logger.error("API: Error creating user: {}", ex.message, ex)
            ResponseEntity.internalServerError().build()
        }
    }
    
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        return try {
            logger.info("API: Getting user by id: {}", id)
            val user = userService.getUserById(id)
            if (user != null) {
                ResponseEntity.ok(user)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (ex: Exception) {
            logger.error("API: Error getting user by id {}: {}", id, ex.message, ex)
            ResponseEntity.internalServerError().build()
        }
    }
    
    @GetMapping("/email/{email}")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<UserResponse> {
        return try {
            logger.info("API: Getting user by email: {}", email)
            val user = userService.getUserByEmail(email)
            if (user != null) {
                ResponseEntity.ok(user)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (ex: Exception) {
            logger.error("API: Error getting user by email {}: {}", email, ex.message, ex)
            ResponseEntity.internalServerError().build()
        }
    }
    
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserResponse>> {
        return try {
            logger.info("API: Getting all users")
            val users = userService.getAllUsers()
            ResponseEntity.ok(users)
        } catch (ex: Exception) {
            logger.error("API: Error getting all users: {}", ex.message, ex)
            ResponseEntity.internalServerError().build()
        }
    }
    
    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody request: UserCreateRequest
    ): ResponseEntity<UserResponse> {
        return try {
            logger.info("API: Updating user with id: {}", id)
            val user = userService.updateUser(id, request)
            if (user != null) {
                ResponseEntity.ok(user)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (ex: IllegalArgumentException) {
            logger.warn("API: Invalid request for user update: {}", ex.message)
            ResponseEntity.badRequest().build()
        } catch (ex: Exception) {
            logger.error("API: Error updating user with id {}: {}", id, ex.message, ex)
            ResponseEntity.internalServerError().build()
        }
    }
    
    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            logger.info("API: Deleting user with id: {}", id)
            val deleted = userService.deleteUser(id)
            if (deleted) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (ex: IllegalArgumentException) {
            logger.warn("API: Invalid request for user deletion: {}", ex.message)
            ResponseEntity.badRequest().build()
        } catch (ex: Exception) {
            logger.error("API: Error deleting user with id {}: {}", id, ex.message, ex)
            ResponseEntity.internalServerError().build()
        }
    }
} 