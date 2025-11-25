package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class CreateTicketRequest(
    @SerializedName("orgId") val orgId: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("subject") val subject: String,
    @SerializedName("description") val description: String,
    @SerializedName("priority") val priority: String? = "medium",
    @SerializedName("category") val category: String? = null
)

data class UpdateTicketRequest(
    @SerializedName("status") val status: String? = null,
    @SerializedName("priority") val priority: String? = null,
    @SerializedName("assignedTo") val assignedTo: String? = null,
    @SerializedName("category") val category: String? = null
)

data class TicketDto(
    @SerializedName("id") val id: String,
    @SerializedName("ticketNumber") val ticketNumber: String,
    @SerializedName("orgId") val orgId: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("subject") val subject: String,
    @SerializedName("description") val description: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("category") val category: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("assignedTo") val assignedTo: String? = null,
    @SerializedName("assignedToName") val assignedToName: String? = null,
    @SerializedName("jiraIssueKey") val jiraIssueKey: String? = null,
    @SerializedName("jiraIssueUrl") val jiraIssueUrl: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class AssignableEmployee(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("picture") val picture: String? = null
)

data class AdminCheckResponse(
    @SerializedName("isAdmin") val isAdmin: Boolean
)

