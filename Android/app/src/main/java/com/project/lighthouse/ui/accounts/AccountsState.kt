package com.project.lighthouse.ui.accounts

import com.project.lighthouse.data.model.AccountDto

data class AccountFormState(
    val name: String = "",
    val domain: String = "",
    val industry: String = "",
    val phone: String = "",
    val status: String = "active",
    val isSubmitting: Boolean = false,
    val editingAccountId: String? = null
)

data class AccountsState(
    val accounts: List<AccountDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val formState: AccountFormState = AccountFormState(),
    val actionInProgress: String? = null
)

