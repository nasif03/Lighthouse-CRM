package com.project.lighthouse.ui.jira

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.JiraRepository

class JiraViewModelFactory(
    private val jiraRepository: JiraRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JiraViewModel::class.java)) {
            return JiraViewModel(jiraRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

