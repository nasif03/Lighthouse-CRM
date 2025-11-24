package com.project.lighthouse.data.repository

import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.local.TokenManager
import com.project.lighthouse.data.model.OrganizationResponse
import com.project.lighthouse.data.model.TokenResponse
import com.project.lighthouse.data.model.UserResponse
import com.project.lighthouse.data.model.VerifyTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AuthRepository(
    private val tokenManager: TokenManager
) {
    suspend fun verifyToken(idToken: String): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val request = VerifyTokenRequest(idToken)
            val response = ApiClient.authApi.verifyToken(request)
            
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                // Save token to secure storage
                tokenManager.saveToken(tokenResponse.token)
                Result.success(tokenResponse)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getCurrentUser(): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApi.getCurrentUser()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApi.logout()
            
            if (response.isSuccessful) {
                // Clear token from storage
                tokenManager.clearToken()
                Result.success(Unit)
            } else {
                // Even if logout fails on backend, clear local token
                tokenManager.clearToken()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // Clear token even on error
            tokenManager.clearToken()
            Result.success(Unit)
        }
    }

    suspend fun getOrganizations(): Result<List<OrganizationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.authApi.getOrganizations()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

