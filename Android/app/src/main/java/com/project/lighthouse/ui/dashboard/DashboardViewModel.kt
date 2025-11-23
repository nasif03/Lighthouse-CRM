package com.project.lighthouse.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.repository.DashboardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState(isLoading = true))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refreshDashboard(initial = true)
    }

    fun refreshDashboard(initial: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            Log.d(TAG, "Refresh dashboard requested. initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null
                )
            }

            val statsResult = dashboardRepository.getDashboardStats()
            val recentResult = dashboardRepository.getRecentItems()

            statsResult.onSuccess { stats ->
                _state.update { current ->
                    current.copy(stats = stats)
                }
            }.onFailure { error ->
                Log.e(TAG, "Stats fetch failed: ${error.message}", error)
                _state.update { current ->
                    current.copy(errorMessage = error.message)
                }
            }

            recentResult.onSuccess { recent ->
                _state.update { current ->
                    current.copy(recentItems = recent)
                }
            }.onFailure { error ->
                Log.e(TAG, "Recent fetch failed: ${error.message}", error)
                _state.update { current ->
                    current.copy(errorMessage = error.message)
                }
            }

            _state.update {
                it.copy(isLoading = false, isRefreshing = false)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}

