package com.project.lighthouse.data.api

import com.project.lighthouse.data.model.DashboardStatsResponse
import com.project.lighthouse.data.model.RecentItemsResponse
import retrofit2.Response
import retrofit2.http.GET

interface DashboardApiService {

    @GET("api/dashboard/stats")
    suspend fun getDashboardStats(): Response<DashboardStatsResponse>

    @GET("api/dashboard/recent")
    suspend fun getRecentItems(): Response<RecentItemsResponse>
}

