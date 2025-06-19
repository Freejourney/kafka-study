package com.example.kafkastudy

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
    locations = ["classpath:application-test.yml"],
    properties = [
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
    ]
)
class KafkaStudyApplicationTests {

    @Test
    fun contextLoads() {
        // Test that the application context loads successfully with all configurations
        // This test verifies that:
        // - Spring Boot application starts correctly
        // - All beans are properly configured
        // - Database connections are established
        // - MyBatis mappers are loaded
    }
} 