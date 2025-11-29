package com.project.lighthouse.ui.accounts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.repository.AccountsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountDetailViewModel(
    private val accountsRepository: AccountsRepository,
    private val accountId: String
) : ViewModel() {

    private val _state = MutableStateFlow(AccountDetailState(isLoading = true))
    val state: StateFlow<AccountDetailState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadAccountDetails(initial = true)
    }

    fun loadAccountDetails(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d(TAG, "loadAccountDetails initial=$initial, accountId=$accountId")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null
                )
            }
            val result = accountsRepository.getAccountDetails(accountId)
            result.onSuccess { details ->
                Log.d(TAG, "Account details loaded: ${details.account.name}, ${details.contacts.size} contacts, ${details.deals.size} deals")
                _state.update {
                    it.copy(
                        account = details.account,
                        contacts = details.contacts,
                        deals = details.deals,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load account details: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _state.update { it.copy(selectedTab = tabIndex) }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "AccountDetailViewModel"
    }
}

