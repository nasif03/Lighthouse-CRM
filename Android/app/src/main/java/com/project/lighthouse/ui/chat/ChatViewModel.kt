package com.project.lighthouse.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.ChatChannel
import com.project.lighthouse.data.model.ChatMessage
import com.project.lighthouse.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState(isLoading = true))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var loadChannelsJob: Job? = null
    private var loadMessagesJob: Job? = null

    init {
        refreshChannels(initial = true)
    }

    fun refreshChannels(initial: Boolean = false) {
        if (loadChannelsJob?.isActive == true) return
        loadChannelsJob = viewModelScope.launch {
            Log.d("ChatViewModel", "refreshChannels initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = chatRepository.getChannels()
            result.onSuccess { channels ->
                Log.d("ChatViewModel", "Channels loaded: ${channels.size} items")
                _state.update {
                    it.copy(
                        channels = channels,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                Log.e("ChatViewModel", "Failed to load channels: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to load channels"
                    )
                }
            }
        }
    }

    fun selectChannel(channel: ChatChannel) {
        Log.d("ChatViewModel", "Selecting channel: ${channel.id}")
        _state.update {
            it.copy(
                selectedChannel = channel,
                showChannelList = false,
                messages = emptyList(),
                errorMessage = null
            )
        }
        loadMessages(channel.type, channel.id)
    }

    fun loadMessages(channelType: String, channelId: String) {
        if (loadMessagesJob?.isActive == true) return
        loadMessagesJob = viewModelScope.launch {
            Log.d("ChatViewModel", "Loading messages for channel: $channelType/$channelId")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = chatRepository.getMessages(channelType, channelId)
            result.onSuccess { messages ->
                Log.d("ChatViewModel", "Messages loaded: ${messages.size} items")
                _state.update {
                    it.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                Log.e("ChatViewModel", "Failed to load messages: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load messages"
                    )
                }
            }
        }
    }

    fun toggleUserSelection(show: Boolean) {
        Log.d("ChatViewModel", "toggleUserSelection: $show")
        if (show) {
            loadAvailableUsers()
        }
        _state.update { it.copy(showUserSelection = show, errorMessage = null) }
    }

    fun loadAvailableUsers() {
        viewModelScope.launch {
            Log.d("ChatViewModel", "Loading available users")
            val result = chatRepository.getChatUsers()
            result.onSuccess { users ->
                Log.d("ChatViewModel", "Loaded ${users.size} available users")
                _state.update { it.copy(availableUsers = users) }
            }.onFailure { error ->
                Log.e("ChatViewModel", "Failed to load users: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message ?: "Failed to load users") }
            }
        }
    }

    fun createChannelWithUser(userId: String) {
        viewModelScope.launch {
            Log.d("ChatViewModel", "Creating channel with user: $userId")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = chatRepository.createDirectChannel(userId)
            result.onSuccess { channelResponse ->
                Log.d("ChatViewModel", "Channel created: ${channelResponse.id}")
                // Refresh channels and select the new one
                refreshChannels()
                // Find the channel in the updated list
                _state.update {
                    it.copy(
                        isLoading = false,
                        showUserSelection = false,
                        infoMessage = "Channel created successfully"
                    )
                }
            }.onFailure { error ->
                Log.e("ChatViewModel", "Failed to create channel: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to create channel"
                    )
                }
            }
        }
    }

    fun updateMessageInput(text: String) {
        _state.update { it.copy(messageInput = text) }
    }

    fun sendMessage() {
        val currentChannel = _state.value.selectedChannel
        val messageText = _state.value.messageInput.trim()
        
        if (currentChannel == null || messageText.isBlank()) {
            Log.w("ChatViewModel", "Cannot send message: channel=${currentChannel != null}, text=${messageText.isNotBlank()}")
            return
        }

        viewModelScope.launch {
            Log.d("ChatViewModel", "Sending message: $messageText")
            _state.update {
                it.copy(
                    isSendingMessage = true,
                    errorMessage = null,
                    messageInput = ""
                )
            }
            val result = chatRepository.sendMessage(currentChannel.type, currentChannel.id, messageText)
            result.onSuccess { response ->
                Log.d("ChatViewModel", "Message sent successfully")
                // Reload messages to get the new one
                loadMessages(currentChannel.type, currentChannel.id)
                _state.update { it.copy(isSendingMessage = false) }
            }.onFailure { error ->
                Log.e("ChatViewModel", "Failed to send message: ${error.message}", error)
                _state.update {
                    it.copy(
                        isSendingMessage = false,
                        errorMessage = error.message ?: "Failed to send message",
                        messageInput = messageText // Restore input on error
                    )
                }
            }
        }
    }

    fun goBackToChannelList() {
        Log.d("ChatViewModel", "Going back to channel list")
        _state.update {
            it.copy(
                showChannelList = true,
                selectedChannel = null,
                messages = emptyList()
            )
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}

