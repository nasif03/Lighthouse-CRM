package com.project.lighthouse.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.local.TokenManager
import com.project.lighthouse.data.model.TokenResponse
import com.project.lighthouse.data.model.UserResponse
import com.project.lighthouse.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        viewModelScope.launch {
            Log.d(TAG, "Checking auth state")
            _authState.update { it.copy(isLoading = true, error = null) }
            
            val token = tokenManager.getToken()
            if (token != null) {
                Log.d(TAG, "Token found, verifying with backend")
                // Token exists, verify it's still valid by getting current user
                val result = authRepository.getCurrentUser()
                result.onSuccess { user ->
                    Log.d(TAG, "Auth verified: user=${user.email}")
                    _authState.update {
                        it.copy(
                            user = user,
                            token = token,
                            isAuthenticated = true,
                            isLoading = false,
                            error = null
                        )
                    }
                }.onFailure { exception ->
                    Log.w(TAG, "Token invalid, clearing: ${exception.message}")
                    // Token is invalid, clear it
                    tokenManager.clearToken()
                    _authState.update {
                        it.copy(
                            user = null,
                            token = null,
                            isAuthenticated = false,
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
            } else {
                Log.d(TAG, "No token found, user not authenticated")
                _authState.update {
                    it.copy(
                        user = null,
                        token = null,
                        isAuthenticated = false,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun verifyTokenWithBackend(idToken: String) {
        viewModelScope.launch {
            Log.d(TAG, "Verifying token with backend")
            _authState.update { it.copy(isLoading = true, error = null) }
            
            val result = authRepository.verifyToken(idToken)
            result.onSuccess { tokenResponse ->
                Log.d(TAG, "Token verified successfully: user=${tokenResponse.user.email}")
                _authState.update {
                    it.copy(
                        user = tokenResponse.user,
                        token = tokenResponse.token,
                        isAuthenticated = true,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { exception ->
                Log.e(TAG, "Token verification failed: ${exception.message}", exception)
                _authState.update {
                    it.copy(
                        user = null,
                        token = null,
                        isAuthenticated = false,
                        isLoading = false,
                        error = exception.message ?: "Authentication failed"
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            Log.d(TAG, "Signing out")
            _authState.update { it.copy(isLoading = true) }
            
            authRepository.logout()
            
            Log.d(TAG, "Sign out complete")
            _authState.update {
                it.copy(
                    user = null,
                    token = null,
                    isAuthenticated = false,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

