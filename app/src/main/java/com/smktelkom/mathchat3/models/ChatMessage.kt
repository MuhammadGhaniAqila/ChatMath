package com.smktelkom.mathchat3.models

import java.io.Serializable
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val ciphertext: String = "",
    val plaintext: String = "",
    val coeffA: Int = 3,
    val coeffB: Int = 2,
    val coeffC: Int = 2,
    val coeffD: Int = -1,
    val dateTime: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
