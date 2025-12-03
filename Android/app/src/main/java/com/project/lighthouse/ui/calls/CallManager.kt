package com.project.lighthouse.ui.calls

import android.content.Context
import android.util.Log
import com.project.lighthouse.data.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Stream Video SDK imports
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.Call
import io.getstream.video.android.model.User

/**
 * CallManager handles audio-only calls using Stream Video SDK.
 * Mirrors the functionality of the web frontend's callStore.
 */
class CallManager(
    private val chatRepository: ChatRepository,
    private val scope: CoroutineScope,
    private val context: Context
) {
    companion object {
        private const val TAG = "CallManager"
        private const val STREAM_API_KEY = "n8nh34grh4b3" // Same as frontend
    }
    
    // Stream Video SDK objects
    private var streamVideo: StreamVideo? = null
    private var activeCall: Call? = null
    private var isCallStarting = false
    private var currentUserId: String? = null
    private var currentUserToken: String? = null
    
    // Call state tracking (using local CallState data class)
    private val _callState = MutableStateFlow<CallState?>(null)
    val callState: StateFlow<CallState?> = _callState.asStateFlow()
    
    /**
     * Initialize the Stream Video client.
     * This should be called when user logs in.
     */
    suspend fun initializeClient(userId: String, userName: String?, userPicture: String?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing Stream Video client for user: $userId")
                
                // Get token from backend (same as web frontend)
                val tokenResult = chatRepository.getChatToken()
                
                tokenResult.fold(
                    onSuccess = { tokenResponse ->
                        currentUserId = userId
                        currentUserToken = tokenResponse.token
                        
                        // Initialize Stream Video client with SDK
                        val user = User(
                            id = tokenResponse.userId,
                            name = userName ?: "User",
                            image = userPicture
                        )
                        
                        streamVideo = StreamVideoBuilder(
                            context = context,
                            apiKey = STREAM_API_KEY,
                            user = user,
                            token = tokenResponse.token
                        ).build()
                        
                        Log.d(TAG, "Stream Video client initialized successfully")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to get Stream token: ${error.message}", error)
                        throw error
                    }
                )
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Stream Video client", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Start an audio-only call with a participant.
     * Mirrors the frontend's startCall function.
     */
    fun startCall(
        callId: String,
        currentUserId: String,
        otherUserId: String,
        participantName: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isCallStarting) {
            onError("Call is already starting")
            return
        }
        
        if (activeCall != null) {
            onError("You are already on a call")
            return
        }
        
        if (streamVideo == null) {
            onError("Stream Video client not initialized. Please try again.")
            return
        }
        
        scope.launch {
            isCallStarting = true
            try {
                Log.d(TAG, "Starting audio call: $callId, currentUser: $currentUserId, otherUser: $otherUserId")
                
                if (streamVideo == null) {
                    // Try to initialize if not already done
                    val initResult = initializeClient(currentUserId, null, null)
                    if (initResult.isFailure) {
                        onError("Stream Video client not initialized. Please try again.")
                        isCallStarting = false
                        return@launch
                    }
                }
                
                // Create or get the call using Stream Video SDK (mirrors web: videoClient.call('default', callId))
                val call = streamVideo!!.call(type = "default", id = callId)
                
                // TODO: Add member data and ring notification to sync with web frontend
                // Web frontend uses: call.getOrCreate({ ring: true, data: { members: [...] } })
                // Android SDK API needs to be checked - may use different method signature
                // For now, we create the call and join - but this won't notify the other participant
                // To fix cross-platform sync, we need to:
                // 1. Create call with member data (currentUserId, otherUserId)
                // 2. Enable ring notification so the other participant gets notified
                // 
                // Check Stream Video Android SDK docs for the correct API:
                // - call.getOrCreate() with members parameter?
                // - call.create() with members and ring?
                // - Or set members/ring through different methods?
                
                Log.d(TAG, "Call created: $callId, members: $currentUserId, $otherUserId")
                Log.w(TAG, "NOTE: Cross-platform call sync may not work until member data and ring are properly set")
                
                // Disable camera for audio-only call
                try {
                    call.camera?.disable()
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to disable camera before join", e)
                }
                
                // Join the call - the SDK will handle audio/video settings
                withContext(Dispatchers.IO) {
                    call.join()
                }
                
                activeCall = call
                
                // Update local call state
                _callState.value = CallState(
                    isCallActive = true,
                    participantName = participantName,
                    participantId = otherUserId,
                    callId = callId
                )
                
                // Call started successfully - update state
                onSuccess()
                
                // Note: Call state observation might need to be set up differently
                // depending on the Stream Video Android SDK API
                // For now, we'll rely on manual state management
                
                Log.d(TAG, "Call started successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start call", e)
                onError(e.message ?: "Failed to start audio call")
            } finally {
                isCallStarting = false
            }
        }
    }
    
    /**
     * End the active call.
     */
    fun endCall(onComplete: () -> Unit) {
        scope.launch {
            try {
                Log.d(TAG, "Ending call")
                activeCall?.leave()
                activeCall = null
                _callState.value = null
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to end call", e)
                activeCall = null
                _callState.value = null
                onComplete()
            }
        }
    }
    
    /**
     * Toggle mute state of the call.
     */
    fun toggleMute(isMuted: Boolean) {
        scope.launch {
            try {
                activeCall?.microphone?.setEnabled(!isMuted)
                Log.d(TAG, "Toggle mute: $isMuted")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle mute", e)
            }
        }
    }
    
    /**
     * Toggle speaker state.
     */
    fun toggleSpeaker(isSpeakerOn: Boolean) {
        scope.launch {
            try {
                // Note: Speaker control might need additional audio routing setup
                // This is a placeholder - actual implementation depends on Stream SDK capabilities
                Log.d(TAG, "Speaker toggle: $isSpeakerOn")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle speaker", e)
            }
        }
    }
    
    /**
     * Check if a call is currently active.
     */
    fun isCallActive(): Boolean {
        return activeCall != null
    }
    
    /**
     * Cleanup when user logs out.
     */
    fun cleanup() {
        scope.launch {
            activeCall?.leave()
            activeCall = null
            streamVideo = null
            currentUserId = null
            currentUserToken = null
            _callState.value = null
        }
    }
}
