package com.project.lighthouse.authentication

import com.project.lighthouse.data.model.UserResponse

data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)

data class UserData(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?,
    val idToken: String?,
    // Backend user info
    val backendUserId: String? = null,
    val backendUser: UserResponse? = null
) {
    companion object {
        fun fromBackendUser(user: UserResponse, idToken: String?): UserData {
            return UserData(
                userId = user.id,
                username = user.name,
                profilePictureUrl = user.picture,
                idToken = idToken,
                backendUserId = user.id,
                backendUser = user
            )
        }
    }
}