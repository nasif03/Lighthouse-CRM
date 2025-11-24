package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class LeadDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("status") val status: String = "new",
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("ownerId") val ownerId: String = "",
    @SerializedName("orgId") val orgId: String = "",
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateLeadRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("source") val source: String = "web",
    @SerializedName("status") val status: String = "new",
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null
)

data class UpdateLeadStatusRequest(
    @SerializedName("status") val status: String
)

data class ConvertLeadResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("leadId") val leadId: String = "",
    @SerializedName("accountId") val accountId: String = "",
    @SerializedName("contactId") val contactId: String = "",
    @SerializedName("dealId") val dealId: String = ""
)

