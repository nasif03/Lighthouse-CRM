package com.project.lighthouse.data.repository

import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.FirefliesTranscript
import com.project.lighthouse.data.model.SyncTranscriptsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class FirefliesRepository {
    suspend fun getTranscripts(limit: Int = 20): Result<List<FirefliesTranscript>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.firefliesApi.getTranscripts(limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to fetch transcripts" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun syncTranscripts(limit: Int = 20): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.firefliesApi.syncTranscripts(limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.savedTranscripts)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to sync transcripts" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

