package com.example.kafkastudy.kafka

import com.example.kafkastudy.dto.KafkaMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class KafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(KafkaProducer::class.java)
    
    companion object {
        const val USER_EVENTS_TOPIC = "user-events"
        const val NOTIFICATION_TOPIC = "notifications"
    }
    
    fun sendUserEvent(message: KafkaMessage): CompletableFuture<SendResult<String, String>> {
        val jsonMessage = objectMapper.writeValueAsString(message)
        logger.info("Sending user event to topic '{}': {}", USER_EVENTS_TOPIC, jsonMessage)
        
        return kafkaTemplate.send(USER_EVENTS_TOPIC, message.id, jsonMessage).also { future ->
            future.whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Successfully sent message with key '{}' to topic '{}' at offset {}",
                        message.id, USER_EVENTS_TOPIC, result.recordMetadata.offset())
                } else {
                    logger.error("Failed to send message with key '{}' to topic '{}'", 
                        message.id, USER_EVENTS_TOPIC, ex)
                }
            }
        }
    }
    
    fun sendNotification(message: String): CompletableFuture<SendResult<String, String>> {
        logger.info("Sending notification to topic '{}': {}", NOTIFICATION_TOPIC, message)
        
        return kafkaTemplate.send(NOTIFICATION_TOPIC, message).also { future ->
            future.whenComplete { result, ex ->
                if (ex == null) {
                    logger.info("Successfully sent notification to topic '{}' at offset {}",
                        NOTIFICATION_TOPIC, result.recordMetadata.offset())
                } else {
                    logger.error("Failed to send notification to topic '{}'", 
                        NOTIFICATION_TOPIC, ex)
                }
            }
        }
    }
} 