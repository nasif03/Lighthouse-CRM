package com.project.lighthouse.ui.supportai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.SupportChatMessage
import com.project.lighthouse.data.repository.SupportChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupportAIViewModel(
    private val supportChatRepository: SupportChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupportAIState(isLoadingHistory = true))
    val state: StateFlow<SupportAIState> = _state.asStateFlow()

    private var loadHistoryJob: Job? = null
    private var sendMessageJob: Job? = null

    init {
        loadHistory()
    }

    fun loadHistory() {
        if (loadHistoryJob?.isActive == true) return
        loadHistoryJob = viewModelScope.launch {
            Log.d(TAG, "loadHistory")
            _state.update { it.copy(isLoadingHistory = true, errorMessage = null) }
            val result = supportChatRepository.getHistory(_state.value.conversationId)
            result.onSuccess { history ->
                Log.d(TAG, "History loaded: ${history.messages.size} messages, conversationId=${history.conversationId}")
                if (history.messages.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            messages = history.messages,
                            conversationId = history.conversationId,
                            isLoadingHistory = false
                        )
                    }
                } else {
                    // No history - show welcome message
                    val welcomeMessage = SupportChatMessage(
                        id = "assistant-welcome",
                        role = "assistant",
                        content = "Hi! I'm Support AI. Ask anything about Jira/JSM integration, CRM workflows, or Lighthouse MCP setup and I will guide you.",
                        createdAt = ""
                    )
                    _state.update {
                        it.copy(
                            messages = listOf(welcomeMessage),
                            conversationId = history.conversationId,
                            isLoadingHistory = false
                        )
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load history: ${error.message}", error)
                // Show welcome message on error
                val welcomeMessage = SupportChatMessage(
                    id = "assistant-welcome",
                    role = "assistant",
                    content = "Hi! I'm Support AI. Ask anything about Jira/JSM integration, CRM workflows, or Lighthouse MCP setup and I will guide you.",
                    createdAt = ""
                )
                _state.update {
                    it.copy(
                        messages = listOf(welcomeMessage),
                        isLoadingHistory = false,
                        errorMessage = null // Don't show error, just show welcome
                    )
                }
            }
        }
    }

    fun updateInput(input: String) {
        if (input.length <= 2000) {
            _state.update { it.copy(input = input) }
        }
    }

    fun sendMessage() {
        val currentInput = _state.value.input.trim()
        if (currentInput.isEmpty() || _state.value.isSending) return

        if (sendMessageJob?.isActive == true) return
        sendMessageJob = viewModelScope.launch {
            Log.d(TAG, "sendMessage: $currentInput")
            val userMessage = SupportChatMessage(
                id = "user-${System.currentTimeMillis()}",
                role = "user",
                content = currentInput,
                createdAt = ""
            )

            // Add user message immediately
            _state.update {
                it.copy(
                    messages = it.messages + userMessage,
                    input = "",
                    isSending = true,
                    errorMessage = null
                )
            }

            val result = supportChatRepository.sendMessage(
                message = currentInput,
                conversationId = _state.value.conversationId
            )

            result.onSuccess { response ->
                Log.d(TAG, "Message sent successfully, conversationId=${response.conversationId}")
                val assistantMessage = SupportChatMessage(
                    id = "assistant-${System.currentTimeMillis()}",
                    role = "assistant",
                    content = response.reply,
                    createdAt = ""
                )
                _state.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        conversationId = response.conversationId,
                        isSending = false
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to send message: ${error.message}", error)
                val errorMessage = SupportChatMessage(
                    id = "assistant-error-${System.currentTimeMillis()}",
                    role = "assistant",
                    content = "Sorry, I could not respond at the moment. Please try again shortly.",
                    createdAt = ""
                )
                _state.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isSending = false,
                        errorMessage = error.message ?: "Unable to reach Support AI right now. Please try again."
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "SupportAIViewModel"
    }
}

