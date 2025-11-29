package com.project.lighthouse.ui.tickets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.TicketDto
import com.project.lighthouse.data.repository.TicketsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketDetailViewModel(
    private val ticketsRepository: TicketsRepository,
    private val ticketId: String,
    private val currentUserName: String = "Current User"
) : ViewModel() {

    private val _state = MutableStateFlow(TicketDetailState(isLoading = true))
    val state: StateFlow<TicketDetailState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadTicket()
        checkAdminStatus()
    }

    fun loadTicket() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d(TAG, "loadTicket: $ticketId")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = ticketsRepository.getTicket(ticketId)
            result.onSuccess { ticket ->
                Log.d(TAG, "Ticket loaded: ${ticket.ticketNumber}")
                _state.update {
                    it.copy(
                        ticket = ticket,
                        selectedStatus = ticket.status,
                        selectedPriority = ticket.priority,
                        selectedAssignee = ticket.assignedTo ?: "unassigned",
                        isLoading = false
                    )
                }
                // Load assignable employees if admin
                if (_state.value.isAdmin) {
                    loadAssignableEmployees()
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load ticket: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load ticket"
                    )
                }
            }
        }
    }

    fun checkAdminStatus() {
        viewModelScope.launch {
            val result = ticketsRepository.checkAdmin()
            result.onSuccess { isAdmin ->
                _state.update { it.copy(isAdmin = isAdmin) }
                if (isAdmin) {
                    loadAssignableEmployees()
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to check admin status: ${error.message}", error)
            }
        }
    }

    fun loadAssignableEmployees() {
        viewModelScope.launch {
            val result = ticketsRepository.getAssignableEmployees()
            result.onSuccess { employees ->
                Log.d(TAG, "Assignable employees loaded: ${employees.size}")
                _state.update { it.copy(employees = employees) }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load assignable employees: ${error.message}", error)
            }
        }
    }

    fun updateStatus(status: String) {
        val currentTicket = _state.value.ticket ?: return
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, errorMessage = null) }
            val result = ticketsRepository.updateTicket(
                ticketId = ticketId,
                status = status
            )
            result.onSuccess { updatedTicket ->
                Log.d(TAG, "Ticket status updated: $status")
                _state.update {
                    it.copy(
                        ticket = updatedTicket,
                        selectedStatus = status,
                        showStatusModal = false,
                        isUpdating = false
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to update status: ${error.message}", error)
                _state.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = error.message ?: "Failed to update ticket status"
                    )
                }
            }
        }
    }

    fun updatePriority(priority: String) {
        val currentTicket = _state.value.ticket ?: return
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, errorMessage = null) }
            val result = ticketsRepository.updateTicket(
                ticketId = ticketId,
                priority = priority
            )
            result.onSuccess { updatedTicket ->
                Log.d(TAG, "Ticket priority updated: $priority")
                _state.update {
                    it.copy(
                        ticket = updatedTicket,
                        selectedPriority = priority,
                        isUpdating = false
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to update priority: ${error.message}", error)
                _state.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = error.message ?: "Failed to update ticket priority"
                    )
                }
            }
        }
    }

    fun assignTicket(employeeId: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, errorMessage = null) }
            val result = ticketsRepository.updateTicket(
                ticketId = ticketId,
                assignedTo = employeeId
            )
            result.onSuccess { updatedTicket ->
                Log.d(TAG, "Ticket assigned: $employeeId")
                _state.update {
                    it.copy(
                        ticket = updatedTicket,
                        selectedAssignee = employeeId ?: "unassigned",
                        showAssignModal = false,
                        isUpdating = false
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to assign ticket: ${error.message}", error)
                _state.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = error.message ?: "Failed to assign ticket"
                    )
                }
            }
        }
    }

    fun addComment(content: String, isInternal: Boolean) {
        if (content.trim().isEmpty()) return
        val comment = TicketComment(
            id = "comment-${System.currentTimeMillis()}",
            author = currentUserName,
            authorId = "current-user",
            authorType = "agent",
            content = content.trim(),
            isInternal = isInternal,
            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        )
        _state.update {
            it.copy(
                comments = it.comments + comment,
                newComment = "",
                isInternalNote = false
            )
        }
    }

    fun updateNewComment(comment: String) {
        if (comment.length <= 5000) { // VALIDATION_LIMITS.NOTES
            _state.update { it.copy(newComment = comment) }
        }
    }

    fun toggleInternalNote(isInternal: Boolean) {
        _state.update { it.copy(isInternalNote = isInternal) }
    }

    fun toggleAssignModal(show: Boolean) {
        _state.update { it.copy(showAssignModal = show) }
    }

    fun toggleStatusModal(show: Boolean) {
        _state.update { it.copy(showStatusModal = show) }
    }

    fun updateSelectedAssignee(employeeId: String) {
        _state.update { it.copy(selectedAssignee = employeeId) }
    }

    fun updateSelectedStatus(status: String) {
        _state.update { it.copy(selectedStatus = status) }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "TicketDetailViewModel"
    }
}

