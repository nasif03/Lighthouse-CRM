package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class RoleDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("permissions") val permissions: List<String> = emptyList(),
    @SerializedName("orgId") val orgId: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateRoleRequest(
    @SerializedName("name") val name: String,
    @SerializedName("permissions") val permissions: List<String> = emptyList()
)

data class UpdateRoleRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("permissions") val permissions: List<String>? = null
)

