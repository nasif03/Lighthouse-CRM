package com.project.lighthouse.ui.gmail

import com.project.lighthouse.data.model.GmailMessage

data class SendEmailFormState(
    val to: String = "",
    val subject: String = "",
    val body: String = "",
    val isSubmitting: Boolean = false
)

data class GmailState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val messages: List<GmailMessage> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showSendEmailDialog: Boolean = false,
    val sendEmailFormState: SendEmailFormState = SendEmailFormState(),
    val authorizationUrl: String? = null
)

