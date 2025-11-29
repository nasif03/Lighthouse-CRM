package com.project.lighthouse.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.AccountsRepository

class AccountDetailViewModelFactory(
    private val accountsRepository: AccountsRepository,
    private val accountId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountDetailViewModel::class.java)) {
            return AccountDetailViewModel(accountsRepository, accountId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

