package com.project.lighthouse.ui.contacts

import com.project.lighthouse.data.model.ContactDto

data class ContactFormState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val title: String = "",
    val isSubmitting: Boolean = false
)

data class ContactsState(
    val contacts: List<ContactDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val formState: ContactFormState = ContactFormState(),
    val actionInProgress: String? = null
)

