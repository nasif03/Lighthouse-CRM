package com.project.lighthouse.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.ChatChannel
import com.project.lighthouse.data.model.ChatMessage
import com.project.lighthouse.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState(isLoading = true))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var loadChannelsJob: Job? = null
    private var loadMessagesJob: Job? = null
    private var messagePollingJob: Job? = null

    init {
        refreshChannels(initial = true)
    }
    
    override fun onCleared() {
        super.onCleared()
        messagePollingJob?.cancel()
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
        
        // Cancel previous polling
        messagePollingJob?.cancel()
        
        loadMessagesJob = viewModelScope.launch {
            Log.d("ChatViewModel", "Loading messages for channel: $channelType/$channelId")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = chatRepository.getMessages(channelType, channelId)
            result.onSuccess { messages ->
                Log.d("ChatViewModel", "Messages loaded: ${messages.size} items")
                // Sort messages by created_at in ascending order (oldest first, newest last)
                val sortedMessages = messages.sortedBy { message ->
                    message.createdAt?.let { 
                        try {
                            java.time.Instant.parse(it).toEpochMilli()
                        } catch (e: Exception) {
                            0L
                        }
                    } ?: 0L
                }
                _state.update {
                    it.copy(
                        messages = sortedMessages,
                        isLoading = false
                    )
                }
                
                // Start polling for new messages
                startMessagePolling(channelType, channelId, sortedMessages.size)
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
    
    private fun startMessagePolling(channelType: String, channelId: String, lastMessageCount: Int) {
        messagePollingJob?.cancel()
        messagePollingJob = viewModelScope.launch {
            while (isActive) {
                delay(2000) // Poll every 2 seconds
                
                val currentChannel = _state.value.selectedChannel
                if (currentChannel == null || currentChannel.type != channelType || currentChannel.id != channelId) {
                    // Channel changed, stop polling
                    break
                }
                
                // Check for new messages
                val result = chatRepository.getMessages(channelType, channelId)
                result.onSuccess { newMessages ->
                    val currentMessages = _state.value.messages
                    // Sort new messages by created_at in ascending order (oldest first, newest last)
                    val sortedNewMessages = newMessages.sortedBy { message ->
                        message.createdAt?.let { 
                            try {
                                java.time.Instant.parse(it).toEpochMilli()
                            } catch (e: Exception) {
                                0L
                            }
                        } ?: 0L
                    }
                    // Compare message IDs to detect new messages
                    // Since messages are in chronological order (oldest first), newest is at the end
                    val hasNewMessages = when {
                        sortedNewMessages.size != currentMessages.size -> true
                        sortedNewMessages.isEmpty() && currentMessages.isEmpty() -> false
                        sortedNewMessages.isNotEmpty() && currentMessages.isEmpty() -> true
                        sortedNewMessages.isEmpty() && currentMessages.isNotEmpty() -> false
                        else -> {
                            // Compare the newest message (last in list)
                            val newestNew = sortedNewMessages.lastOrNull()?.id
                            val newestCurrent = currentMessages.lastOrNull()?.id
                            newestNew != newestCurrent || newestNew == null
                        }
                    }
                    
                    if (hasNewMessages) {
                        Log.d("ChatViewModel", "New messages detected: ${sortedNewMessages.size} (was ${currentMessages.size})")
                        _state.update {
                            it.copy(messages = sortedNewMessages)
                        }
                    }
                }.onFailure { error ->
                    // Don't show error for polling failures, just log
                    Log.d("ChatViewModel", "Polling error (non-critical): ${error.message}")
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
                // Reload messages to get the new one immediately
                // The polling will also pick it up, but this ensures instant update
                val reloadResult = chatRepository.getMessages(currentChannel.type, currentChannel.id)
                reloadResult.onSuccess { updatedMessages ->
                    // Sort messages by created_at in ascending order (oldest first, newest last)
                    val sortedMessages = updatedMessages.sortedBy { message ->
                        message.createdAt?.let { 
                            try {
                                java.time.Instant.parse(it).toEpochMilli()
                            } catch (e: Exception) {
                                0L
                            }
                        } ?: 0L
                    }
                    _state.update {
                        it.copy(
                            messages = sortedMessages,
                            isSendingMessage = false
                        )
                    }
                }.onFailure {
                    // If reload fails, just stop sending state
                    _state.update { it.copy(isSendingMessage = false) }
                }
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
        // Stop message polling
        messagePollingJob?.cancel()
        messagePollingJob = null
        
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

