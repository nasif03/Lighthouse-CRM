package com.project.lighthouse.ui.supportai

import com.project.lighthouse.data.model.SupportChatMessage

data class SupportAIState(
    val messages: List<SupportChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val isLoadingHistory: Boolean = true,
    val errorMessage: String? = null,
    val conversationId: String? = null
)

