package com.project.lighthouse.ui.administration

import com.project.lighthouse.data.model.EmployeeDto
import com.project.lighthouse.data.model.OrganizationResponse
import com.project.lighthouse.data.model.RoleDto

sealed class AdministrationUiState {
    data object Loading : AdministrationUiState()
    data class Success(val employees: List<EmployeeDto>, val roles: List<RoleDto>) : AdministrationUiState()
    data object Empty : AdministrationUiState()
    data class Error(val message: String) : AdministrationUiState()
}

data class AdministrationState(
    // Organization selection
    val organizations: List<OrganizationResponse> = emptyList(),
    val selectedOrgId: String? = null,
    val isLoadingOrganizations: Boolean = false,
    
    // UI State
    val uiState: AdministrationUiState = AdministrationUiState.Loading,
    
    // Data
    val employees: List<EmployeeDto> = emptyList(),
    val roles: List<RoleDto> = emptyList(),
    
    // Messages
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    
    // Employee management
    val isAddingEmployee: Boolean = false,
    val newEmployeeName: String = "",
    val newEmployeeEmail: String = "",
    val selectedRoleIds: List<String> = emptyList(),
    val editingEmployeeId: String? = null,
    val editingEmployeeRoles: List<String> = emptyList(),
    
    // Role management
    val isAddingRole: Boolean = false,
    val newRoleName: String = "",
    val newRolePermissions: List<String> = emptyList()
)

val availablePermissions = listOf(
    "read:leads",
    "write:leads",
    "read:contacts",
    "write:contacts",
    "read:deals",
    "write:deals",
    "read:accounts",
    "write:accounts",
    "read:campaigns",
    "write:campaigns",
    "read:tickets",
    "write:tickets",
    "admin:users",
    "admin:roles"
)

