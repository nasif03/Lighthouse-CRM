package com.project.lighthouse.ui.accounts

import com.project.lighthouse.data.model.AccountContactDto
import com.project.lighthouse.data.model.AccountDealDto
import com.project.lighthouse.data.model.AccountDto

data class AccountDetailState(
    val account: AccountDto? = null,
    val contacts: List<AccountContactDto> = emptyList(),
    val deals: List<AccountDealDto> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: Int = 0 // 0 = Contacts, 1 = Deals
)

