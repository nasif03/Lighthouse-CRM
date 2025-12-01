package com.project.lighthouse.ui.contacts

import com.project.lighthouse.data.model.AccountDto
import com.project.lighthouse.data.model.ContactDto

data class ContactFormState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val title: String = "",
    val selectedAccountId: String? = null,
    val tags: List<String> = emptyList(),
    val isSubmitting: Boolean = false
)

data class ContactsState(
    val contacts: List<ContactDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingContactId: String? = null,
    val formState: ContactFormState = ContactFormState(),
    val actionInProgress: String? = null
)

