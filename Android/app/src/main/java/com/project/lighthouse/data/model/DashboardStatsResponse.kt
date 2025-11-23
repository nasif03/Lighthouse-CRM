package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class DashboardStatsResponse(
    @SerializedName("summary")
    val summary: DashboardSummary = DashboardSummary(),
    @SerializedName("leadsByStatus")
    val leadsByStatus: Map<String, Int> = emptyMap(),
    @SerializedName("dealsByStage")
    val dealsByStage: Map<String, Int> = emptyMap()
)

