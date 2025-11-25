package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.SupportChatHistoryResponse
import com.project.lighthouse.data.model.SupportChatRequest
import com.project.lighthouse.data.model.SupportChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SupportChatApiService {
    @POST("api/support-chat")
    suspend fun sendMessage(@Body request: SupportChatRequest): Response<SupportChatResponse>

    @GET("api/support-chat/history")
    suspend fun getHistory(@Query("conversationId") conversationId: String? = null): Response<SupportChatHistoryResponse>
}

