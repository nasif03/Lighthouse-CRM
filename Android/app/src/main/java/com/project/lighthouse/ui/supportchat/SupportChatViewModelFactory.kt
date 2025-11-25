package com.project.lighthouse.ui.supportchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.SupportChatRepository

class SupportChatViewModelFactory(
    private val supportChatRepository: SupportChatRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupportChatViewModel::class.java)) {
            return SupportChatViewModel(supportChatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

