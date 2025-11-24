package com.project.lighthouse.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.ui.auth.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    private val authViewModel: AuthViewModel
): ViewModel() {
    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    init {
        // Observe auth state changes
        viewModelScope.launch {
            authViewModel.authState.collect { authState ->
                _state.update {
                    it.copy(
                        isSignInSuccessful = authState.isAuthenticated,
                        signInError = authState.error,
                        isLoading = authState.isLoading
                    )
                }
            }
        }
    }

    fun onSignInResult(result: SignInResult) {
        if (result.data != null && result.data.idToken != null) {
            // Firebase sign-in successful, now verify with backend
            _state.update { it.copy(isLoading = true, signInError = null) }
            authViewModel.verifyTokenWithBackend(result.data.idToken)
        } else {
            // Firebase sign-in failed
            _state.update {
                it.copy(
                    isSignInSuccessful = false,
                    signInError = result.errorMessage ?: "Sign in failed",
                    isLoading = false
                )
            }
        }
    }

    fun resetState() {
        _state.update { SignInState() }
    }
}