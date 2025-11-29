package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.ChatChannel
import com.project.lighthouse.data.model.ChatMessage
import com.project.lighthouse.data.model.ChatUser
import com.project.lighthouse.data.model.CreateChannelRequest
import com.project.lighthouse.data.model.CreateChannelResponse
import com.project.lighthouse.data.model.SendMessageRequest
import com.project.lighthouse.data.model.SendMessageResponse
import com.project.lighthouse.data.model.StreamTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class ChatRepository {
    private val api = ApiClient.chatApi

    suspend fun getChatToken(): Result<StreamTokenResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ChatRepository", "Getting Stream Chat token")
            val response = api.getChatToken()
            if (response.isSuccessful && response.body() != null) {
                Log.d("ChatRepository", "Token received successfully")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("ChatRepository", "Failed to get token: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("ChatRepository", "Network error getting token", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("ChatRepository", "Unexpected error getting token", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getChatUsers(): Result<List<ChatUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d("ChatRepository", "Getting chat users")
            val response = api.getChatUsers()
            if (response.isSuccessful && response.body() != null) {
                val users = response.body()!!
                Log.d("ChatRepository", "Retrieved ${users.size} chat users")
                Result.success(users)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("ChatRepository", "Failed to get users: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("ChatRepository", "Network error getting users", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("ChatRepository", "Unexpected error getting users", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getChannels(): Result<List<ChatChannel>> = withContext(Dispatchers.IO) {
        try {
            Log.d("ChatRepository", "Getting chat channels")
            val response = api.getChannels()
            if (response.isSuccessful && response.body() != null) {
                val channels = response.body()!!
                Log.d("ChatRepository", "Retrieved ${channels.size} channels")
                Result.success(channels)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("ChatRepository", "Failed to get channels: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("ChatRepository", "Network error getting channels", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("ChatRepository", "Unexpected error getting channels", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createDirectChannel(userId: String): Result<CreateChannelResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ChatRepository", "Creating direct channel with user: $userId")
            val request = CreateChannelRequest(userId)
            val response = api.createDirectChannel(request)
            if (response.isSuccessful && response.body() != null) {
                Log.d("ChatRepository", "Channel created successfully")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("ChatRepository", "Failed to create channel: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("ChatRepository", "Network error creating channel", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("ChatRepository", "Unexpected error creating channel", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getMessages(
        channelType: String, 
        channelId: String, 
        limit: Int = 50, 
        offset: Int = 0
    ): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            Log.d("ChatRepository", "Getting messages for channel: $channelType/$channelId (limit=$limit, offset=$offset)")
            val response = api.getMessages(channelType, channelId, limit, offset)
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!
                Log.d("ChatRepository", "Retrieved ${messages.size} messages")
                Result.success(messages)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("ChatRepository", "Failed to get messages: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("ChatRepository", "Network error getting messages", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("ChatRepository", "Unexpected error getting messages", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun sendMessage(channelType: String, channelId: String, text: String): Result<SendMessageResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ChatRepository", "Sending message to channel: $channelType/$channelId")
            val request = SendMessageRequest(channelType, channelId, text)
            val response = api.sendMessage(request)
            if (response.isSuccessful && response.body() != null) {
                Log.d("ChatRepository", "Message sent successfully")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("ChatRepository", "Failed to send message: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("ChatRepository", "Network error sending message", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("ChatRepository", "Unexpected error sending message", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

