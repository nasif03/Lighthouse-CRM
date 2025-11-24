package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class DealDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("currency") val currency: String = "USD",
    @SerializedName("stageId") val stageId: String = "",
    @SerializedName("stageName") val stageName: String? = null,
    @SerializedName("status") val status: String = "",
    @SerializedName("accountId") val accountId: String? = null,
    @SerializedName("contactId") val contactId: String? = null,
    @SerializedName("ownerId") val ownerId: String = "",
    @SerializedName("orgId") val orgId: String = "",
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateDealRequest(
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("currency") val currency: String = "USD",
    @SerializedName("stageId") val stageId: String = "prospecting",
    @SerializedName("stageName") val stageName: String = "Prospecting"
)

data class UpdateDealStageRequest(
    @SerializedName("stageId") val stageId: String,
    @SerializedName("stageName") val stageName: String? = null
)

