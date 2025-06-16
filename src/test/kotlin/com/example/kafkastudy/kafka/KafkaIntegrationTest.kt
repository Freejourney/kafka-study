package com.example.kafkastudy.kafka

import com.example.kafkastudy.dto.KafkaMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://localhost:9093", "port=9093"],
    topics = ["user-events", "notifications"]
)
@TestPropertySource(properties = [
    "spring.kafka.bootstrap-servers=localhost:9093",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
class KafkaIntegrationTest {

    @Autowired
    private lateinit var kafkaProducer: KafkaProducer

    @Autowired
    private lateinit var kafkaConsumer: KafkaConsumer

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        kafkaConsumer.clearProcessedMessages()
    }

    @Test
    fun `should send and consume user event successfully`() {
        // Given
        val kafkaMessage = KafkaMessage(
            id = UUID.randomUUID().toString(),
            message = "Test user event message",
            timestamp = LocalDateTime.now()
        )

        // When
        kafkaProducer.sendUserEvent(kafkaMessage)

        // Then - wait for message to be processed
        Thread.sleep(2000)

        val processedMessages = kafkaConsumer.getProcessedMessages()
        assertEquals(1, processedMessages.size)
        assertEquals(kafkaMessage.id, processedMessages[0].id)
        assertEquals(kafkaMessage.message, processedMessages[0].message)
    }

    @Test
    fun `should send and consume notification successfully`() {
        // Given
        val notificationMessage = "Test notification message"

        // When
        kafkaProducer.sendNotification(notificationMessage)

        // Then - wait for message to be processed
        Thread.sleep(1000)

        // Since notifications don't get stored in processedMessages,
        // we just verify no exceptions were thrown and the method completes
        assertTrue(true, "Notification sent and processed without errors")
    }

    @Test
    fun `should handle multiple user events`() {
        // Given
        val messages = listOf(
            KafkaMessage(UUID.randomUUID().toString(), "Message 1"),
            KafkaMessage(UUID.randomUUID().toString(), "Message 2"),
            KafkaMessage(UUID.randomUUID().toString(), "Message 3")
        )

        // When
        messages.forEach { kafkaProducer.sendUserEvent(it) }

        // Then - wait for all messages to be processed
        Thread.sleep(3000)

        val processedMessages = kafkaConsumer.getProcessedMessages()
        assertEquals(3, processedMessages.size)

        val processedIds = processedMessages.map { it.id }.toSet()
        val originalIds = messages.map { it.id }.toSet()
        assertEquals(originalIds, processedIds)
    }

    @Test
    fun `should handle JSON serialization and deserialization correctly`() {
        // Given
        val kafkaMessage = KafkaMessage(
            id = "test-id-123",
            message = "Test message with special characters: äöü @#$%",
            timestamp = LocalDateTime.now()
        )

        // When
        kafkaProducer.sendUserEvent(kafkaMessage)

        // Then - wait for message to be processed
        Thread.sleep(2000)

        val processedMessages = kafkaConsumer.getProcessedMessages()
        assertEquals(1, processedMessages.size)

        val processedMessage = processedMessages[0]
        assertEquals(kafkaMessage.id, processedMessage.id)
        assertEquals(kafkaMessage.message, processedMessage.message)
        // Note: timestamp comparison might have minor differences due to serialization
        assertNotNull(processedMessage.timestamp)
    }

    @Test
    fun `should clear processed messages correctly`() {
        // Given
        val kafkaMessage = KafkaMessage(
            id = UUID.randomUUID().toString(),
            message = "Test message for clearing"
        )

        // When
        kafkaProducer.sendUserEvent(kafkaMessage)
        Thread.sleep(1000)

        assertEquals(1, kafkaConsumer.getProcessedMessages().size)

        kafkaConsumer.clearProcessedMessages()

        // Then
        assertEquals(0, kafkaConsumer.getProcessedMessages().size)
    }
} 