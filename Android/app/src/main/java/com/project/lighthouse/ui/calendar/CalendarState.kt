package com.project.lighthouse.ui.calendar

import com.project.lighthouse.data.model.CalendarMeeting

data class CalendarState(
    val meetings: List<CalendarMeeting> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

