package com.project.lighthouse.ui.tickets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.repository.TicketsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubmitTicketViewModel(
    private val ticketsRepository: TicketsRepository,
    private val orgId: String
) : ViewModel() {

    private val _state = MutableStateFlow(SubmitTicketState())
    val state: StateFlow<SubmitTicketState> = _state.asStateFlow()

    fun updateField(field: String, value: String) {
        _state.update {
            when (field) {
                "name" -> it.copy(name = value)
                "email" -> it.copy(email = value)
                "phone" -> it.copy(phone = value)
                "subject" -> {
                    if (value.length <= 200) it.copy(subject = value) else it
                }
                "description" -> {
                    if (value.length <= 5000) it.copy(description = value) else it
                }
                "priority" -> it.copy(priority = value)
                "category" -> it.copy(category = value)
                else -> it
            }
        }
    }

    fun submitTicket() {
        val current = _state.value
        if (current.name.isBlank() || current.email.isBlank() || 
            current.subject.isBlank() || current.description.isBlank()) {
            _state.update { it.copy(errorMessage = "Please fill in all required fields") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = ticketsRepository.createTicket(
                orgId = orgId,
                name = current.name.trim(),
                email = current.email.trim(),
                subject = current.subject.trim(),
                description = current.description.trim(),
                phone = current.phone.takeIf { it.isNotBlank() },
                priority = current.priority,
                category = current.category.takeIf { it.isNotBlank() }
            )
            result.onSuccess { ticket ->
                Log.d(TAG, "Ticket submitted: ${ticket.ticketNumber}")
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        success = true,
                        ticketNumber = ticket.ticketNumber,
                        name = "",
                        email = "",
                        phone = "",
                        subject = "",
                        description = "",
                        priority = "medium",
                        category = ""
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to submit ticket: ${error.message}", error)
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Failed to submit ticket. Please try again."
                    )
                }
            }
        }
    }

    fun resetSuccess() {
        _state.update { it.copy(success = false, ticketNumber = null) }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "SubmitTicketViewModel"
    }
}

