package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class ContactDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("email") val email: String = "",
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("accountId") val accountId: String? = null,
    @SerializedName("ownerId") val ownerId: String = "",
    @SerializedName("orgId") val orgId: String = "",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateContactRequest(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("title") val title: String? = null
)

