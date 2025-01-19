package com.example.nothingchat.model

data class Channel (
    val id: String,
    val name: String,
    val isFavorite: Boolean = false,
    val createdAty: Long = System.currentTimeMillis(),
    val hasPassword: Boolean = false
)