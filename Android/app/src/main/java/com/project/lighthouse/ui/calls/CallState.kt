package com.project.lighthouse.ui.calls

data class CallState(
    val isCallActive: Boolean = false,
    val participantName: String? = null,
    val participantId: String? = null,
    val callId: String? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val callDuration: Long = 0L, // in seconds
    val errorMessage: String? = null
)

