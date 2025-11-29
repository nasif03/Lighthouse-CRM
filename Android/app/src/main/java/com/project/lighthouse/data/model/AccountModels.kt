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

data class AccountDetailsResponse(
    @SerializedName("account") val account: AccountDto,
    @SerializedName("contacts") val contacts: List<AccountContactDto>,
    @SerializedName("deals") val deals: List<AccountDealDto>
)

data class AccountContactDto(
    @SerializedName("id") val id: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("email") val email: String
)

data class AccountDealDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("status") val status: String
)

