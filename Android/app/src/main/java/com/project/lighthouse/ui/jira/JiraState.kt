package com.project.lighthouse.ui.jira

import com.project.lighthouse.data.model.JiraIssue

data class JiraState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val issues: List<JiraIssue> = emptyList(),
    val projectType: String = "jsm", // "jsm" or "software"
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateProjectDialog: Boolean = false,
    val showCreateIssueDialog: Boolean = false,
    val selectedTicketId: String? = null
)

