package com.project.lighthouse.ui.dashboard

import com.project.lighthouse.data.model.DashboardStatsResponse
import com.project.lighthouse.data.model.RecentItemsResponse

data class DashboardState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val stats: DashboardStatsResponse = DashboardStatsResponse(),
    val recentItems: RecentItemsResponse = RecentItemsResponse(),
    val errorMessage: String? = null
)

