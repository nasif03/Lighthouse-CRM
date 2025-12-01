package com.project.lighthouse.ui.contacts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.model.ContactDto
import com.project.lighthouse.data.model.CreateContactRequest
import com.project.lighthouse.data.model.UpdateContactRequest
import com.project.lighthouse.data.repository.AccountsRepository
import com.project.lighthouse.data.repository.ContactsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val accountsRepository: AccountsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ContactsState(isLoading = true))
    val state: StateFlow<ContactsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refreshContacts(initial = true)
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            Log.d(TAG, "Loading accounts for contact form")
            val result = accountsRepository.getAccounts()
            result.onSuccess { accounts ->
                Log.d(TAG, "Accounts loaded: ${accounts.size} items")
                _state.update { it.copy(accounts = accounts) }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load accounts: ${error.message}", error)
                // Don't show error to user, just log it - accounts are optional
            }
        }
    }

    fun refreshContacts(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d(TAG, "refreshContacts initial=$initial")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = contactsRepository.getContacts()
            result.onSuccess { contacts ->
                Log.d(TAG, "Contacts loaded: ${contacts.size} items")
                _state.update {
                    it.copy(
                        contacts = contacts,
                        isLoading = false,
                        isRefreshing = false,
                        actionInProgress = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load contacts: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message,
                        actionInProgress = null
                    )
                }
            }
        }
    }

    fun toggleCreateDialog(show: Boolean) {
        _state.update { it.copy(showCreateDialog = show) }
        if (!show) {
            _state.update { it.copy(formState = ContactFormState()) }
        }
    }

    fun updateForm(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        title: String? = null,
        selectedAccountId: String? = null,
        tags: List<String>? = null
    ) {
        _state.update {
            it.copy(
                formState = it.formState.copy(
                    firstName = firstName ?: it.formState.firstName,
                    lastName = lastName ?: it.formState.lastName,
                    email = email ?: it.formState.email,
                    phone = phone ?: it.formState.phone,
                    title = title ?: it.formState.title,
                    selectedAccountId = selectedAccountId ?: it.formState.selectedAccountId,
                    tags = tags ?: it.formState.tags
                )
            )
        }
    }

    fun createContact() {
        val form = _state.value.formState
        if (form.firstName.isBlank() || form.email.isBlank()) {
            Log.w(TAG, "Create contact validation failed: missing required fields")
            _state.update { it.copy(errorMessage = "First name and email are required") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Creating contact: ${form.firstName} ${form.lastName}")
            _state.update { it.copy(formState = it.formState.copy(isSubmitting = true)) }
            val result = contactsRepository.createContact(
                CreateContactRequest(
                    firstName = form.firstName.trim(),
                    lastName = form.lastName.takeIf { it.isNotBlank() }?.trim(),
                    email = form.email.trim(),
                    phone = form.phone.takeIf { it.isNotBlank() },
                    title = form.title.takeIf { it.isNotBlank() },
                    accountId = form.selectedAccountId?.takeIf { it.isNotBlank() },
                    tags = form.tags
                )
            )
            result.onSuccess { contact ->
                Log.d(TAG, "Contact created successfully: ${contact.id}")
                _state.update {
                    it.copy(
                        contacts = listOf(contact) + it.contacts,
                        formState = ContactFormState(),
                        showCreateDialog = false,
                        infoMessage = "Contact created"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to create contact: ${error.message}", error)
                _state.update {
                    it.copy(
                        errorMessage = error.message,
                        formState = it.formState.copy(isSubmitting = false)
                    )
                }
            }
        }
    }

    fun toggleEditDialog(show: Boolean, contact: ContactDto? = null) {
        _state.update {
            it.copy(
                showEditDialog = show,
                editingContactId = contact?.id,
                formState = if (show && contact != null) {
                    ContactFormState(
                        firstName = contact.firstName ?: "",
                        lastName = contact.lastName ?: "",
                        email = contact.email,
                        phone = contact.phone ?: "",
                        title = contact.title ?: "",
                        selectedAccountId = contact.accountId,
                        tags = contact.tags
                    )
                } else {
                    ContactFormState()
                }
            )
        }
    }

    fun updateContact() {
        val contactId = _state.value.editingContactId ?: return
        val form = _state.value.formState
        if (form.firstName.isBlank() || form.email.isBlank()) {
            Log.w(TAG, "Update contact validation failed: missing required fields")
            _state.update { it.copy(errorMessage = "First name and email are required") }
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Updating contact: $contactId")
            _state.update { it.copy(formState = it.formState.copy(isSubmitting = true)) }
            val result = contactsRepository.updateContact(
                contactId,
                UpdateContactRequest(
                    firstName = form.firstName.trim(),
                    lastName = form.lastName.takeIf { it.isNotBlank() }?.trim(),
                    email = form.email.trim(),
                    phone = form.phone.takeIf { it.isNotBlank() },
                    title = form.title.takeIf { it.isNotBlank() },
                    accountId = form.selectedAccountId?.takeIf { it.isNotBlank() },
                    tags = form.tags
                )
            )
            result.onSuccess { updatedContact ->
                Log.d(TAG, "Contact updated successfully: ${updatedContact.id}")
                _state.update {
                    it.copy(
                        contacts = it.contacts.map { c -> if (c.id == contactId) updatedContact else c },
                        formState = ContactFormState(),
                        showEditDialog = false,
                        editingContactId = null,
                        infoMessage = "Contact updated"
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to update contact: ${error.message}", error)
                _state.update {
                    it.copy(
                        errorMessage = error.message,
                        formState = it.formState.copy(isSubmitting = false)
                    )
                }
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting contact: $contactId")
            _state.update { it.copy(actionInProgress = contactId) }
            val result = contactsRepository.deleteContact(contactId)
            result.onSuccess {
                Log.d(TAG, "Contact deleted successfully: $contactId")
                _state.update {
                    it.copy(
                        contacts = it.contacts.filterNot { contact -> contact.id == contactId },
                        infoMessage = "Contact deleted",
                        actionInProgress = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to delete contact: ${error.message}", error)
                _state.update { it.copy(errorMessage = error.message, actionInProgress = null) }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "ContactsViewModel"
    }
}

