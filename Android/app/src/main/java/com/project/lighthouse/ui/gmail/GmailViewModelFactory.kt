package com.project.lighthouse.ui.gmail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.GmailRepository

class GmailViewModelFactory(
    private val gmailRepository: GmailRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GmailViewModel::class.java)) {
            return GmailViewModel(gmailRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

