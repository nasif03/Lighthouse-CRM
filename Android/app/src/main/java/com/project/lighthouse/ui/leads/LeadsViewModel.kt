package com.project.lighthouse.ui.leads

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.CreateLeadRequest
import com.project.lighthouse.data.repository.LeadsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LeadsViewModel(
    private val leadsRepository: LeadsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LeadsState(isLoading = true))
    val state: StateFlow<LeadsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refreshLeads(initial = true)
    }

    fun refreshLeads(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d(TAG, "refreshLeads initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = leadsRepository.getLeads()
            result.onSuccess { leads ->
                Log.d(TAG, "Leads loaded: ${leads.size} items")
                _state.update {
                    it.copy(
                        leads = leads,
                        isLoading = false,
                        isRefreshing = false,
                        actionInProgress = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load leads: ${error.message}", error)
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
        _state.update { it.copy(showCreateDialog = show, errorMessage = null, infoMessage = null) }
        if (!show) {
            _state.update { it.copy(formState = LeadFormState()) }
        }
    }

    fun updateForm(name: String? = null, email: String? = null, phone: String? = null, source: String? = null) {
        _state.update {
            it.copy(
                formState = it.formState.copy(
                    name = name ?: it.formState.name,
                    email = email ?: it.formState.email,
                    phone = phone ?: it.formState.phone,
                    source = source ?: it.formState.source
                )
            )
        }
    }

    fun createLead() {
        val currentForm = _state.value.formState
        if (currentForm.name.isBlank() || currentForm.email.isBlank()) {
            Log.w(TAG, "Create lead validation failed: name and email required")
            _state.update { it.copy(errorMessage = "Name and email are required") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Creating lead: ${currentForm.name}, email=${currentForm.email}")
            _state.update { it.copy(formState = it.formState.copy(isSubmitting = true)) }
            val request = CreateLeadRequest(
                name = currentForm.name.trim(),
                email = currentForm.email.trim(),
                phone = currentForm.phone.takeIf { it.isNotBlank() },
                source = currentForm.source,
                status = "new"
            )
            val result = leadsRepository.createLead(request)
            result.onSuccess { lead ->
                Log.d(TAG, "Lead created successfully: ${lead.id}")
                _state.update {
                    it.copy(
                        leads = listOf(lead) + it.leads,
                        formState = LeadFormState(),
                        showCreateDialog = false,
                        infoMessage = "Lead created successfully"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to create lead: ${error.message}", error)
                _state.update {
                    it.copy(
                        errorMessage = error.message,
                        formState = it.formState.copy(isSubmitting = false)
                    )
                }
            }
        }
    }

    fun updateLeadStatus(leadId: String, newStatus: String) {
        viewModelScope.launch {
            Log.d(TAG, "Updating lead status: $leadId -> $newStatus")
            _state.update { it.copy(actionInProgress = leadId) }
            val result = leadsRepository.updateLeadStatus(leadId, newStatus)
            result.onSuccess { updatedLead ->
                Log.d(TAG, "Lead status updated successfully: $leadId")
                _state.update {
                    it.copy(
                        leads = it.leads.map { lead -> if (lead.id == updatedLead.id) updatedLead else lead },
                        actionInProgress = null,
                        infoMessage = "Status updated"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to update lead status: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message, actionInProgress = null) }
            }
        }
    }

    fun convertLead(leadId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Converting lead: $leadId")
            _state.update { it.copy(actionInProgress = leadId) }
            val result = leadsRepository.convertLead(leadId)
            result.onSuccess {
                Log.d(TAG, "Lead converted successfully: $leadId")
                _state.update {
                    it.copy(
                        infoMessage = "Lead converted",
                        actionInProgress = null
                    )
                }
                refreshLeads()
            }.onFailure { error ->
                Log.e(TAG, "Failed to convert lead: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message, actionInProgress = null) }
            }
        }
    }

    fun deleteLead(leadId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting lead: $leadId")
            _state.update { it.copy(actionInProgress = leadId) }
            val result = leadsRepository.deleteLead(leadId)
            result.onSuccess {
                Log.d(TAG, "Lead deleted successfully: $leadId")
                _state.update {
                    it.copy(
                        leads = it.leads.filterNot { lead -> lead.id == leadId },
                        infoMessage = "Lead deleted",
                        actionInProgress = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to delete lead: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message, actionInProgress = null) }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "LeadsViewModel"
    }
}

