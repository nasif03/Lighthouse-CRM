package com.project.lighthouse.ui.supportchat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.SupportChatMessage
import com.project.lighthouse.data.repository.SupportChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class SupportChatViewModel(
    private val supportChatRepository: SupportChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupportChatState())
    val state: StateFlow<SupportChatState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            Log.d("SupportChatViewModel", "Loading conversation history")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = supportChatRepository.getHistory(_state.value.conversationId)
            result.onSuccess { history ->
                Log.d("SupportChatViewModel", "History loaded: ${history.messages.size} messages, conversationId=${history.conversationId}")
                _state.update {
                    it.copy(
                        messages = history.messages,
                        conversationId = history.conversationId,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                Log.e("SupportChatViewModel", "Failed to load history: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load conversation history"
                    )
                }
            }
        }
    }

    fun updateMessageInput(text: String) {
        _state.update { it.copy(messageInput = text) }
    }

    fun sendMessage() {
        val messageText = _state.value.messageInput.trim()
        if (messageText.isBlank()) {
            Log.w("SupportChatViewModel", "Cannot send empty message")
            return
        }

        viewModelScope.launch {
            Log.d("SupportChatViewModel", "Sending message to support AI: $messageText")
            
            // Add user message to UI immediately
            val userMessage = SupportChatMessage(
                id = UUID.randomUUID().toString(),
                role = "user",
                content = messageText,
                createdAt = java.time.Instant.now().toString()
            )
            _state.update {
                it.copy(
                    messages = it.messages + userMessage,
                    messageInput = "",
                    isSending = true,
                    errorMessage = null
                )
            }

            val result = supportChatRepository.sendMessage(
                message = messageText,
                conversationId = _state.value.conversationId
            )

            result.onSuccess { response ->
                Log.d("SupportChatViewModel", "Support AI response received: length=${response.reply.length}, conversationId=${response.conversationId}")
                
                // Add assistant response to UI
                val assistantMessage = SupportChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "assistant",
                    content = response.reply,
                    createdAt = java.time.Instant.now().toString()
                )
                _state.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        conversationId = response.conversationId,
                        isSending = false
                    )
                }
            }.onFailure { error ->
                Log.e("SupportChatViewModel", "Failed to send message: ${error.message}", error)
                
                // Add error message to UI
                val errorMessage = SupportChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "assistant",
                    content = "Sorry, I couldn't process your request. Please try again.",
                    createdAt = java.time.Instant.now().toString()
                )
                _state.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isSending = false,
                        errorMessage = error.message ?: "Failed to send message"
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "SupportChatViewModel"
    }
}

