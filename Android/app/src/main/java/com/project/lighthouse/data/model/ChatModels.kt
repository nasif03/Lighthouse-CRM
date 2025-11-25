package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class StreamTokenResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user_id") val userId: String
)

data class ChatUser(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("picture") val picture: String? = null
)

data class ChatChannel(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("cid") val cid: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("members") val members: List<ChatUser>? = null,
    @SerializedName("other_member") val otherMember: ChatUser? = null,
    @SerializedName("lastMessage") val lastMessage: ChatMessage? = null,
    @SerializedName("unreadCount") val unreadCount: Int = 0
)

data class ChatMessage(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("user") val user: ChatUser? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CreateChannelRequest(
    @SerializedName("user_id") val userId: String
)

data class CreateChannelResponse(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("cid") val cid: String,
    @SerializedName("members") val members: List<ChatUser>? = null
)

data class SendMessageRequest(
    @SerializedName("channel_type") val channelType: String = "messaging",
    @SerializedName("channel_id") val channelId: String,
    @SerializedName("text") val text: String
)

data class SendMessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: ChatMessage? = null
)

