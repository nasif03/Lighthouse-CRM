package com.project.lighthouse.ui.meetings

import com.project.lighthouse.data.model.FirefliesTranscript
import com.project.lighthouse.data.model.MeetingResponse

data class CreateMeetingFormState(
    val title: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val attendees: List<String> = emptyList(),
    val description: String = "",
    val timezone: String = "UTC",
    val isSubmitting: Boolean = false
)

data class MeetingsState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val transcripts: List<FirefliesTranscript> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateMeetingDialog: Boolean = false,
    val createMeetingFormState: CreateMeetingFormState = CreateMeetingFormState(),
    val createdMeeting: MeetingResponse? = null
)

