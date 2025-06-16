package com.example.kafkastudy.service

import com.example.kafkastudy.dto.UserCreateRequest
import com.example.kafkastudy.entity.User
import com.example.kafkastudy.mapper.UserMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
])
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userMapper: UserMapper

    @Autowired
    private lateinit var redisService: RedisService

    private lateinit var testUserRequest: UserCreateRequest

    @BeforeEach
    fun setUp() {
        testUserRequest = UserCreateRequest(
            email = "integration@test.com",
            name = "Integration Test User",
            age = 25
        )

        // Clean up any existing data
        try {
            val existingUser = userMapper.findByEmail(testUserRequest.email)
            existingUser?.let { 
                redisService.evictUser(it.id!!)
                redisService.evictUserByEmail(it.email)
                userMapper.deleteById(it.id!!)
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun `should create user and cache it`() {
        // When
        val createdUser = userService.createUser(testUserRequest)

        // Then
        assertNotNull(createdUser)
        assertEquals(testUserRequest.email, createdUser.email)
        assertEquals(testUserRequest.name, createdUser.name)
        assertEquals(testUserRequest.age, createdUser.age)

        // Verify user is cached
        val cachedUser = redisService.getUserById(createdUser.id)
        assertNotNull(cachedUser)
        assertEquals(createdUser.id, cachedUser!!.id)

        // Verify user is in database
        val dbUser = userMapper.findById(createdUser.id)
        assertNotNull(dbUser)
        assertEquals(createdUser.id, dbUser!!.id)
    }

    @Test
    fun `should get user from cache first`() {
        // Given
        val createdUser = userService.createUser(testUserRequest)

        // When - clear from database but keep in cache
        userMapper.deleteById(createdUser.id)

        // Then - should still get user from cache
        val retrievedUser = userService.getUserById(createdUser.id)
        assertNotNull(retrievedUser)
        assertEquals(createdUser.id, retrievedUser!!.id)
    }

    @Test
    fun `should fallback to database when not in cache`() {
        // Given
        val createdUser = userService.createUser(testUserRequest)

        // When - clear from cache but keep in database
        redisService.evictUser(createdUser.id)
        redisService.evictUserByEmail(createdUser.email)

        // Then - should get user from database and cache it
        val retrievedUser = userService.getUserById(createdUser.id)
        assertNotNull(retrievedUser)
        assertEquals(createdUser.id, retrievedUser!!.id)

        // Verify it's now cached again
        val cachedUser = redisService.getUserById(createdUser.id)
        assertNotNull(cachedUser)
    }

    @Test
    fun `should return null when user not found in cache or database`() {
        // When
        val retrievedUser = userService.getUserById(999L)

        // Then
        assertNull(retrievedUser)
    }

    @Test
    fun `should get user by email from cache first`() {
        // Given
        val createdUser = userService.createUser(testUserRequest)

        // When - clear from database but keep in cache
        userMapper.deleteById(createdUser.id)

        // Then - should still get user from cache
        val retrievedUser = userService.getUserByEmail(createdUser.email)
        assertNotNull(retrievedUser)
        assertEquals(createdUser.id, retrievedUser!!.id)
    }

    @Test
    fun `should fallback to database when email not in cache`() {
        // Given
        val createdUser = userService.createUser(testUserRequest)

        // When - clear from cache but keep in database
        redisService.evictUser(createdUser.id)
        redisService.evictUserByEmail(createdUser.email)

        // Then - should get user from database and cache it
        val retrievedUser = userService.getUserByEmail(createdUser.email)
        assertNotNull(retrievedUser)
        assertEquals(createdUser.id, retrievedUser!!.id)

        // Verify it's now cached again
        val cachedUser = redisService.getUserByEmail(createdUser.email)
        assertNotNull(cachedUser)
    }

    @Test
    fun `should update user and update cache`() {
        // Given
        val createdUser = userService.createUser(testUserRequest)
        val updateRequest = UserCreateRequest(
            email = createdUser.email,
            name = "Updated Name",
            age = 30
        )

        // When
        val updatedUser = userService.updateUser(createdUser.id, updateRequest)

        // Then
        assertNotNull(updatedUser)
        assertEquals(updateRequest.name, updatedUser!!.name)
        assertEquals(updateRequest.age, updatedUser.age)
        assertNotNull(updatedUser.updatedAt)

        // Verify cache is updated
        val cachedUser = redisService.getUserById(updatedUser.id)
        assertNotNull(cachedUser)
        assertEquals(updateRequest.name, cachedUser!!.name)
        assertEquals(updateRequest.age, cachedUser.age)

        // Verify database is updated
        val dbUser = userMapper.findById(updatedUser.id)
        assertNotNull(dbUser)
        assertEquals(updateRequest.name, dbUser!!.name)
        assertEquals(updateRequest.age, dbUser.age)
    }

    @Test
    fun `should delete user and remove from cache`() {
        // Given
        val createdUser = userService.createUser(testUserRequest)

        // When
        val deleted = userService.deleteUser(createdUser.id)

        // Then
        assertTrue(deleted)

        // Verify user is not in cache
        val cachedUser = redisService.getUserById(createdUser.id)
        assertNull(cachedUser)

        // Verify user is not in database
        val dbUser = userMapper.findById(createdUser.id)
        assertNull(dbUser)

        // Verify email cache is also cleared
        val cachedByEmail = redisService.getUserByEmail(createdUser.email)
        assertNull(cachedByEmail)
    }

    @Test
    fun `should get all users from database`() {
        // Given
        val user1 = userService.createUser(testUserRequest)
        val user2Request = testUserRequest.copy(email = "user2@test.com", name = "User 2")
        val user2 = userService.createUser(user2Request)

        // When
        val allUsers = userService.getAllUsers()

        // Then
        assertTrue(allUsers.size >= 2)
        assertTrue(allUsers.any { it.id == user1.id })
        assertTrue(allUsers.any { it.id == user2.id })
    }

    @Test
    fun `should throw exception when creating user with duplicate email`() {
        // Given
        userService.createUser(testUserRequest)

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            userService.createUser(testUserRequest)
        }
    }

    @Test
    fun `should throw exception when updating non-existent user`() {
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            userService.updateUser(999L, testUserRequest)
        }
    }

    @Test
    fun `should throw exception when deleting non-existent user`() {
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            userService.deleteUser(999L)
        }
    }

    @Test
    fun `should handle cache miss gracefully`() {
        // Given - user exists in database but not in cache
        val user = User(
            email = "cache-miss@test.com",
            name = "Cache Miss User",
            age = 35,
            createdAt = LocalDateTime.now()
        )
        userMapper.insert(user)

        // When
        val retrievedUser = userService.getUserById(user.id!!)

        // Then
        assertNotNull(retrievedUser)
        assertEquals(user.id, retrievedUser!!.id)
        assertEquals(user.email, retrievedUser.email)

        // Verify it's now cached
        val cachedUser = redisService.getUserById(user.id!!)
        assertNotNull(cachedUser)
    }
} 