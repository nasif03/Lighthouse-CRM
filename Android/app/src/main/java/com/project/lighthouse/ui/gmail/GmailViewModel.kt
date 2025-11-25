package com.project.lighthouse.ui.gmail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.repository.GmailRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GmailViewModel(
    private val gmailRepository: GmailRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GmailState(isLoading = true))
    val state: StateFlow<GmailState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            Log.d("GmailViewModel", "Checking Gmail auth status")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = gmailRepository.getAuthStatus()
            result.onSuccess { authResponse ->
                Log.d("GmailViewModel", "Auth status: authenticated=${authResponse.authenticated}")
                _state.update {
                    it.copy(
                        isAuthenticated = authResponse.authenticated,
                        isLoading = false,
                        authorizationUrl = authResponse.authorizationUrl
                    )
                }
                if (authResponse.authenticated) {
                    refreshMessages()
                }
            }.onFailure { error ->
                Log.e("GmailViewModel", "Failed to check auth status: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to check authentication status"
                    )
                }
            }
        }
    }

    fun authenticate(authorizationCode: String? = null, accessToken: String? = null, refreshToken: String? = null) {
        viewModelScope.launch {
            Log.d("GmailViewModel", "Authenticating Gmail")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = gmailRepository.authenticate(authorizationCode, accessToken, refreshToken)
            result.onSuccess { authResponse ->
                Log.d("GmailViewModel", "Gmail authentication successful")
                _state.update {
                    it.copy(
                        isAuthenticated = authResponse.authenticated,
                        isLoading = false,
                        infoMessage = authResponse.message
                    )
                }
                if (authResponse.authenticated) {
                    refreshMessages()
                }
            }.onFailure { error ->
                Log.e("GmailViewModel", "Failed to authenticate: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to authenticate Gmail"
                    )
                }
            }
        }
    }

    fun refreshMessages(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d("GmailViewModel", "refreshMessages initial=$initial")
            if (!_state.value.isAuthenticated) {
                Log.w("GmailViewModel", "Cannot refresh messages: not authenticated")
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = gmailRepository.getMessages(maxResults = 20)
            result.onSuccess { messages ->
                Log.d("GmailViewModel", "Messages loaded: ${messages.size} items")
                _state.update {
                    it.copy(
                        messages = messages,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                Log.e("GmailViewModel", "Failed to load messages: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to load messages"
                    )
                }
            }
        }
    }

    fun toggleSendEmailDialog(show: Boolean) {
        Log.d("GmailViewModel", "toggleSendEmailDialog: $show")
        _state.update { it.copy(showSendEmailDialog = show, errorMessage = null, infoMessage = null) }
        if (!show) {
            _state.update { it.copy(sendEmailFormState = SendEmailFormState()) }
        }
    }

    fun updateSendEmailForm(to: String? = null, subject: String? = null, body: String? = null) {
        _state.update {
            it.copy(
                sendEmailFormState = it.sendEmailFormState.copy(
                    to = to ?: it.sendEmailFormState.to,
                    subject = subject ?: it.sendEmailFormState.subject,
                    body = body ?: it.sendEmailFormState.body
                )
            )
        }
    }

    fun sendEmail() {
        val currentForm = _state.value.sendEmailFormState
        if (currentForm.to.isBlank() || currentForm.subject.isBlank() || currentForm.body.isBlank()) {
            Log.w("GmailViewModel", "Send email validation failed: all fields required")
            _state.update { it.copy(errorMessage = "To, subject, and body are required") }
            return
        }
        viewModelScope.launch {
            Log.d("GmailViewModel", "Sending email to: ${currentForm.to}")
            _state.update { it.copy(sendEmailFormState = it.sendEmailFormState.copy(isSubmitting = true)) }
            val result = gmailRepository.sendEmail(
                to = currentForm.to.trim(),
                subject = currentForm.subject.trim(),
                body = currentForm.body.trim()
            )
            result.onSuccess { response ->
                Log.d("GmailViewModel", "Email sent successfully: ${response.id}")
                _state.update {
                    it.copy(
                        sendEmailFormState = SendEmailFormState(),
                        showSendEmailDialog = false,
                        infoMessage = "Email sent successfully"
                    )
                }
                refreshMessages()
            }.onFailure { error ->
                Log.e("GmailViewModel", "Failed to send email: ${error.message}", error)
                _state.update {
                    it.copy(
                        sendEmailFormState = it.sendEmailFormState.copy(isSubmitting = false),
                        errorMessage = error.message ?: "Failed to send email"
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "GmailViewModel"
    }
}

