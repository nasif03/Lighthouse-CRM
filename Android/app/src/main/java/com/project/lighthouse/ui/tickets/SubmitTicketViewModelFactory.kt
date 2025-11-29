package com.project.lighthouse.ui.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.TicketsRepository

class SubmitTicketViewModelFactory(
    private val ticketsRepository: TicketsRepository,
    private val orgId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubmitTicketViewModel::class.java)) {
            return SubmitTicketViewModel(ticketsRepository, orgId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

