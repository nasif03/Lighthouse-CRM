package com.project.lighthouse.ui.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.JiraRepository
import com.project.lighthouse.data.repository.TicketsRepository

class TicketDetailViewModelFactory(
    private val ticketsRepository: TicketsRepository,
    private val jiraRepository: JiraRepository,
    private val ticketId: String,
    private val currentUserName: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketDetailViewModel::class.java)) {
            return TicketDetailViewModel(ticketsRepository, jiraRepository, ticketId, currentUserName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

