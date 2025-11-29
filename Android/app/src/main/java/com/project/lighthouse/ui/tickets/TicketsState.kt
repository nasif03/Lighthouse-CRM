package com.project.lighthouse.ui.tickets

import com.project.lighthouse.data.model.AssignableEmployee
import com.project.lighthouse.data.model.TicketDto

data class CreateTicketFormState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val subject: String = "",
    val description: String = "",
    val priority: String = "medium",
    val category: String? = null,
    val isSubmitting: Boolean = false
)

data class TicketsState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val tickets: List<TicketDto> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val selectedTicket: TicketDto? = null,
    val createTicketFormState: CreateTicketFormState = CreateTicketFormState(),
    val isAdmin: Boolean = false,
    val assignableEmployees: List<AssignableEmployee> = emptyList(),
    val filterStatus: String? = null,
    val filterPriority: String? = null,
    val searchQuery: String = "",
    val createdTicketId: String? = null // For navigation after creation
)

