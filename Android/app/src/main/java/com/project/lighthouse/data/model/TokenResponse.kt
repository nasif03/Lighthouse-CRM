package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("user")
    val user: UserResponse
)

