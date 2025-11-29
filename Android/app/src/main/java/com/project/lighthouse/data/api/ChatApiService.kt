package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.ChatChannel
import com.project.lighthouse.data.model.ChatMessage
import com.project.lighthouse.data.model.ChatUser
import com.project.lighthouse.data.model.CreateChannelRequest
import com.project.lighthouse.data.model.CreateChannelResponse
import com.project.lighthouse.data.model.SendMessageRequest
import com.project.lighthouse.data.model.SendMessageResponse
import com.project.lighthouse.data.model.StreamTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {
    @GET("api/chat/token")
    suspend fun getChatToken(): Response<StreamTokenResponse>

    @GET("api/chat/users")
    suspend fun getChatUsers(): Response<List<ChatUser>>

    @GET("api/chat/channels")
    suspend fun getChannels(): Response<List<ChatChannel>>

    @POST("api/chat/channels/direct")
    suspend fun createDirectChannel(@Body request: CreateChannelRequest): Response<CreateChannelResponse>

    @GET("api/chat/channels/{channel_type}/{channel_id}/messages")
    suspend fun getMessages(
        @Path("channel_type") channelType: String,
        @Path("channel_id") channelId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<ChatMessage>>

    @POST("api/chat/messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<SendMessageResponse>
}

