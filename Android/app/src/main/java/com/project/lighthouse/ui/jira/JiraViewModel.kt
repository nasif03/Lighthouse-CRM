package com.project.lighthouse.ui.jira

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.lighthouse.data.repository.JiraRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JiraViewModel(
    private val jiraRepository: JiraRepository
) : ViewModel() {

    private val _state = MutableStateFlow(JiraState(isLoading = true))
    val state: StateFlow<JiraState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refreshIssues(initial = true)
    }

    fun refreshIssues(initial: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Log.d("JiraViewModel", "refreshIssues initial=$initial, projectType=${_state.value.projectType}")
            _state.update {
                it.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val result = jiraRepository.getIssues(_state.value.projectType)
            result.onSuccess { issues ->
                Log.d("JiraViewModel", "Issues loaded: ${issues.size} items")
                _state.update {
                    it.copy(
                        issues = issues,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }.onFailure { error ->
                Log.e("JiraViewModel", "Failed to load issues: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "Failed to load issues"
                    )
                }
            }
        }
    }

    fun setProjectType(projectType: String) {
        Log.d("JiraViewModel", "Setting project type: $projectType")
        _state.update { it.copy(projectType = projectType) }
        refreshIssues(initial = true)
    }

    fun createProject(orgId: String) {
        viewModelScope.launch {
            Log.d("JiraViewModel", "Creating Jira project for org: $orgId")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = jiraRepository.createProject(orgId)
            result.onSuccess { project ->
                Log.d("JiraViewModel", "Project created: ${project.projectKey}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        showCreateProjectDialog = false,
                        infoMessage = "Project created: ${project.projectKey}"
                    )
                }
                refreshIssues()
            }.onFailure { error ->
                Log.e("JiraViewModel", "Failed to create project: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to create project"
                    )
                }
            }
        }
    }

    fun createSoftwareProject(orgId: String) {
        viewModelScope.launch {
            Log.d("JiraViewModel", "Creating Jira Software project for org: $orgId")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = jiraRepository.createSoftwareProject(orgId)
            result.onSuccess { project ->
                Log.d("JiraViewModel", "Software project created: ${project.projectKey}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        showCreateProjectDialog = false,
                        infoMessage = "Software project created: ${project.projectKey}"
                    )
                }
            }.onFailure { error ->
                Log.e("JiraViewModel", "Failed to create software project: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to create software project"
                    )
                }
            }
        }
    }

    fun createIssueForTicket(ticketId: String) {
        viewModelScope.launch {
            Log.d("JiraViewModel", "Creating Jira issue for ticket: $ticketId")
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = jiraRepository.createIssueForTicket(ticketId)
            result.onSuccess { response ->
                Log.d("JiraViewModel", "Issue created: ${response.issueKey}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        showCreateIssueDialog = false,
                        selectedTicketId = null,
                        infoMessage = "Issue created: ${response.issueKey}"
                    )
                }
                refreshIssues()
            }.onFailure { error ->
                Log.e("JiraViewModel", "Failed to create issue: ${error.message}", error)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to create issue"
                    )
                }
            }
        }
    }

    fun toggleCreateProjectDialog(show: Boolean) {
        Log.d("JiraViewModel", "toggleCreateProjectDialog: $show")
        _state.update { it.copy(showCreateProjectDialog = show, errorMessage = null) }
    }

    fun toggleCreateIssueDialog(show: Boolean, ticketId: String? = null) {
        Log.d("JiraViewModel", "toggleCreateIssueDialog: $show, ticketId=$ticketId")
        _state.update {
            it.copy(
                showCreateIssueDialog = show,
                selectedTicketId = ticketId,
                errorMessage = null
            )
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    companion object {
        private const val TAG = "JiraViewModel"
    }
}

