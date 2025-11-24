package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class AccountDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("domain") val domain: String? = null,
    @SerializedName("industry") val industry: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("status") val status: String = "",
    @SerializedName("ownerId") val ownerId: String = "",
    @SerializedName("orgId") val orgId: String = "",
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateAccountRequest(
    @SerializedName("name") val name: String,
    @SerializedName("domain") val domain: String? = null,
    @SerializedName("industry") val industry: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("status") val status: String = "active"
)

data class UpdateAccountRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("domain") val domain: String? = null,
    @SerializedName("industry") val industry: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("status") val status: String? = null
)

