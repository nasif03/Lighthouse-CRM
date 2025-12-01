package com.project.lighthouse.ui.leads

import com.project.lighthouse.data.model.LeadDto

data class LeadFormState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val source: String = "web",
    val status: String = "new",
    val isSubmitting: Boolean = false
)

data class ConvertedEntities(
    val accountId: String,
    val contactId: String,
    val dealId: String
)

data class LeadsState(
    val leads: List<LeadDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val formState: LeadFormState = LeadFormState(),
    val actionInProgress: String? = null,
    val lastConvertedEntities: ConvertedEntities? = null
)

