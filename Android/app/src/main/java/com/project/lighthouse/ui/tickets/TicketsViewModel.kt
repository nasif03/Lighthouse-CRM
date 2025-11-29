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

class TicketsViewModel(
    private val ticketsRepository: TicketsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TicketsState(isLoading = true))
    val state: StateFlow<TicketsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        checkAdminStatus()
        refreshTickets(initial = true)
    }

    fun checkAdminStatus() {
        viewModelScope.launch {
            Log.d("TicketsViewModel", "Checking admin status")
            val result = ticketsRepository.checkAdmin()
            result.onSuccess { isAdmin ->
                Log.d("TicketsViewModel", "Admin status: $isAdmin")
                _state.update { it.copy(isAdmin = isAdmin) }
                if (isAdmin) {
                    loadAssignableEmployees()
                }
            }.onFailure { error ->
                Log.e("TicketsViewModel", "Failed to check admin: ${error.message}", error)
            }
        }
    }

    fun loadAssignableEmployees() {
        viewModelScope.launch {
            Log.d("TicketsViewModel", "Loading assignable employees")
            val result = ticketsRepository.getAssignableEmployees()
            result.onSuccess { employees ->
                Log.d("TicketsViewModel", "Loaded ${employees.size} assignable employees")
                _state.update { it.copy(assignableEmployees = employees) }
            }.onFailure { error ->
                Log.e("TicketsViewModel", "Failed to load assignable employees: ${error.message}", error)
            }
        }
    }

    fun refreshTickets(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d("TicketsViewModel", "refreshTickets initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = ticketsRepository.getTickets(
                status = _state.value.filterStatus,
                priority = _state.value.filterPriority
            )
            result.onSuccess { tickets ->
                Log.d("TicketsViewModel", "Tickets loaded: ${tickets.size} items")
                _state.update {
                    it.copy(
                        tickets = tickets,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                Log.e("TicketsViewModel", "Failed to load tickets: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to load tickets"
                    )
                }
            }
        }
    }

    fun setFilter(status: String? = null, priority: String? = null) {
        Log.d("TicketsViewModel", "Setting filter: status=$status, priority=$priority")
        _state.update {
            it.copy(filterStatus = status, filterPriority = priority)
        }
        refreshTickets(initial = true)
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        refreshTickets(initial = true)
    }

    fun toggleCreateDialog(show: Boolean) {
        Log.d("TicketsViewModel", "toggleCreateDialog: $show")
        _state.update { it.copy(showCreateDialog = show, errorMessage = null, infoMessage = null) }
        if (!show) {
            _state.update { it.copy(createTicketFormState = CreateTicketFormState()) }
        }
    }

    fun toggleUpdateDialog(show: Boolean, ticket: TicketDto? = null) {
        Log.d("TicketsViewModel", "toggleUpdateDialog: $show, ticket=${ticket?.id}")
        _state.update {
            it.copy(
                showUpdateDialog = show,
                selectedTicket = ticket,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun updateCreateTicketForm(
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        subject: String? = null,
        description: String? = null,
        priority: String? = null,
        category: String? = null
    ) {
        _state.update {
            it.copy(
                createTicketFormState = it.createTicketFormState.copy(
                    name = name ?: it.createTicketFormState.name,
                    email = email ?: it.createTicketFormState.email,
                    phone = phone ?: it.createTicketFormState.phone,
                    subject = subject ?: it.createTicketFormState.subject,
                    description = description ?: it.createTicketFormState.description,
                    priority = priority ?: it.createTicketFormState.priority,
                    category = category ?: it.createTicketFormState.category
                )
            )
        }
    }

    fun createTicket(orgId: String) {
        val currentForm = _state.value.createTicketFormState
        if (currentForm.name.isBlank() || currentForm.email.isBlank() || 
            currentForm.subject.isBlank() || currentForm.description.isBlank()) {
            Log.w("TicketsViewModel", "Create ticket validation failed: required fields missing")
            _state.update { it.copy(errorMessage = "Name, email, subject, and description are required") }
            return
        }
        viewModelScope.launch {
            Log.d("TicketsViewModel", "Creating ticket: ${currentForm.subject}")
            _state.update { it.copy(createTicketFormState = it.createTicketFormState.copy(isSubmitting = true)) }
            val result = ticketsRepository.createTicket(
                orgId = orgId,
                name = currentForm.name.trim(),
                email = currentForm.email.trim(),
                subject = currentForm.subject.trim(),
                description = currentForm.description.trim(),
                phone = currentForm.phone.takeIf { it.isNotBlank() },
                priority = currentForm.priority,
                category = currentForm.category
            )
            result.onSuccess { ticket ->
                Log.d("TicketsViewModel", "Ticket created successfully: ${ticket.ticketNumber}")
                _state.update {
                    it.copy(
                        createTicketFormState = CreateTicketFormState(),
                        showCreateDialog = false,
                        infoMessage = "Ticket created: ${ticket.ticketNumber}",
                        createdTicketId = ticket.id
                    )
                }
                refreshTickets()
            }.onFailure { error ->
                Log.e("TicketsViewModel", "Failed to create ticket: ${error.message}", error)
                _state.update {
                    it.copy(
                        createTicketFormState = it.createTicketFormState.copy(isSubmitting = false),
                        errorMessage = error.message ?: "Failed to create ticket"
                    )
                }
            }
        }
    }

    fun updateTicket(
        ticketId: String,
        status: String? = null,
        priority: String? = null,
        assignedTo: String? = null,
        category: String? = null
    ) {
        viewModelScope.launch {
            Log.d("TicketsViewModel", "Updating ticket: $ticketId")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = ticketsRepository.updateTicket(ticketId, status, priority, assignedTo, category)
            result.onSuccess { ticket ->
                Log.d("TicketsViewModel", "Ticket updated successfully")
                _state.update {
                    it.copy(
                        isLoading = false,
                        showUpdateDialog = false,
                        selectedTicket = null,
                        infoMessage = "Ticket updated successfully"
                    )
                }
                refreshTickets()
            }.onFailure { error ->
                Log.e("TicketsViewModel", "Failed to update ticket: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to update ticket"
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun clearCreatedTicketId() {
        _state.update { it.copy(createdTicketId = null) }
    }

    companion object {
        private const val TAG = "TicketsViewModel"
    }
}

