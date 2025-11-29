package com.project.lighthouse.ui.administration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.EmployeesRepository
import com.project.lighthouse.data.repository.RolesRepository

class AdministrationViewModelFactory(
    private val employeesRepository: EmployeesRepository,
    private val rolesRepository: RolesRepository,
    private val orgId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdministrationViewModel::class.java)) {
            return AdministrationViewModel(employeesRepository, rolesRepository, orgId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

