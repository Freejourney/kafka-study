package com.example.kafkastudy.service

import com.example.kafkastudy.dto.UserResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.LocalDateTime

@SpringBootTest
@TestPropertySource(properties = [
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver"
])
class RedisServiceTest {

    @Autowired
    private lateinit var redisService: RedisService

    private lateinit var testUser: UserResponse

    @BeforeEach
    fun setUp() {
        testUser = UserResponse(
            id = 1L,
            email = "test@example.com",
            name = "Test User",
            age = 30,
            createdAt = LocalDateTime.now(),
            updatedAt = null
        )
        
        // Clear any existing data
        redisService.evictUser(testUser.id)
        redisService.evictUserByEmail(testUser.email)
    }

    @Test
    fun `should cache and retrieve user by id`() {
        // When
        redisService.cacheUser(testUser)

        // Then
        val retrievedUser = redisService.getUserById(testUser.id)
        assertNotNull(retrievedUser)
        assertEquals(testUser.id, retrievedUser!!.id)
        assertEquals(testUser.email, retrievedUser.email)
        assertEquals(testUser.name, retrievedUser.name)
        assertEquals(testUser.age, retrievedUser.age)
    }

    @Test
    fun `should cache and retrieve user by email`() {
        // When
        redisService.cacheUser(testUser)

        // Then
        val retrievedUser = redisService.getUserByEmail(testUser.email)
        assertNotNull(retrievedUser)
        assertEquals(testUser.id, retrievedUser!!.id)
        assertEquals(testUser.email, retrievedUser.email)
        assertEquals(testUser.name, retrievedUser.name)
        assertEquals(testUser.age, retrievedUser.age)
    }

    @Test
    fun `should return null when user not found in cache`() {
        // When
        val retrievedUser = redisService.getUserById(999L)

        // Then
        assertNull(retrievedUser)
    }

    @Test
    fun `should return null when user email not found in cache`() {
        // When
        val retrievedUser = redisService.getUserByEmail("nonexistent@example.com")

        // Then
        assertNull(retrievedUser)
    }

    @Test
    fun `should evict user from cache`() {
        // Given
        redisService.cacheUser(testUser)
        assertNotNull(redisService.getUserById(testUser.id))

        // When
        redisService.evictUser(testUser.id)

        // Then
        assertNull(redisService.getUserById(testUser.id))
    }

    @Test
    fun `should evict user email from cache`() {
        // Given
        redisService.cacheUser(testUser)
        assertNotNull(redisService.getUserByEmail(testUser.email))

        // When
        redisService.evictUserByEmail(testUser.email)

        // Then
        // The user by ID should still be available, but email lookup should fail
        assertNotNull(redisService.getUserById(testUser.id))
        assertNull(redisService.getUserByEmail(testUser.email))
    }

    @Test
    fun `should set and get string values`() {
        // Given
        val key = "test:key"
        val value = "test value"

        // When
        redisService.setValue(key, value)

        // Then
        val retrievedValue = redisService.getValue(key)
        assertEquals(value, retrievedValue)
    }

    @Test
    fun `should set value with custom expiration`() {
        // Given
        val key = "test:key:expiration"
        val value = "test value with expiration"
        val expiration = Duration.ofSeconds(1)

        // When
        redisService.setValue(key, value, expiration)

        // Then
        val retrievedValue = redisService.getValue(key)
        assertEquals(value, retrievedValue)

        // Wait for expiration
        Thread.sleep(1100)

        // Value should be expired
        assertNull(redisService.getValue(key))
    }

    @Test
    fun `should delete key correctly`() {
        // Given
        val key = "test:key:delete"
        val value = "test value to delete"
        redisService.setValue(key, value)

        // When
        val deleted = redisService.deleteKey(key)

        // Then
        assertTrue(deleted)
        assertNull(redisService.getValue(key))
    }

    @Test
    fun `should check key existence correctly`() {
        // Given
        val key = "test:key:exists"
        val value = "test value"

        // When key doesn't exist
        assertFalse(redisService.hasKey(key))

        // When key exists
        redisService.setValue(key, value)
        assertTrue(redisService.hasKey(key))

        // When key is deleted
        redisService.deleteKey(key)
        assertFalse(redisService.hasKey(key))
    }

    @Test
    fun `should handle multiple user cache operations`() {
        // Given
        val user1 = testUser
        val user2 = testUser.copy(id = 2L, email = "test2@example.com", name = "Test User 2")

        // When
        redisService.cacheUser(user1)
        redisService.cacheUser(user2)

        // Then
        val retrievedUser1 = redisService.getUserById(user1.id)
        val retrievedUser2 = redisService.getUserById(user2.id)

        assertNotNull(retrievedUser1)
        assertNotNull(retrievedUser2)
        assertEquals(user1.id, retrievedUser1!!.id)
        assertEquals(user2.id, retrievedUser2!!.id)

        // Test email lookups
        val retrievedByEmail1 = redisService.getUserByEmail(user1.email)
        val retrievedByEmail2 = redisService.getUserByEmail(user2.email)

        assertNotNull(retrievedByEmail1)
        assertNotNull(retrievedByEmail2)
        assertEquals(user1.id, retrievedByEmail1!!.id)
        assertEquals(user2.id, retrievedByEmail2!!.id)
    }
} 