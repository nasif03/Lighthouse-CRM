package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class EmployeeDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("picture") val picture: String? = null,
    @SerializedName("roleIds") val roleIds: List<String> = emptyList(),
    @SerializedName("isAdmin") val isAdmin: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class CreateEmployeeRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("roleIds") val roleIds: List<String> = emptyList()
)

data class UpdateEmployeeRequest(
    @SerializedName("roleIds") val roleIds: List<String>? = null
)

