package com.project.lighthouse.ui.meetings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.repository.MeetingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MeetingsViewModel(
    private val meetingsRepository: MeetingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MeetingsState(isLoading = true))
    val state: StateFlow<MeetingsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refreshTranscripts(initial = true)
    }

    fun refreshTranscripts(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d("MeetingsViewModel", "refreshTranscripts initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = meetingsRepository.getTranscripts(limit = 20)
            result.onSuccess { transcripts ->
                Log.d("MeetingsViewModel", "Transcripts loaded: ${transcripts.size} items")
                _state.update {
                    it.copy(
                        transcripts = transcripts,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                Log.e("MeetingsViewModel", "Failed to load transcripts: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to load transcripts"
                    )
                }
            }
        }
    }

    fun toggleCreateMeetingDialog(show: Boolean) {
        Log.d("MeetingsViewModel", "toggleCreateMeetingDialog: $show")
        _state.update { it.copy(showCreateMeetingDialog = show, errorMessage = null, infoMessage = null) }
        if (!show) {
            _state.update { it.copy(createMeetingFormState = CreateMeetingFormState()) }
        } else {
            // Set default start and end times (now + 1 hour)
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val startTime = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val endTime = now.plusHours(1).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            _state.update {
                it.copy(
                    createMeetingFormState = it.createMeetingFormState.copy(
                        startTime = startTime,
                        endTime = endTime,
                        timezone = ZoneId.systemDefault().id
                    )
                )
            }
        }
    }

    fun updateCreateMeetingForm(
        title: String? = null,
        startTime: String? = null,
        endTime: String? = null,
        description: String? = null,
        attendees: List<String>? = null,
        timezone: String? = null
    ) {
        _state.update {
            it.copy(
                createMeetingFormState = it.createMeetingFormState.copy(
                    title = title ?: it.createMeetingFormState.title,
                    startTime = startTime ?: it.createMeetingFormState.startTime,
                    endTime = endTime ?: it.createMeetingFormState.endTime,
                    description = description ?: it.createMeetingFormState.description,
                    attendees = attendees ?: it.createMeetingFormState.attendees,
                    timezone = timezone ?: it.createMeetingFormState.timezone
                )
            )
        }
    }

    fun addAttendee(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isNotBlank() && !_state.value.createMeetingFormState.attendees.contains(trimmedEmail)) {
            _state.update {
                it.copy(
                    createMeetingFormState = it.createMeetingFormState.copy(
                        attendees = it.createMeetingFormState.attendees + trimmedEmail
                    )
                )
            }
        }
    }

    fun removeAttendee(email: String) {
        _state.update {
            it.copy(
                createMeetingFormState = it.createMeetingFormState.copy(
                    attendees = it.createMeetingFormState.attendees.filter { it != email }
                )
            )
        }
    }

    fun createMeeting() {
        val currentForm = _state.value.createMeetingFormState
        if (currentForm.title.isBlank() || currentForm.startTime.isBlank() || currentForm.endTime.isBlank()) {
            Log.w("MeetingsViewModel", "Create meeting validation failed: title, startTime, and endTime required")
            _state.update { it.copy(errorMessage = "Title, start time, and end time are required") }
            return
        }
        viewModelScope.launch {
            Log.d("MeetingsViewModel", "Creating meeting: ${currentForm.title}")
            _state.update { it.copy(createMeetingFormState = it.createMeetingFormState.copy(isSubmitting = true)) }
            val result = meetingsRepository.createMeeting(
                title = currentForm.title.trim(),
                startTime = currentForm.startTime,
                endTime = currentForm.endTime,
                attendees = currentForm.attendees,
                description = currentForm.description.takeIf { it.isNotBlank() },
                timezone = currentForm.timezone
            )
            result.onSuccess { meeting ->
                Log.d("MeetingsViewModel", "Meeting created successfully: ${meeting.eventId}")
                _state.update {
                    it.copy(
                        createMeetingFormState = CreateMeetingFormState(),
                        showCreateMeetingDialog = false,
                        createdMeeting = meeting,
                        infoMessage = "Meeting created successfully"
                    )
                }
            }.onFailure { error ->
                Log.e("MeetingsViewModel", "Failed to create meeting: ${error.message}", error)
                _state.update {
                    it.copy(
                        createMeetingFormState = it.createMeetingFormState.copy(isSubmitting = false),
                        errorMessage = error.message ?: "Failed to create meeting"
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "MeetingsViewModel"
    }
}

