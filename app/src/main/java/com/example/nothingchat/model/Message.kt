package com.example.nothingchat.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val senderName: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null
)
