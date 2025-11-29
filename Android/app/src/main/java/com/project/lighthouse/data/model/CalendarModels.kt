package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class CalendarMeeting(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("hangout_link") val hangoutLink: String? = null,
    @SerializedName("html_link") val htmlLink: String? = null,
    @SerializedName("attendees") val attendees: List<String> = emptyList(),
    @SerializedName("status") val status: String = "confirmed"
)

data class CalendarMeetingsResponse(
    @SerializedName("meetings") val meetings: List<CalendarMeeting>
)

