package com.example.kafkastudy.service

import com.example.kafkastudy.testutil.TestDataFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
    locations = ["classpath:application-test.yml"],
    properties = [
        "spring.data.redis.timeout=500ms",
        "spring.data.redis.lettuce.pool.max-active=2",
        "spring.data.redis.lettuce.pool.max-idle=2"
    ]
)
class RedisServiceTest {

    @Autowired
    private lateinit var redisService: RedisService

    private val testUser1 = TestDataFactory.TestUsers.USER_1
    private val testUser2 = TestDataFactory.TestUsers.USER_2

    @BeforeEach
    fun setUp() {
        // Only clear test-specific data, not all Redis data
        try {
            redisService.evictUser(testUser1.id)
            redisService.evictUser(testUser2.id)
            redisService.evictUserByEmail(testUser1.email)
            redisService.evictUserByEmail(testUser2.email)
            
            // Clear test keys efficiently
            listOf("test:key", "test:key:expiration", "test:key:delete", "test:key:exists").forEach {
                redisService.deleteKey(it)
            }
        } catch (e: Exception) {
            // Ignore cleanup errors for speed
        }
    }

    @Test
    fun `should cache and retrieve user by id`() {
        // Given - Use shared test data
        val userResponse = testUser1.toResponse()

        // When
        redisService.cacheUser(userResponse)

        // Then
        val retrievedUser = redisService.getUserById(testUser1.id)
        assertNotNull(retrievedUser)
        assertEquals(testUser1.id, retrievedUser!!.id)
        assertEquals(testUser1.email, retrievedUser.email)
        assertEquals(testUser1.name, retrievedUser.name)
        assertEquals(testUser1.age, retrievedUser.age)
    }

    @Test
    fun `should cache and retrieve user by email`() {
        // Given - Use shared test data
        val userResponse = testUser1.toResponse()

        // When
        redisService.cacheUser(userResponse)

        // Then
        val retrievedUser = redisService.getUserByEmail(testUser1.email)
        assertNotNull(retrievedUser)
        assertEquals(testUser1.id, retrievedUser!!.id)
        assertEquals(testUser1.email, retrievedUser.email)
        assertEquals(testUser1.name, retrievedUser.name)
        assertEquals(testUser1.age, retrievedUser.age)
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
        // Given - Use shared test data
        val userResponse = testUser1.toResponse()
        redisService.cacheUser(userResponse)
        assertNotNull(redisService.getUserById(testUser1.id))

        // When
        redisService.evictUser(testUser1.id)

        // Then
        assertNull(redisService.getUserById(testUser1.id))
    }

    @Test
    fun `should evict user email from cache`() {
        // Given - Use shared test data
        val userResponse = testUser1.toResponse()
        redisService.cacheUser(userResponse)
        assertNotNull(redisService.getUserByEmail(testUser1.email))

        // When
        redisService.evictUserByEmail(testUser1.email)

        // Then
        // The user by ID should still be available, but email lookup should fail
        assertNotNull(redisService.getUserById(testUser1.id))
        assertNull(redisService.getUserByEmail(testUser1.email))
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
        val expiration = Duration.ofMillis(500) // Shorter expiration for speed

        // When
        redisService.setValue(key, value, expiration)

        // Then
        val retrievedValue = redisService.getValue(key)
        assertEquals(value, retrievedValue)

        // Wait for expiration
        Thread.sleep(600)

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
    fun `should handle multiple user cache operations with consistent data`() {
        // Given - Use shared test data
        val user1Response = testUser1.toResponse()
        val user2Response = testUser2.toResponse()

        // When
        redisService.cacheUser(user1Response)
        redisService.cacheUser(user2Response)

        // Then
        val retrievedUser1 = redisService.getUserById(testUser1.id)
        val retrievedUser2 = redisService.getUserById(testUser2.id)

        assertNotNull(retrievedUser1)
        assertNotNull(retrievedUser2)
        assertEquals(testUser1.id, retrievedUser1!!.id)
        assertEquals(testUser2.id, retrievedUser2!!.id)

        // Test email lookups
        val retrievedByEmail1 = redisService.getUserByEmail(testUser1.email)
        val retrievedByEmail2 = redisService.getUserByEmail(testUser2.email)

        assertNotNull(retrievedByEmail1)
        assertNotNull(retrievedByEmail2)
        assertEquals(testUser1.id, retrievedByEmail1!!.id)
        assertEquals(testUser2.id, retrievedByEmail2!!.id)
    }
} 