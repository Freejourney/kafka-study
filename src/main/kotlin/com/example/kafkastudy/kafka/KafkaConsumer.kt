package com.example.kafkastudy.kafka

import com.example.kafkastudy.dto.KafkaMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class KafkaConsumer(
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(KafkaConsumer::class.java)
    private val processedMessages = mutableListOf<KafkaMessage>()
    
    @KafkaListener(topics = [KafkaProducer.USER_EVENTS_TOPIC], groupId = "user-events-group")
    fun consumeUserEvent(
        @Payload message: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received user event from topic '{}', partition {}, offset {}: {}", 
                topic, partition, offset, message)
            
            val kafkaMessage = objectMapper.readValue(message, KafkaMessage::class.java)
            processedMessages.add(kafkaMessage)
            
            // Process the message
            processUserEvent(kafkaMessage)
            
            // Acknowledge successful processing
            acknowledgment.acknowledge()
            logger.info("Successfully processed user event with id: {}", kafkaMessage.id)
            
        } catch (ex: Exception) {
            logger.error("Error processing user event from topic '{}': {}", topic, ex.message, ex)
            // In a real application, you might want to send to a dead letter queue
        }
    }
    
    @KafkaListener(topics = [KafkaProducer.NOTIFICATION_TOPIC], groupId = "notification-group")
    fun consumeNotification(
        @Payload message: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received notification from topic '{}', partition {}, offset {}: {}", 
                topic, partition, offset, message)
            
            // Process the notification
            processNotification(message)
            
            // Acknowledge successful processing
            acknowledgment.acknowledge()
            logger.info("Successfully processed notification: {}", message)
            
        } catch (ex: Exception) {
            logger.error("Error processing notification from topic '{}': {}", topic, ex.message, ex)
        }
    }
    
    private fun processUserEvent(message: KafkaMessage) {
        // Simulate processing logic
        logger.info("Processing user event: {}", message.message)
        Thread.sleep(100) // Simulate some processing time
    }
    
    private fun processNotification(message: String) {
        // Simulate notification processing
        logger.info("Processing notification: {}", message)
        Thread.sleep(50) // Simulate some processing time
    }
    
    // For testing purposes
    fun getProcessedMessages(): List<KafkaMessage> = processedMessages.toList()
    
    fun clearProcessedMessages() {
        processedMessages.clear()
    }
} 