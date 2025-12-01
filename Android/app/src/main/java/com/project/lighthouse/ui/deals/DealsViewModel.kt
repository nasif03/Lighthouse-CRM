package com.project.lighthouse.ui.deals

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.CreateDealRequest
import com.project.lighthouse.data.model.DealDto
import com.project.lighthouse.data.model.UpdateDealRequest
import com.project.lighthouse.data.repository.AccountsRepository
import com.project.lighthouse.data.repository.ContactsRepository
import com.project.lighthouse.data.repository.DealsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DealsViewModel(
    private val dealsRepository: DealsRepository,
    private val accountsRepository: AccountsRepository,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DealsState(isLoading = true))
    val state: StateFlow<DealsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refreshDeals(initial = true)
        loadAccounts()
        loadContacts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            Log.d(TAG, "Loading accounts for deal form")
            val result = accountsRepository.getAccounts()
            result.onSuccess { accounts ->
                Log.d(TAG, "Accounts loaded: ${accounts.size} items")
                _state.update { it.copy(accounts = accounts) }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load accounts: ${error.message}", error)
                // Don't show error to user, just log it - accounts are optional
            }
        }
    }

    private fun loadContacts() {
        viewModelScope.launch {
            Log.d(TAG, "Loading contacts for deal form")
            val result = contactsRepository.getContacts()
            result.onSuccess { contacts ->
                Log.d(TAG, "Contacts loaded: ${contacts.size} items")
                _state.update { it.copy(contacts = contacts) }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load contacts: ${error.message}", error)
                // Don't show error to user, just log it - contacts are optional
            }
        }
    }

    fun refreshDeals(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d(TAG, "refreshDeals initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = dealsRepository.getDeals()
            result.onSuccess { deals ->
                Log.d(TAG, "Deals loaded: ${deals.size} items")
                _state.update {
                    it.copy(
                        deals = deals,
                        isLoading = false,
                        isRefreshing = false,
                        actionInProgress = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load deals: ${error.message}", error)
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

    fun toggleCreateDialog(show: Boolean) {
        _state.update { it.copy(showCreateDialog = show) }
        if (!show) {
            _state.update { it.copy(formState = DealFormState()) }
        }
    }

    fun updateForm(
        name: String? = null,
        amount: String? = null,
        stageId: String? = null,
        selectedAccountId: String? = null,
        selectedContactId: String? = null
    ) {
        _state.update {
            it.copy(
                formState = it.formState.copy(
                    name = name ?: it.formState.name,
                    amount = amount ?: it.formState.amount,
                    stageId = stageId ?: it.formState.stageId,
                    selectedAccountId = selectedAccountId ?: it.formState.selectedAccountId,
                    selectedContactId = selectedContactId ?: it.formState.selectedContactId
                )
            )
        }
    }

    fun createDeal() {
        val form = _state.value.formState
        if (form.name.isBlank()) {
            Log.w(TAG, "Create deal validation failed: name is required")
            _state.update { it.copy(errorMessage = "Deal name is required") }
            return
        }
        if (form.selectedAccountId.isNullOrBlank()) {
            Log.w(TAG, "Create deal validation failed: account is required")
            _state.update { it.copy(errorMessage = "Account is required") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Creating deal: ${form.name}, amount=${form.amount}, stage=${form.stageId}")
            _state.update { it.copy(formState = it.formState.copy(isSubmitting = true)) }
            val request = CreateDealRequest(
                name = form.name.trim(),
                amount = form.amount.toDoubleOrNull(),
                stageId = form.stageId,
                stageName = form.stageId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                accountId = form.selectedAccountId?.takeIf { it.isNotBlank() },
                contactId = form.selectedContactId?.takeIf { it.isNotBlank() }
            )
            val result = dealsRepository.createDeal(request)
            result.onSuccess { deal ->
                Log.d(TAG, "Deal created successfully: ${deal.id}")
                _state.update {
                    it.copy(
                        deals = listOf(deal) + it.deals,
                        formState = DealFormState(),
                        showCreateDialog = false,
                        infoMessage = "Deal created"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to create deal: ${error.message}", error)
                _state.update {
                    it.copy(
                        errorMessage = error.message,
                        formState = it.formState.copy(isSubmitting = false)
                    )
                }
            }
        }
    }

    fun toggleEditDialog(show: Boolean, deal: DealDto? = null) {
        _state.update {
            it.copy(
                showEditDialog = show,
                editingDealId = deal?.id,
                editFormState = if (show && deal != null) {
                    DealFormState(
                        name = deal.name,
                        amount = deal.amount?.toString() ?: "",
                        stageId = deal.stageId,
                        selectedAccountId = deal.accountId,
                        selectedContactId = deal.contactId
                    )
                } else {
                    DealFormState()
                }
            )
        }
    }

    fun updateEditForm(
        name: String? = null,
        amount: String? = null,
        stageId: String? = null,
        selectedAccountId: String? = null,
        selectedContactId: String? = null
    ) {
        _state.update {
            it.copy(
                editFormState = it.editFormState.copy(
                    name = name ?: it.editFormState.name,
                    amount = amount ?: it.editFormState.amount,
                    stageId = stageId ?: it.editFormState.stageId,
                    selectedAccountId = selectedAccountId ?: it.editFormState.selectedAccountId,
                    selectedContactId = selectedContactId ?: it.editFormState.selectedContactId
                )
            )
        }
    }

    fun updateDeal() {
        val dealId = _state.value.editingDealId ?: return
        val form = _state.value.editFormState
        if (form.name.isBlank()) {
            Log.w(TAG, "Update deal validation failed: name is required")
            _state.update { it.copy(errorMessage = "Deal name is required") }
            return
        }
        if (form.selectedAccountId.isNullOrBlank()) {
            Log.w(TAG, "Update deal validation failed: account is required")
            _state.update { it.copy(errorMessage = "Account is required") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Updating deal: $dealId")
            _state.update { it.copy(editFormState = it.editFormState.copy(isSubmitting = true)) }
            val deal = _state.value.deals.find { it.id == dealId }
            val request = UpdateDealRequest(
                name = form.name.trim(),
                amount = form.amount.toDoubleOrNull(),
                currency = deal?.currency ?: "USD",
                stageId = form.stageId,
                stageName = form.stageId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                status = deal?.status,
                accountId = form.selectedAccountId?.takeIf { it.isNotBlank() },
                contactId = form.selectedContactId?.takeIf { it.isNotBlank() }
            )
            val result = dealsRepository.updateDeal(dealId, request)
            result.onSuccess { updatedDeal ->
                Log.d(TAG, "Deal updated successfully: $dealId")
                _state.update {
                    it.copy(
                        deals = it.deals.map { d -> if (d.id == dealId) updatedDeal else d },
                        editFormState = DealFormState(),
                        showEditDialog = false,
                        editingDealId = null,
                        infoMessage = "Deal updated"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to update deal: ${error.message}", error)
                _state.update {
                    it.copy(
                        errorMessage = error.message,
                        editFormState = it.editFormState.copy(isSubmitting = false)
                    )
                }
            }
        }
    }

    fun updateDealStage(dealId: String, stageId: String, stageName: String?) {
        viewModelScope.launch {
            Log.d(TAG, "Updating deal stage: $dealId -> $stageId")
            _state.update { it.copy(actionInProgress = dealId) }
            val result = dealsRepository.updateDealStage(dealId, stageId, stageName)
            result.onSuccess { updatedDeal ->
                Log.d(TAG, "Deal stage updated successfully: $dealId")
                _state.update {
                    it.copy(
                        deals = it.deals.map { deal -> if (deal.id == updatedDeal.id) updatedDeal else deal },
                        infoMessage = "Stage updated",
                        actionInProgress = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to update deal stage: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message, actionInProgress = null) }
            }
        }
    }

    fun deleteDeal(dealId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting deal: $dealId")
            _state.update { it.copy(actionInProgress = dealId) }
            val result = dealsRepository.deleteDeal(dealId)
            result.onSuccess {
                Log.d(TAG, "Deal deleted successfully: $dealId")
                _state.update {
                    it.copy(
                        deals = it.deals.filterNot { deal -> deal.id == dealId },
                        infoMessage = "Deal deleted",
                        actionInProgress = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to delete deal: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message, actionInProgress = null) }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "DealsViewModel"
    }
}

