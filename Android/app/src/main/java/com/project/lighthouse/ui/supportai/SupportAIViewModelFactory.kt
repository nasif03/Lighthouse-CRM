package com.project.lighthouse.ui.supportai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.SupportChatRepository

class SupportAIViewModelFactory(
    private val supportChatRepository: SupportChatRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupportAIViewModel::class.java)) {
            return SupportAIViewModel(supportChatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

