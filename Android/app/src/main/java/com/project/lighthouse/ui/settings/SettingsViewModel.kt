package com.project.lighthouse.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.CreateOrganizationRequest
import com.project.lighthouse.data.model.JoinOrganizationRequest
import com.project.lighthouse.data.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(isLoading = true))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            Log.d(TAG, "Refreshing organizations and tenants data")
            _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val orgsResult = organizationRepository.getOrganizations()
            val tenantsResult = organizationRepository.getTenants()

            val organizations = orgsResult.getOrElse {
                Log.e(TAG, "Failed to load organizations: ${it.message}", it)
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = it.message
                    )
                }
                return@launch
            }

            val tenants = tenantsResult.getOrElse {
                Log.e(TAG, "Failed to load tenants: ${it.message}", it)
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = it.message
                    )
                }
                return@launch
            }

            Log.d(TAG, "Data refreshed: ${organizations.size} orgs, ${tenants.tenants.size} tenants")
            _state.update {
                it.copy(
                    organizations = organizations,
                    tenants = tenants,
                    isLoading = false
                )
            }
        }
    }

    fun updateCreateOrgForm(name: String? = null, domain: String? = null) {
        _state.update {
            it.copy(
                createOrgName = name ?: it.createOrgName,
                createOrgDomain = domain ?: it.createOrgDomain
            )
        }
    }

    fun updateJoinOrgForm(email: String? = null, name: String? = null) {
        _state.update {
            it.copy(
                joinOrgEmail = email ?: it.joinOrgEmail,
                joinOrgName = name ?: it.joinOrgName
            )
        }
    }

    fun createOrganization() {
        val name = _state.value.createOrgName.trim()
        if (name.isBlank()) {
            Log.w(TAG, "Create org validation failed: name is required")
            _state.update { it.copy(errorMessage = "Organization name is required") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Creating organization: $name")
            val result = organizationRepository.createOrganization(
                CreateOrganizationRequest(
                    name = name,
                    domain = _state.value.createOrgDomain.takeIf { it.isNotBlank() }?.trim()
                )
            )
            result.onSuccess {
                Log.d(TAG, "Organization created successfully")
                _state.update {
                    it.copy(
                        createOrgName = "",
                        createOrgDomain = "",
                        infoMessage = "Organization created"
                    )
                }
                refreshData()
            }.onFailure { error ->
                Log.e(TAG, "Failed to create organization: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun joinOrganization() {
        val email = _state.value.joinOrgEmail.trim()
        val orgName = _state.value.joinOrgName.trim()
        if (email.isBlank() || orgName.isBlank()) {
            Log.w(TAG, "Join org validation failed: email and name required")
            _state.update { it.copy(errorMessage = "Email and organization name are required") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Joining organization: $orgName with email: $email")
            val result = organizationRepository.joinOrganization(
                JoinOrganizationRequest(email = email, organizationName = orgName)
            )
            result.onSuccess {
                Log.d(TAG, "Join request successful")
                _state.update {
                    it.copy(
                        joinOrgEmail = "",
                        joinOrgName = "",
                        infoMessage = "Join request successful"
                    )
                }
                refreshData()
            }.onFailure { error ->
                Log.e(TAG, "Failed to join organization: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun switchTenant(tenantId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Switching tenant: $tenantId")
            _state.update { it.copy(isSwitchingTenant = true) }
            val result = organizationRepository.switchTenant(tenantId)
            result.onSuccess {
                Log.d(TAG, "Tenant switched successfully: $tenantId")
                _state.update {
                    it.copy(
                        isSwitchingTenant = false,
                        infoMessage = "Tenant switched",
                        shouldRefreshAuth = true
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to switch tenant: ${error.message}", error)
                _state.update {
                    it.copy(
                        isSwitchingTenant = false,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    fun clearAuthRefreshFlag() {
        _state.update { it.copy(shouldRefreshAuth = false) }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}

