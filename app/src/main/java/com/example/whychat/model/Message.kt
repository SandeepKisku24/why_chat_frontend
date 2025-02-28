package com.example.whychat.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val MessageID: String,
    val ChatGroupID: String,
    val SenderID: String,
    val Message: String,
    val timestamp: String,
    val MessageType: String
)

@Serializable
data class TextMessage(
    val SenderID: String,
    val Message: String
)

//{
//    "sender_id": "SANDeep1",
//    "message": "Hello, SANDEEP1!",
//    "message_type": "text"
//}

@Serializable
data class MessagesResponse(
    val messages: List<Message>
)
