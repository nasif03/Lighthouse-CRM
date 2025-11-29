package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class CreateMeetingRequest(
    @SerializedName("title") val title: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("attendees") val attendees: List<String> = emptyList(),
    @SerializedName("description") val description: String? = null,
    @SerializedName("timezone") val timezone: String = "UTC"
)

data class MeetingResponse(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("hangout_link") val hangoutLink: String? = null,
    @SerializedName("html_link") val htmlLink: String? = null,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String
)

data class MeetingFirefliesSummary(
    @SerializedName("overview") val overview: String? = null,
    @SerializedName("short_summary") val shortSummary: String? = null
)

data class MeetingFirefliesTranscript(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("date") val date: Long,
    @SerializedName("transcript_url") val transcriptUrl: String? = null,
    @SerializedName("summary") val summary: MeetingFirefliesSummary? = null
)

