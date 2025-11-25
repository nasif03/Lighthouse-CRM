package com.project.lighthouse.data.repository

import android.util.Log
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.api.ApiException
import com.project.lighthouse.data.model.CreateMeetingRequest
import com.project.lighthouse.data.model.FirefliesTranscript
import com.project.lighthouse.data.model.MeetingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class MeetingsRepository {
    private val api = ApiClient.meetingsApi

    suspend fun createMeeting(
        title: String,
        startTime: String,
        endTime: String,
        attendees: List<String> = emptyList(),
        description: String? = null,
        timezone: String = "UTC"
    ): Result<MeetingResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("MeetingsRepository", "Creating meeting: title=$title, startTime=$startTime, endTime=$endTime")
            val request = CreateMeetingRequest(
                title = title,
                startTime = startTime,
                endTime = endTime,
                attendees = attendees,
                description = description,
                timezone = timezone
            )
            val response = api.createMeeting(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("MeetingsRepository", "Meeting created successfully: ${response.body()?.eventId}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("MeetingsRepository", "Failed to create meeting: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("MeetingsRepository", "Network error creating meeting", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("MeetingsRepository", "Unexpected error creating meeting", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getTranscripts(limit: Int = 10): Result<List<FirefliesTranscript>> = withContext(Dispatchers.IO) {
        try {
            Log.d("MeetingsRepository", "Getting transcripts: limit=$limit")
            val response = api.getTranscripts(limit)
            
            if (response.isSuccessful && response.body() != null) {
                val transcripts = response.body()!!
                Log.d("MeetingsRepository", "Retrieved ${transcripts.size} transcripts")
                Result.success(transcripts)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("MeetingsRepository", "Failed to get transcripts: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("MeetingsRepository", "Network error getting transcripts", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("MeetingsRepository", "Unexpected error getting transcripts", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }

    suspend fun getTranscript(transcriptId: String): Result<FirefliesTranscript> = withContext(Dispatchers.IO) {
        try {
            Log.d("MeetingsRepository", "Getting transcript: id=$transcriptId")
            val response = api.getTranscript(transcriptId)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d("MeetingsRepository", "Transcript retrieved successfully")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("MeetingsRepository", "Failed to get transcript: $errorBody")
                Result.failure(
                    ApiException.HttpError(
                        code = response.code(),
                        message = errorBody
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("MeetingsRepository", "Network error getting transcript", e)
            Result.failure(ApiException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("MeetingsRepository", "Unexpected error getting transcript", e)
            Result.failure(ApiException.UnknownError("Unexpected error: ${e.message}"))
        }
    }
}

