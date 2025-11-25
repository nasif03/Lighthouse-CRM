package com.project.lighthouse.ui.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.MeetingsRepository

class MeetingsViewModelFactory(
    private val meetingsRepository: MeetingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeetingsViewModel::class.java)) {
            return MeetingsViewModel(meetingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

