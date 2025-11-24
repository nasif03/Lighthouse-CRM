package com.project.lighthouse.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.lighthouse.data.repository.AuthRepository
import com.project.lighthouse.data.local.TokenManager

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

