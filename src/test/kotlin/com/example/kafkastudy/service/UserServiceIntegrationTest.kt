package com.example.kafkastudy.service

import com.example.kafkastudy.entity.User
import com.example.kafkastudy.mapper.UserMapper
import com.example.kafkastudy.testutil.TestDataFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
    locations = ["classpath:application-test.yml"],
    properties = [
        "spring.jpa.show-sql=false",
        "logging.level.org.springframework.transaction=WARN"
    ]
)
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userMapper: UserMapper

    @Autowired
    private lateinit var redisService: RedisService

    private val testUser1 = TestDataFactory.TestUsers.USER_1
    private val testUser2 = TestDataFactory.TestUsers.USER_2
    private val testUser3 = TestDataFactory.TestUsers.USER_3

    @BeforeEach
    fun setUp() {
        // Minimal cleanup for speed
        try {
            listOf(testUser1, testUser2, testUser3).forEach { testUser ->
                redisService.evictUser(testUser.id)
                redisService.evictUserByEmail(testUser.email)
            }
        } catch (e: Exception) {
            // Ignore cleanup errors for speed
        }
    }

    @Test
    fun `should create user and cache it with consistent data across all systems`() {
        // Given - Use shared test data
        val createRequest = testUser1.toCreateRequest()

        // When
        val createdUser = userService.createUser(createRequest)

        // Then - Verify response
        assertNotNull(createdUser)
        assertEquals(createRequest.email, createdUser.email)
        assertEquals(createRequest.name, createdUser.name)
        assertEquals(createRequest.age, createdUser.age)

        // Verify user is cached in Redis
        val cachedUser = redisService.getUserById(createdUser.id)
        assertNotNull(cachedUser)
        assertEquals(createdUser.id, cachedUser!!.id)
        assertEquals(createdUser.email, cachedUser.email)
        assertEquals(createdUser.name, cachedUser.name)

        // Verify user is in database
        val dbUser = userMapper.findById(createdUser.id)
        assertNotNull(dbUser)
        assertEquals(createdUser.id, dbUser!!.id)
        assertEquals(createdUser.email, dbUser.email)
        assertEquals(createdUser.name, dbUser.name)
    }

    @Test
    fun `should get user from cache first demonstrating cache-first pattern`() {
        // Given - Create user through service (populates cache and DB)
        val createRequest = testUser1.toCreateRequest()
        val createdUser = userService.createUser(createRequest)

        // When - Delete from database but keep in cache
        userMapper.deleteById(createdUser.id)

        // Then - Should still get user from cache
        val retrievedUser = userService.getUserById(createdUser.id)
        assertNotNull(retrievedUser)
        assertEquals(createdUser.id, retrievedUser!!.id)
        assertEquals(createdUser.email, retrievedUser.email)
        assertEquals(createdUser.name, retrievedUser.name)
    }

    @Test
    fun `should fallback to database when not in cache and repopulate cache`() {
        // Given - Create user through service
        val createRequest = testUser1.toCreateRequest()
        val createdUser = userService.createUser(createRequest)

        // When - Clear from cache but keep in database
        redisService.evictUser(createdUser.id)
        redisService.evictUserByEmail(createdUser.email)

        // Then - Should get user from database and cache it
        val retrievedUser = userService.getUserById(createdUser.id)
        assertNotNull(retrievedUser)
        assertEquals(createdUser.id, retrievedUser!!.id)

        // Verify it's now cached again
        val cachedUser = redisService.getUserById(createdUser.id)
        assertNotNull(cachedUser)
        assertEquals(createdUser.id, cachedUser!!.id)
    }

    @Test
    fun `should return null when user not found in cache or database`() {
        // When
        val retrievedUser = userService.getUserById(999L)

        // Then
        assertNull(retrievedUser)
    }

    @Test
    fun `should get user by email with consistent cache-db behavior`() {
        // Given - Use shared test data
        val createRequest = testUser1.toCreateRequest()
        val createdUser = userService.createUser(createRequest)

        // When - Delete from database but keep in cache
        userMapper.deleteById(createdUser.id)

        // Then - Should still get user from cache
        val retrievedUser = userService.getUserByEmail(createdUser.email)
        assertNotNull(retrievedUser)
        assertEquals(createdUser.id, retrievedUser!!.id)
    }

    @Test
    fun `should update user and update cache with consistent data`() {
        // Given - Use shared test data
        val createRequest = testUser1.toCreateRequest()
        val createdUser = userService.createUser(createRequest)
        
        val updateRequest = testUser1.copy(
            name = "Updated ${testUser1.name}",
            age = testUser1.age + 5
        ).toCreateRequest()

        // When
        val updatedUser = userService.updateUser(createdUser.id, updateRequest)

        // Then - Verify response
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
    fun `should delete user and remove from cache and database`() {
        // Given - Use shared test data
        val createRequest = testUser1.toCreateRequest()
        val createdUser = userService.createUser(createRequest)

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
    fun `should get all users from database with consistent data`() {
        // Given - Use shared test data (only create 2 users for speed)
        val user1 = userService.createUser(testUser1.toCreateRequest())
        val user2 = userService.createUser(testUser2.toCreateRequest())

        // When
        val allUsers = userService.getAllUsers()

        // Then - Should include our test users plus any existing ones
        assertTrue(allUsers.size >= 2)
        val userIds = allUsers.map { it.id }.toSet()
        assertTrue(userIds.contains(user1.id))
        assertTrue(userIds.contains(user2.id))
    }

    @Test
    fun `should throw exception when creating user with duplicate email`() {
        // Given - Use shared test data
        val createRequest = testUser1.toCreateRequest()
        userService.createUser(createRequest)

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            userService.createUser(createRequest)
        }
    }

    @Test
    fun `should throw exception when updating non-existent user`() {
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            userService.updateUser(999L, testUser1.toCreateRequest())
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
    fun `should handle cache miss gracefully and maintain data consistency`() {
        // Given - Insert user directly into database (bypassing cache)
        val userEntity = testUser1.toEntity().copy(id = null) // Let DB generate ID
        userMapper.insert(userEntity)
        val insertedId = userEntity.id!!

        // When - Get user through service (should hit DB and populate cache)
        val retrievedUser = userService.getUserById(insertedId)

        // Then - Verify response
        assertNotNull(retrievedUser)
        assertEquals(insertedId, retrievedUser!!.id)
        assertEquals(testUser1.email, retrievedUser.email)
        assertEquals(testUser1.name, retrievedUser.name)

        // Verify it's now cached
        val cachedUser = redisService.getUserById(insertedId)
        assertNotNull(cachedUser)
        assertEquals(insertedId, cachedUser!!.id)
        assertEquals(testUser1.email, cachedUser.email)
    }
} 