package com.project.lighthouse.ui.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.repository.CalendarRepository
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

class CalendarViewModel(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState(isLoading = true))
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadMeetings(initial = true)
    }

    fun loadMeetings(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d(TAG, "loadMeetings initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null
                )
            }

            // Get upcoming meetings (next 30 days)
            val now = Instant.now()
            val thirtyDaysLater = now.plusSeconds(30 * 24 * 60 * 60L)
            val timeMin = now.toString()
            val timeMax = thirtyDaysLater.toString()

            val result = calendarRepository.getMeetings(
                timeMin = timeMin,
                timeMax = timeMax,
                maxResults = 50
            )

            result.onSuccess { meetings ->
                Log.d(TAG, "Meetings loaded: ${meetings.size} items")
                _state.update {
                    it.copy(
                        meetings = meetings,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load meetings: ${error.message}", error)
                val errorMessage = if (error.message?.contains("403") == true || 
                    error.message?.contains("not connected") == true) {
                    "Google Calendar is not connected. Please connect your Google account in Gmail settings."
                } else {
                    error.message ?: "Failed to load meetings"
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "CalendarViewModel"
    }
}

