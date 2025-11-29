package com.project.lighthouse.ui.fireflies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.FirefliesTranscript
import com.project.lighthouse.data.repository.FirefliesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FirefliesViewModel(
    private val firefliesRepository: FirefliesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FirefliesState(isLoading = true))
    val state: StateFlow<FirefliesState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var syncJob: Job? = null

    init {
        loadTranscripts(initial = true)
    }

    fun loadTranscripts(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d(TAG, "loadTranscripts initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = firefliesRepository.getTranscripts(limit = 20)
            result.onSuccess { transcripts ->
                Log.d(TAG, "Transcripts loaded: ${transcripts.size} items")
                _state.update {
                    it.copy(
                        transcripts = transcripts,
                        isLoading = false,
                        isRefreshing = false,
                        lastFetchedAt = System.currentTimeMillis()
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load transcripts: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to load transcripts"
                    )
                }
            }
        }
    }

    fun syncTranscripts() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            Log.d(TAG, "syncTranscripts")
            _state.update {
                it.copy(
                    isSyncing = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = firefliesRepository.syncTranscripts(limit = 20)
            result.onSuccess { savedCount ->
                Log.d(TAG, "Transcripts synced: $savedCount saved")
                val message = if (savedCount > 0) {
                    "Synced $savedCount transcript${if (savedCount == 1) "" else "s"} from Fireflies."
                } else {
                    "No new transcripts were synced."
                }
                _state.update {
                    it.copy(
                        isSyncing = false,
                        infoMessage = message,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                }
                // Refresh list after sync
                loadTranscripts()
            }.onFailure { error ->
                Log.e(TAG, "Failed to sync transcripts: ${error.message}", error)
                _state.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = error.message ?: "Failed to sync transcripts"
                    )
                }
            }
        }
    }

    fun selectTranscript(transcript: FirefliesTranscript?) {
        _state.update { it.copy(selectedTranscript = transcript) }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "FirefliesViewModel"
    }
}

