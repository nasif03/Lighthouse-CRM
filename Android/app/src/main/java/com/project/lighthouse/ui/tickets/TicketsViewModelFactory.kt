package com.project.lighthouse.ui.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.TicketsRepository

class TicketsViewModelFactory(
    private val ticketsRepository: TicketsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketsViewModel::class.java)) {
            return TicketsViewModel(ticketsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

