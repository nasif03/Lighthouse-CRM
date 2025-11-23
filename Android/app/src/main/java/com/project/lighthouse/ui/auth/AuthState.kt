package com.project.lighthouse.ui.auth

import com.project.lighthouse.data.model.UserResponse

data class AuthState(
    val user: UserResponse? = null,
    val token: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

