package com.example.kafkastudy.dto

import java.time.LocalDateTime

data class UserCreateRequest(
    val email: String,
    val name: String,
    val age: Int
)

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val age: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)

data class KafkaMessage(
    val id: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
) 