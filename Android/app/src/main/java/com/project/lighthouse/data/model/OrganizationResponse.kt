package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class OrganizationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("domain") val domain: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateOrganizationRequest(
    @SerializedName("name") val name: String,
    @SerializedName("domain") val domain: String? = null
)

data class JoinOrganizationRequest(
    @SerializedName("email") val email: String,
    @SerializedName("organizationName") val organizationName: String
)

data class TenantResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

data class TenantListResponse(
    @SerializedName("tenants") val tenants: List<TenantResponse>,
    @SerializedName("activeTenantId") val activeTenantId: String? = null
)

data class SwitchTenantRequest(
    @SerializedName("tenant_id") val tenantId: String
)

