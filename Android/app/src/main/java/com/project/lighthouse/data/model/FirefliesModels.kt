package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class FirefliesSummary(
    @SerializedName("overview") val overview: String? = null,
    @SerializedName("short_summary") val shortSummary: String? = null
)

data class FirefliesTranscript(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("date") val date: Long, // Unix timestamp
    @SerializedName("transcript_url") val transcriptUrl: String? = null,
    @SerializedName("summary") val summary: FirefliesSummary? = null
)

data class SyncTranscriptsResponse(
    @SerializedName("saved_transcripts") val savedTranscripts: Int
)

