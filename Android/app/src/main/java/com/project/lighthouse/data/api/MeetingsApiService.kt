package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.CreateMeetingRequest
import com.project.lighthouse.data.model.FirefliesTranscript
import com.project.lighthouse.data.model.MeetingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MeetingsApiService {
    @POST("api/calendar/meetings")
    suspend fun createMeeting(@Body request: CreateMeetingRequest): Response<MeetingResponse>

    @GET("api/fireflies/transcripts")
    suspend fun getTranscripts(@Query("limit") limit: Int = 10): Response<List<FirefliesTranscript>>

    @GET("api/fireflies/transcripts/{transcript_id}")
    suspend fun getTranscript(@Path("transcript_id") transcriptId: String): Response<FirefliesTranscript>
}

