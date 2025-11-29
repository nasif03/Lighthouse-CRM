package com.project.lighthouse.ui.tickets

import com.project.lighthouse.data.model.AssignableEmployee
import com.project.lighthouse.data.model.TicketDto

data class TicketComment(
    val id: String,
    val author: String,
    val authorId: String,
    val authorType: String, // "agent" or "customer"
    val content: String,
    val isInternal: Boolean,
    val createdAt: String
)

data class TicketDetailState(
    val ticket: TicketDto? = null,
    val employees: List<AssignableEmployee> = emptyList(),
    val comments: List<TicketComment> = emptyList(),
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val isCreatingJiraIssue: Boolean = false,
    val errorMessage: String? = null,
    val isAdmin: Boolean = false,
    val newComment: String = "",
    val isInternalNote: Boolean = false,
    val showAssignModal: Boolean = false,
    val showStatusModal: Boolean = false,
    val selectedAssignee: String = "unassigned",
    val selectedStatus: String = "open",
    val selectedPriority: String = "medium"
)

