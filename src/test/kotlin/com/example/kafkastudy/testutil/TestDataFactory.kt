package com.example.kafkastudy.testutil

import com.example.kafkastudy.dto.KafkaMessage
import com.example.kafkastudy.dto.UserCreateRequest
import com.example.kafkastudy.dto.UserResponse
import com.example.kafkastudy.entity.User
import java.time.LocalDateTime
import java.util.*

object TestDataFactory {
    
    // Standard test users for consistent data across all tests
    object TestUsers {
        val USER_1 = TestUserData(
            id = 100L,
            email = "testuser1@example.com",
            name = "Test User One",
            age = 25,
            createdAt = LocalDateTime.of(2023, 1, 1, 10, 0, 0)
        )
        
        val USER_2 = TestUserData(
            id = 200L,
            email = "testuser2@example.com", 
            name = "Test User Two",
            age = 30,
            createdAt = LocalDateTime.of(2023, 1, 2, 10, 0, 0)
        )
        
        val USER_3 = TestUserData(
            id = 300L,
            email = "testuser3@example.com",
            name = "Test User Three", 
            age = 35,
            createdAt = LocalDateTime.of(2023, 1, 3, 10, 0, 0)
        )
    }
    
    data class TestUserData(
        val id: Long,
        val email: String,
        val name: String,
        val age: Int,
        val createdAt: LocalDateTime
    ) {
        fun toCreateRequest() = UserCreateRequest(
            email = email,
            name = name,
            age = age
        )
        
        fun toEntity() = User(
            id = id,
            email = email,
            name = name,
            age = age,
            createdAt = createdAt,
            updatedAt = null
        )
        
        fun toResponse(updatedAt: LocalDateTime? = null) = UserResponse(
            id = id,
            email = email,
            name = name,
            age = age,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
        
        fun toKafkaMessage(action: String = "USER_CREATED") = KafkaMessage(
            id = "user-event-$id",
            message = "$action: User $name ($email)",
            timestamp = createdAt
        )
    }
    
    // Generate unique test data for each test run
    fun generateUniqueUser(suffix: String = UUID.randomUUID().toString().take(8)): TestUserData {
        return TestUserData(
            id = Random().nextLong(1000, 9999),
            email = "test-$suffix@example.com",
            name = "Test User $suffix",
            age = Random().nextInt(18, 65),
            createdAt = LocalDateTime.now()
        )
    }
    
    // Kafka message templates
    object KafkaMessages {
        fun userCreated(user: TestUserData) = KafkaMessage(
            id = "user-created-${user.id}",
            message = "User created: ${user.name} (${user.email})",
            timestamp = user.createdAt
        )
        
        fun userUpdated(user: TestUserData) = KafkaMessage(
            id = "user-updated-${user.id}",
            message = "User updated: ${user.name} (${user.email})",
            timestamp = LocalDateTime.now()
        )
        
        fun userDeleted(user: TestUserData) = KafkaMessage(
            id = "user-deleted-${user.id}",
            message = "User deleted: ${user.name} (${user.email})",
            timestamp = LocalDateTime.now()
        )
        
        fun notification(message: String) = "Test notification: $message"
        
        fun testMessage(id: String = UUID.randomUUID().toString()) = KafkaMessage(
            id = id,
            message = "Test message $id",
            timestamp = LocalDateTime.now()
        )
    }
} 