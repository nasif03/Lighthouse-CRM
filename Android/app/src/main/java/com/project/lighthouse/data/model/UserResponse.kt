package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("picture")
    val picture: String? = null,
    @SerializedName("orgId")
    val orgId: Any? = null // Can be String or List<String> from backend
)

