package com.example.kafkastudy

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
])
class KafkaStudyApplicationTests {

    @Test
    fun contextLoads() {
        // Test that the application context loads successfully
    }
} 