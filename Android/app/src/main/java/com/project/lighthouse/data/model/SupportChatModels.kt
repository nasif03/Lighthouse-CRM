package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class SupportChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("conversationId") val conversationId: String? = null,
    @SerializedName("history") val history: List<ChatTurn>? = null
)

data class ChatTurn(
    @SerializedName("role") val role: String, // "user" or "assistant"
    @SerializedName("content") val content: String
)

data class SupportChatResponse(
    @SerializedName("reply") val reply: String,
    @SerializedName("conversationId") val conversationId: String
)

data class SupportChatMessage(
    @SerializedName("id") val id: String,
    @SerializedName("role") val role: String, // "user" or "assistant"
    @SerializedName("content") val content: String,
    @SerializedName("createdAt") val createdAt: String
)

data class SupportChatHistoryResponse(
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("messages") val messages: List<SupportChatMessage>
)

