package com.project.lighthouse.ui.deals

import com.project.lighthouse.data.model.DealDto

data class DealFormState(
    val name: String = "",
    val amount: String = "",
    val stageId: String = "prospecting",
    val isSubmitting: Boolean = false
)

data class DealsState(
    val deals: List<DealDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val formState: DealFormState = DealFormState(),
    val actionInProgress: String? = null
)

