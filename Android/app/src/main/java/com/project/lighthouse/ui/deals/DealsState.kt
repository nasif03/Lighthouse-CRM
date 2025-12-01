package com.project.lighthouse.ui.deals

import com.project.lighthouse.data.model.AccountDto
import com.project.lighthouse.data.model.ContactDto
import com.project.lighthouse.data.model.DealDto

data class DealFormState(
    val name: String = "",
    val amount: String = "",
    val stageId: String = "prospecting",
    val selectedAccountId: String? = null,
    val selectedContactId: String? = null,
    val isSubmitting: Boolean = false
)

data class DealsState(
    val deals: List<DealDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val contacts: List<ContactDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingDealId: String? = null,
    val formState: DealFormState = DealFormState(),
    val editFormState: DealFormState = DealFormState(),
    val actionInProgress: String? = null
)

