package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.GmailAuthRequest
import com.project.lighthouse.data.model.GmailAuthResponse
import com.project.lighthouse.data.model.GmailMessage
import com.project.lighthouse.data.model.GmailMessagesResponse
import com.project.lighthouse.data.model.SendEmailRequest
import com.project.lighthouse.data.model.SendEmailResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class GmailRepository {
    private val api = ApiClient.gmailApi

    suspend fun getAuthStatus(): Result<GmailAuthResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("GmailRepository", "Getting Gmail auth status")
            val response = api.getAuthStatus()
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("GmailRepository", "Auth status: ${response.body()?.authenticated}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("GmailRepository", "Failed to get auth status: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("GmailRepository", "Network error getting auth status", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("GmailRepository", "Unexpected error getting auth status", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun authenticate(
        authorizationCode: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null
    ): Result<GmailAuthResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("GmailRepository", "Authenticating Gmail")
            val request = GmailAuthRequest(
                authorizationCode = authorizationCode,
                accessToken = accessToken,
                refreshToken = refreshToken
            )
            val response = api.authenticate(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("GmailRepository", "Gmail authentication successful")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("GmailRepository", "Failed to authenticate: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("GmailRepository", "Network error authenticating", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("GmailRepository", "Unexpected error authenticating", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getMessages(maxResults: Int = 10, query: String = ""): Result<List<GmailMessage>> = withContext(Dispatchers.IO) {
        try {
            Log.d("GmailRepository", "Getting Gmail messages: maxResults=$maxResults, query=$query")
            val response = api.getMessages(maxResults, query)
            
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!.messages
                Log.d("GmailRepository", "Retrieved ${messages.size} messages")
                Result.success(messages)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("GmailRepository", "Failed to get messages: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("GmailRepository", "Network error getting messages", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("GmailRepository", "Unexpected error getting messages", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun sendEmail(to: String, subject: String, body: String): Result<SendEmailResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("GmailRepository", "Sending email to: $to, subject: $subject")
            val request = SendEmailRequest(to = to, subject = subject, body = body)
            val response = api.sendEmail(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("GmailRepository", "Email sent successfully")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("GmailRepository", "Failed to send email: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("GmailRepository", "Network error sending email", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("GmailRepository", "Unexpected error sending email", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

