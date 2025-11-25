package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.SupportChatHistoryResponse
import com.project.lighthouse.data.model.SupportChatRequest
import com.project.lighthouse.data.model.SupportChatResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class SupportChatRepository {
    private val api = ApiClient.supportChatApi

    suspend fun sendMessage(
        message: String,
        conversationId: String? = null
    ): Result<SupportChatResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("SupportChatRepository", "Sending message to support AI: message length=${message.length}, conversationId=$conversationId")
            val request = SupportChatRequest(message, conversationId)
            val response = api.sendMessage(request)
            if (response.isSuccessful && response.body() != null) {
                Log.d("SupportChatRepository", "Support AI response received: length=${response.body()!!.reply.length}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("SupportChatRepository", "Failed to send message: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("SupportChatRepository", "Network error sending message", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("SupportChatRepository", "Unexpected error sending message", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getHistory(conversationId: String? = null): Result<SupportChatHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("SupportChatRepository", "Getting conversation history: conversationId=$conversationId")
            val response = api.getHistory(conversationId)
            if (response.isSuccessful && response.body() != null) {
                val history = response.body()!!
                Log.d("SupportChatRepository", "Retrieved ${history.messages.size} messages from history")
                Result.success(history)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("SupportChatRepository", "Failed to get history: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("SupportChatRepository", "Network error getting history", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("SupportChatRepository", "Unexpected error getting history", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

