package com.project.lighthouse.ui.fireflies

import com.project.lighthouse.data.model.FirefliesTranscript

data class FirefliesState(
    val transcripts: List<FirefliesTranscript> = emptyList(),
    val selectedTranscript: FirefliesTranscript? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val lastFetchedAt: Long? = null,
    val lastSyncedAt: Long? = null
)

