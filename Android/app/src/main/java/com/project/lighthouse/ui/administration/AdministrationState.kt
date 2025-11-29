package com.project.lighthouse.ui.administration

import com.project.lighthouse.data.model.EmployeeDto
import com.project.lighthouse.data.model.RoleDto

data class AdministrationState(
    val employees: List<EmployeeDto> = emptyList(),
    val roles: List<RoleDto> = emptyList(),
    val isLoadingEmployees: Boolean = false,
    val isLoadingRoles: Boolean = false,
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

