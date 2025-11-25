package com.project.lighthouse.ui.chat

import com.project.lighthouse.data.model.ChatChannel
import com.project.lighthouse.data.model.ChatMessage
import com.project.lighthouse.data.model.ChatUser

data class ChatState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val channels: List<ChatChannel> = emptyList(),
    val selectedChannel: ChatChannel? = null,
    val messages: List<ChatMessage> = emptyList(),
    val availableUsers: List<ChatUser> = emptyList(),
    val showUserSelection: Boolean = false,
    val showChannelList: Boolean = true,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isSendingMessage: Boolean = false,
    val messageInput: String = ""
)

