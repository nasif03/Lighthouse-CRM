package com.project.lighthouse.ui.administration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.repository.EmployeesRepository
import com.project.lighthouse.data.repository.OrganizationRepository
import com.project.lighthouse.data.repository.RolesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdministrationViewModel(
    private val employeesRepository: EmployeesRepository,
    private val rolesRepository: RolesRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdministrationState(isLoadingOrganizations = true))
    val state: StateFlow<AdministrationState> = _state.asStateFlow()

    init {
        loadOrganizations()
    }
    
    fun loadOrganizations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingOrganizations = true, errorMessage = null) }
            val result = organizationRepository.getOrganizations()
            result.onSuccess { organizations ->
                Log.d(TAG, "Loaded ${organizations.size} organizations")
                val selectedOrgId = when {
                    organizations.isEmpty() -> {
                        _state.update { 
                            it.copy(
                                organizations = emptyList(),
                                isLoadingOrganizations = false,
                                uiState = AdministrationUiState.Error("No organizations found. Please create or join an organization first.")
                            )
                        }
                        return@launch
                    }
                    organizations.size == 1 -> {
                        // Auto-select if only one organization
                        organizations.first().id
                    }
                    else -> {
                        // Multiple organizations - user needs to select
                        null
                    }
                }
                
                _state.update { 
                    it.copy(
                        organizations = organizations,
                        selectedOrgId = selectedOrgId,
                        isLoadingOrganizations = false
                    )
                }
                
                // Load data if organization is selected
                selectedOrgId?.let { loadDataForOrganization(it) }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load organizations: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoadingOrganizations = false,
                        uiState = AdministrationUiState.Error(error.message ?: "Failed to load organizations")
                    )
                }
            }
        }
    }
    
    fun selectOrganization(orgId: String) {
        _state.update { it.copy(selectedOrgId = orgId) }
        loadDataForOrganization(orgId)
    }
    
    private fun loadDataForOrganization(orgId: String) {
        _state.update { it.copy(uiState = AdministrationUiState.Loading) }
        loadEmployees(orgId)
        loadRoles(orgId)
    }

    private fun loadEmployees(orgId: String) {
        viewModelScope.launch {
            val result = employeesRepository.getEmployees(orgId)
            result.onSuccess { employees ->
                Log.d(TAG, "Loaded ${employees.size} employees")
                val currentState = _state.value
                val roles = currentState.roles
                _state.update { 
                    it.copy(
                        employees = employees,
                        uiState = when {
                            employees.isEmpty() && roles.isEmpty() -> AdministrationUiState.Empty
                            else -> AdministrationUiState.Success(employees, roles)
                        }
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load employees: ${error.message}", error)
                // Check if it's a 403 (access denied)
                val isAccessDenied = error.message?.contains("403") == true || 
                                    error.message?.contains("admin") == true ||
                                    error.message?.contains("permission") == true
                _state.update {
                    it.copy(
                        uiState = AdministrationUiState.Error(
                            if (isAccessDenied) {
                                "Only organization admins can view employees and roles."
                            } else {
                                error.message ?: "Failed to load employees"
                            }
                        )
                    )
                }
            }
        }
    }

    private fun loadRoles(orgId: String) {
        viewModelScope.launch {
            val result = rolesRepository.getRoles(orgId)
            result.onSuccess { roles ->
                Log.d(TAG, "Loaded ${roles.size} roles")
                val currentState = _state.value
                val employees = currentState.employees
                _state.update { 
                    it.copy(
                        roles = roles,
                        uiState = when {
                            employees.isEmpty() && roles.isEmpty() -> AdministrationUiState.Empty
                            else -> AdministrationUiState.Success(employees, roles)
                        }
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load roles: ${error.message}", error)
                // Check if it's a 403 (access denied)
                val isAccessDenied = error.message?.contains("403") == true || 
                                    error.message?.contains("admin") == true ||
                                    error.message?.contains("permission") == true
                _state.update {
                    it.copy(
                        uiState = AdministrationUiState.Error(
                            if (isAccessDenied) {
                                "Only organization admins can view employees and roles."
                            } else {
                                error.message ?: "Failed to load roles"
                            }
                        )
                    )
                }
            }
        }
    }

    fun updateNewEmployeeName(name: String) {
        _state.update { it.copy(newEmployeeName = name) }
    }

    fun updateNewEmployeeEmail(email: String) {
        _state.update { it.copy(newEmployeeEmail = email) }
    }

    fun toggleRoleSelection(roleId: String) {
        val current = _state.value.selectedRoleIds
        _state.update {
            it.copy(
                selectedRoleIds = if (current.contains(roleId)) {
                    current.filter { it != roleId }
                } else {
                    current + roleId
                }
            )
        }
    }

    fun addEmployee() {
        val current = _state.value
        val orgId = current.selectedOrgId
        if (orgId == null) {
            _state.update { it.copy(errorMessage = "Please select an organization first") }
            return
        }
        if (current.newEmployeeName.isBlank() || current.newEmployeeEmail.isBlank()) {
            _state.update { it.copy(errorMessage = "Name and email are required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isAddingEmployee = true, errorMessage = null) }
            val result = employeesRepository.createEmployee(
                orgId = orgId,
                name = current.newEmployeeName.trim(),
                email = current.newEmployeeEmail.trim(),
                roleIds = current.selectedRoleIds
            )
            result.onSuccess { employee ->
                Log.d(TAG, "Employee created: ${employee.id}")
                _state.update {
                    it.copy(
                        employees = it.employees + employee,
                        isAddingEmployee = false,
                        newEmployeeName = "",
                        newEmployeeEmail = "",
                        selectedRoleIds = emptyList(),
                        infoMessage = "Employee added successfully"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to create employee: ${error.message}", error)
                _state.update {
                    it.copy(
                        isAddingEmployee = false,
                        errorMessage = error.message ?: "Failed to add employee"
                    )
                }
            }
        }
    }

    fun startEditingEmployee(employeeId: String) {
        val employee = _state.value.employees.find { it.id == employeeId }
        _state.update {
            it.copy(
                editingEmployeeId = employeeId,
                editingEmployeeRoles = employee?.roleIds ?: emptyList()
            )
        }
    }

    fun cancelEditingEmployee() {
        _state.update { it.copy(editingEmployeeId = null, editingEmployeeRoles = emptyList()) }
    }

    fun toggleEditingRoleSelection(roleId: String) {
        val current = _state.value.editingEmployeeRoles
        _state.update {
            it.copy(
                editingEmployeeRoles = if (current.contains(roleId)) {
                    current.filter { it != roleId }
                } else {
                    current + roleId
                }
            )
        }
    }

    fun updateEmployeeRoles() {
        val current = _state.value
        val employeeId = current.editingEmployeeId ?: return
        val orgId = current.selectedOrgId
        if (orgId == null) {
            _state.update { it.copy(errorMessage = "Please select an organization first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            val result = employeesRepository.updateEmployee(
                orgId = orgId,
                employeeId = employeeId,
                roleIds = current.editingEmployeeRoles
            )
            result.onSuccess { updatedEmployee ->
                Log.d(TAG, "Employee roles updated: $employeeId")
                _state.update {
                    it.copy(
                        employees = it.employees.map { if (it.id == employeeId) updatedEmployee else it },
                        editingEmployeeId = null,
                        editingEmployeeRoles = emptyList(),
                        infoMessage = "Employee roles updated"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to update employee roles: ${error.message}", error)
                _state.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to update employee roles"
                    )
                }
            }
        }
    }

    fun updateNewRoleName(name: String) {
        _state.update { it.copy(newRoleName = name) }
    }

    fun togglePermission(permission: String) {
        val current = _state.value.newRolePermissions
        _state.update {
            it.copy(
                newRolePermissions = if (current.contains(permission)) {
                    current.filter { it != permission }
                } else {
                    current + permission
                }
            )
        }
    }

    fun addRole() {
        val current = _state.value
        val orgId = current.selectedOrgId
        if (orgId == null) {
            _state.update { it.copy(errorMessage = "Please select an organization first") }
            return
        }
        if (current.newRoleName.isBlank()) {
            _state.update { it.copy(errorMessage = "Role name is required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isAddingRole = true, errorMessage = null) }
            val result = rolesRepository.createRole(
                orgId = orgId,
                name = current.newRoleName.trim(),
                permissions = current.newRolePermissions
            )
            result.onSuccess { role ->
                Log.d(TAG, "Role created: ${role.id}")
                _state.update {
                    it.copy(
                        roles = it.roles + role,
                        isAddingRole = false,
                        newRoleName = "",
                        newRolePermissions = emptyList(),
                        infoMessage = "Role created successfully"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to create role: ${error.message}", error)
                _state.update {
                    it.copy(
                        isAddingRole = false,
                        errorMessage = error.message ?: "Failed to create role"
                    )
                }
            }
        }
    }

    fun deleteRole(roleId: String) {
        val orgId = _state.value.selectedOrgId
        if (orgId == null) {
            _state.update { it.copy(errorMessage = "Please select an organization first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            val result = rolesRepository.deleteRole(orgId, roleId)
            result.onSuccess {
                Log.d(TAG, "Role deleted: $roleId")
                _state.update {
                    it.copy(
                        roles = it.roles.filter { it.id != roleId },
                        employees = it.employees.map { emp ->
                            emp.copy(roleIds = emp.roleIds.filter { it != roleId })
                        },
                        infoMessage = "Role deleted successfully"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to delete role: ${error.message}", error)
                _state.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to delete role"
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "AdministrationViewModel"
    }
}

