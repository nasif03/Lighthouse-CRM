package com.project.lighthouse.ui.supportchat

import com.project.lighthouse.data.model.SupportChatMessage

data class SupportChatState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val messages: List<SupportChatMessage> = emptyList(),
    val conversationId: String? = null,
    val messageInput: String = "",
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

