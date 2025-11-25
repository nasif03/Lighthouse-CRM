package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.GmailAuthRequest
import com.project.lighthouse.data.model.GmailAuthResponse
import com.project.lighthouse.data.model.GmailMessagesResponse
import com.project.lighthouse.data.model.SendEmailRequest
import com.project.lighthouse.data.model.SendEmailResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GmailApiService {
    @GET("api/gmail/auth/status")
    suspend fun getAuthStatus(): Response<GmailAuthResponse>

    @POST("api/gmail/auth/callback")
    suspend fun authenticate(@Body request: GmailAuthRequest): Response<GmailAuthResponse>

    @GET("api/gmail/messages")
    suspend fun getMessages(
        @Query("max_results") maxResults: Int = 10,
        @Query("query") query: String = ""
    ): Response<GmailMessagesResponse>

    @POST("api/gmail/send")
    suspend fun sendEmail(@Body request: SendEmailRequest): Response<SendEmailResponse>
}

