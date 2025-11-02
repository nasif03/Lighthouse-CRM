package com.project.lighthouse.authentication

data class SignInState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)