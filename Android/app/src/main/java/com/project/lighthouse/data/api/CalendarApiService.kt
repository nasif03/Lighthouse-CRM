package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.CalendarMeetingsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CalendarApiService {
    @GET("api/calendar/meetings")
    suspend fun getMeetings(
        @Query("time_min") timeMin: String? = null,
        @Query("time_max") timeMax: String? = null,
        @Query("max_results") maxResults: Int = 50
    ): Response<CalendarMeetingsResponse>
}

