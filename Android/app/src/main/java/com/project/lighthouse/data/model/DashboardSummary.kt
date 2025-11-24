package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class DashboardSummary(
    @SerializedName("totalLeads")
    val totalLeads: Int = 0,
    @SerializedName("totalContacts")
    val totalContacts: Int = 0,
    @SerializedName("totalDeals")
    val totalDeals: Int = 0,
    @SerializedName("totalAccounts")
    val totalAccounts: Int = 0,
    @SerializedName("recentLeads")
    val recentLeads: Int = 0,
    @SerializedName("recentContacts")
    val recentContacts: Int = 0,
    @SerializedName("recentDeals")
    val recentDeals: Int = 0,
    @SerializedName("recentActivities")
    val recentActivities: Int = 0,
    @SerializedName("totalDealValue")
    val totalDealValue: Double = 0.0,
    @SerializedName("wonDealValue")
    val wonDealValue: Double = 0.0,
    @SerializedName("conversionRate")
    val conversionRate: Double = 0.0
)

