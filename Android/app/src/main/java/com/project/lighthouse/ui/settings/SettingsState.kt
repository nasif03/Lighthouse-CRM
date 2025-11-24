package com.project.lighthouse.ui.settings

import com.project.lighthouse.data.model.OrganizationResponse
import com.project.lighthouse.data.model.TenantListResponse

data class SettingsState(
    val organizations: List<OrganizationResponse> = emptyList(),
    val tenants: TenantListResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val createOrgName: String = "",
    val createOrgDomain: String = "",
    val joinOrgEmail: String = "",
    val joinOrgName: String = "",
    val isSwitchingTenant: Boolean = false,
    val shouldRefreshAuth: Boolean = false
)

