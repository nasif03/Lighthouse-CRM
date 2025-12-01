package com.project.lighthouse.ui.deals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.AccountsRepository
import com.project.lighthouse.data.repository.ContactsRepository
import com.project.lighthouse.data.repository.DealsRepository

class DealsViewModelFactory(
    private val dealsRepository: DealsRepository,
    private val accountsRepository: AccountsRepository,
    private val contactsRepository: ContactsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DealsViewModel::class.java)) {
            return DealsViewModel(dealsRepository, accountsRepository, contactsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

