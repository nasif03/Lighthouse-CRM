package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.FirefliesTranscript
import com.project.lighthouse.data.model.SyncTranscriptsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FirefliesApiService {
    @GET("api/fireflies/transcripts")
    suspend fun getTranscripts(
        @Query("limit") limit: Int = 20
    ): Response<List<FirefliesTranscript>>

    @GET("api/fireflies/sync_transcripts")
    suspend fun syncTranscripts(
        @Query("limit") limit: Int = 20
    ): Response<SyncTranscriptsResponse>
}

