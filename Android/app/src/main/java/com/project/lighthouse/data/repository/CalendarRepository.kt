package com.project.lighthouse.data.repository

import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.CalendarMeeting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class CalendarRepository {
    suspend fun getMeetings(
        timeMin: String? = null,
        timeMax: String? = null,
        maxResults: Int = 50
    ): Result<List<CalendarMeeting>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.calendarApi.getMeetings(timeMin, timeMax, maxResults)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.meetings)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(ApiException.HttpError(response.code(), errorBody.ifBlank { "Failed to fetch meetings" }))
            }
        } catch (e: IOException) {
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

