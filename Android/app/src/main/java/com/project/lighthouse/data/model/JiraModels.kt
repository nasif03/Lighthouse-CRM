package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class JiraProjectResponse(
    @SerializedName("message") val message: String,
    @SerializedName("projectKey") val projectKey: String,
    @SerializedName("projectName") val projectName: String? = null,
    @SerializedName("projectUrl") val projectUrl: String? = null,
    @SerializedName("serviceDeskId") val serviceDeskId: String? = null
)

data class JiraIssue(
    @SerializedName("key") val key: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("priority") val priority: String? = null,
    @SerializedName("issueType") val issueType: String? = null,
    @SerializedName("reporterName") val reporterName: String? = null,
    @SerializedName("reporterEmail") val reporterEmail: String? = null,
    @SerializedName("assigneeName") val assigneeName: String? = null,
    @SerializedName("created") val created: String? = null,
    @SerializedName("updated") val updated: String? = null,
    @SerializedName("linkedJiraSoftwareIssue") val linkedJiraSoftwareIssue: String? = null,
    @SerializedName("linkedJsmTicket") val linkedJsmTicket: String? = null
)

data class CreateJiraIssueRequest(
    @SerializedName("ticket_id") val ticketId: String
)

data class CreateJiraIssueResponse(
    @SerializedName("message") val message: String,
    @SerializedName("issueKey") val issueKey: String,
    @SerializedName("issueUrl") val issueUrl: String? = null,
    @SerializedName("jsmTicketKey") val jsmTicketKey: String? = null,
    @SerializedName("linked") val linked: Boolean? = null
)

