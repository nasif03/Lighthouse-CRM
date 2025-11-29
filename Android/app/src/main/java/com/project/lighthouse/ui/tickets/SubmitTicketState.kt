package com.project.lighthouse.ui.tickets

data class SubmitTicketState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val subject: String = "",
    val description: String = "",
    val priority: String = "medium",
    val category: String = "",
    val isSubmitting: Boolean = false,
    val success: Boolean = false,
    val ticketNumber: String? = null,
    val errorMessage: String? = null
)

