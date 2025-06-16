package com.example.kafkastudy.controller

import com.example.kafkastudy.dto.KafkaMessage
import com.example.kafkastudy.kafka.KafkaConsumer
import com.example.kafkastudy.kafka.KafkaProducer
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/kafka")
class KafkaController(
    private val kafkaProducer: KafkaProducer,
    private val kafkaConsumer: KafkaConsumer
) {
    
    private val logger = LoggerFactory.getLogger(KafkaController::class.java)
    
    @PostMapping("/send-user-event")
    fun sendUserEvent(@RequestBody message: KafkaMessage): ResponseEntity<String> {
        return try {
            logger.info("API: Sending user event: {}", message.message)
            kafkaProducer.sendUserEvent(message)
            ResponseEntity.ok("User event sent successfully")
        } catch (ex: Exception) {
            logger.error("API: Error sending user event: {}", ex.message, ex)
            ResponseEntity.internalServerError().body("Error sending user event: ${ex.message}")
        }
    }
    
    @PostMapping("/send-notification")
    fun sendNotification(@RequestParam message: String): ResponseEntity<String> {
        return try {
            logger.info("API: Sending notification: {}", message)
            kafkaProducer.sendNotification(message)
            ResponseEntity.ok("Notification sent successfully")
        } catch (ex: Exception) {
            logger.error("API: Error sending notification: {}", ex.message, ex)
            ResponseEntity.internalServerError().body("Error sending notification: ${ex.message}")
        }
    }
    
    @GetMapping("/processed-messages")
    fun getProcessedMessages(): ResponseEntity<List<KafkaMessage>> {
        return try {
            logger.info("API: Getting processed messages")
            val messages = kafkaConsumer.getProcessedMessages()
            ResponseEntity.ok(messages)
        } catch (ex: Exception) {
            logger.error("API: Error getting processed messages: {}", ex.message, ex)
            ResponseEntity.internalServerError().build()
        }
    }
    
    @DeleteMapping("/processed-messages")
    fun clearProcessedMessages(): ResponseEntity<String> {
        return try {
            logger.info("API: Clearing processed messages")
            kafkaConsumer.clearProcessedMessages()
            ResponseEntity.ok("Processed messages cleared successfully")
        } catch (ex: Exception) {
            logger.error("API: Error clearing processed messages: {}", ex.message, ex)
            ResponseEntity.internalServerError().body("Error clearing processed messages: ${ex.message}")
        }
    }
    
    @PostMapping("/test-kafka-flow")
    fun testKafkaFlow(@RequestParam message: String): ResponseEntity<String> {
        return try {
            logger.info("API: Testing Kafka flow with message: {}", message)
            
            val kafkaMessage = KafkaMessage(
                id = UUID.randomUUID().toString(),
                message = "Test message: $message"
            )
            
            // Send user event
            kafkaProducer.sendUserEvent(kafkaMessage)
            
            // Send notification
            kafkaProducer.sendNotification("Test notification: $message")
            
            ResponseEntity.ok("Kafka flow test completed successfully")
        } catch (ex: Exception) {
            logger.error("API: Error testing Kafka flow: {}", ex.message, ex)
            ResponseEntity.internalServerError().body("Error testing Kafka flow: ${ex.message}")
        }
    }
} 