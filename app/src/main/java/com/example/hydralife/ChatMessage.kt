package com.example.hydralife

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoadingBubble: Boolean = false
)
