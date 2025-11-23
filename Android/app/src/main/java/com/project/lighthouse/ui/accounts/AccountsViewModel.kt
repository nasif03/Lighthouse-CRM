package com.project.lighthouse.ui.accounts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.CreateAccountRequest
import com.project.lighthouse.data.model.UpdateAccountRequest
import com.project.lighthouse.data.repository.AccountsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val accountsRepository: AccountsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountsState(isLoading = true))
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refreshAccounts(initial = true)
    }

    fun refreshAccounts(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d(TAG, "refreshAccounts initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = accountsRepository.getAccounts()
            result.onSuccess { accounts ->
                Log.d(TAG, "Accounts loaded: ${accounts.size} items")
                _state.update {
                    it.copy(
                        accounts = accounts,
                        isLoading = false,
                        isRefreshing = false,
                        actionInProgress = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load accounts: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message,
                        actionInProgress = null
                    )
                }
            }
        }
    }

    fun toggleDialog(show: Boolean, accountId: String? = null) {
        _state.update {
            it.copy(
                showCreateDialog = show,
                formState = if (show && accountId != null) {
                    val existing = it.accounts.find { acc -> acc.id == accountId }
                    if (existing != null) {
                        AccountFormState(
                            name = existing.name,
                            domain = existing.domain.orEmpty(),
                            industry = existing.industry.orEmpty(),
                            phone = existing.phone.orEmpty(),
                            status = existing.status,
                            editingAccountId = existing.id
                        )
                    } else AccountFormState()
                } else AccountFormState()
            )
        }
    }

    fun updateForm(
        name: String? = null,
        domain: String? = null,
        industry: String? = null,
        phone: String? = null,
        status: String? = null
    ) {
        _state.update {
            it.copy(
                formState = it.formState.copy(
                    name = name ?: it.formState.name,
                    domain = domain ?: it.formState.domain,
                    industry = industry ?: it.formState.industry,
                    phone = phone ?: it.formState.phone,
                    status = status ?: it.formState.status
                )
            )
        }
    }

    fun submitForm() {
        val form = _state.value.formState
        if (form.name.isBlank()) {
            Log.w(TAG, "Submit form validation failed: name is required")
            _state.update { it.copy(errorMessage = "Account name is required") }
            return
        }

        viewModelScope.launch {
            val editingId = form.editingAccountId
            Log.d(TAG, if (editingId == null) "Creating account: ${form.name}" else "Updating account: $editingId")
            _state.update { it.copy(formState = it.formState.copy(isSubmitting = true)) }
            val result = if (editingId == null) {
                accountsRepository.createAccount(
                    CreateAccountRequest(
                        name = form.name.trim(),
                        domain = form.domain.takeIf { it.isNotBlank() }?.trim(),
                        industry = form.industry.takeIf { it.isNotBlank() }?.trim(),
                        phone = form.phone.takeIf { it.isNotBlank() }?.trim(),
                        status = form.status
                    )
                )
            } else {
                accountsRepository.updateAccount(
                    editingId,
                    UpdateAccountRequest(
                        name = form.name.trim(),
                        domain = form.domain.takeIf { it.isNotBlank() }?.trim(),
                        industry = form.industry.takeIf { it.isNotBlank() }?.trim(),
                        phone = form.phone.takeIf { it.isNotBlank() }?.trim(),
                        status = form.status
                    )
                )
            }

            result.onSuccess { account ->
                Log.d(TAG, if (editingId == null) "Account created successfully: ${account.id}" else "Account updated successfully: ${account.id}")
                _state.update {
                    val updatedList = if (editingId == null) {
                        listOf(account) + it.accounts
                    } else {
                        it.accounts.map { acc -> if (acc.id == account.id) account else acc }
                    }
                    it.copy(
                        accounts = updatedList,
                        infoMessage = if (editingId == null) "Account created" else "Account updated",
                        showCreateDialog = false,
                        formState = AccountFormState()
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to ${if (editingId == null) "create" else "update"} account: ${error.message}", error)
                _state.update {
                    it.copy(
                        errorMessage = error.message,
                        formState = it.formState.copy(isSubmitting = false)
                    )
                }
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting account: $accountId")
            _state.update { it.copy(actionInProgress = accountId) }
            val result = accountsRepository.deleteAccount(accountId)
            result.onSuccess {
                Log.d(TAG, "Account deleted successfully: $accountId")
                _state.update {
                    it.copy(
                        accounts = it.accounts.filterNot { acc -> acc.id == accountId },
                        infoMessage = "Account deleted",
                        actionInProgress = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to delete account: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message, actionInProgress = null) }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "AccountsViewModel"
    }
}

