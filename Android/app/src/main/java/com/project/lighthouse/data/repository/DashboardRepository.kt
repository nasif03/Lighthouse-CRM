package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.DashboardStatsResponse
import com.project.lighthouse.data.model.RecentItemsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class DashboardRepository {

    suspend fun getDashboardStats(): Result<DashboardStatsResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching dashboard stats")
            val response = ApiClient.dashboardApi.getDashboardStats()
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Dashboard stats success")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e(TAG, "Dashboard stats error: ${response.code()} $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody.ifBlank { "Unknown error" }
                    )
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error dashboard stats: ${e.message}", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error dashboard stats: ${e.message}", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getRecentItems(): Result<RecentItemsResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching dashboard recent items")
            val response = ApiClient.dashboardApi.getRecentItems()
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Dashboard recent success")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e(TAG, "Dashboard recent error: ${response.code()} $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody.ifBlank { "Unknown error" }
                    )
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error dashboard recent: ${e.message}", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error dashboard recent: ${e.message}", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    companion object {
        private const val TAG = "DashboardRepository"
    }
}

