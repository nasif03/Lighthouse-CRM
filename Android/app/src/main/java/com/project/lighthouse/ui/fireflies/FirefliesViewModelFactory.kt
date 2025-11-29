package com.project.lighthouse.ui.fireflies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.FirefliesRepository

class FirefliesViewModelFactory(
    private val firefliesRepository: FirefliesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FirefliesViewModel::class.java)) {
            return FirefliesViewModel(firefliesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

