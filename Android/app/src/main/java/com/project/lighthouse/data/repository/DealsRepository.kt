package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.CreateDealRequest
import com.project.lighthouse.data.model.DealDto
import com.project.lighthouse.data.model.UpdateDealRequest
import com.project.lighthouse.data.model.UpdateDealStageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class DealsRepository {

    suspend fun getDeals(): Result<List<DealDto>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching deals")
            val response = ApiClient.dealsApi.getDeals()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to fetch deals" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun createDeal(request: CreateDealRequest): Result<DealDto> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating deal ${request.name}")
            val response = ApiClient.dealsApi.createDeal(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to create deal" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun updateDeal(dealId: String, request: UpdateDealRequest): Result<DealDto> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating deal $dealId")
            val response = ApiClient.dealsApi.updateDeal(dealId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to update deal" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun updateDealStage(dealId: String, stageId: String, stageName: String?): Result<DealDto> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating deal $dealId stage -> $stageId")
                val response = ApiClient.dealsApi.updateDealStage(dealId, UpdateDealStageRequest(stageId, stageName))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to update deal" }))
                }
            } catch (e: IOException) {
                Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
            }
        }

    suspend fun deleteDeal(dealId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.dealsApi.deleteDeal(dealId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to delete deal" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    companion object {
        private const val TAG = "DealsRepository"
    }
}

