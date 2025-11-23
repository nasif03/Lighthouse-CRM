package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class RecentLead(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("status")
    val status: String = "",
    @SerializedName("source")
    val source: String = "",
    @SerializedName("createdAt")
    val createdAt: String = ""
)

data class RecentDeal(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("amount")
    val amount: Double? = null,
    @SerializedName("currency")
    val currency: String = "USD",
    @SerializedName("stageId")
    val stageId: String = "",
    @SerializedName("stageName")
    val stageName: String = "",
    @SerializedName("createdAt")
    val createdAt: String = ""
)

data class RecentContact(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("createdAt")
    val createdAt: String = ""
)

data class RecentItemsResponse(
    @SerializedName("recentLeads")
    val recentLeads: List<RecentLead> = emptyList(),
    @SerializedName("recentDeals")
    val recentDeals: List<RecentDeal> = emptyList(),
    @SerializedName("recentContacts")
    val recentContacts: List<RecentContact> = emptyList()
)

