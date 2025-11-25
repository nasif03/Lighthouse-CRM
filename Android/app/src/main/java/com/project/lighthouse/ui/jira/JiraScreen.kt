package com.project.lighthouse.ui.jira

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.JiraIssue
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JiraScreen(
    state: JiraState,
    orgId: String?,
    onRefresh: () -> Unit,
    onSetProjectType: (String) -> Unit,
    onToggleCreateProjectDialog: (Boolean) -> Unit,
    onCreateProject: (String) -> Unit,
    onCreateSoftwareProject: (String) -> Unit,
    onToggleCreateIssueDialog: (Boolean, String?) -> Unit,
    onCreateIssueForTicket: (String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            Log.d("JiraScreen", "Showing error: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
        state.infoMessage?.let {
            Log.d("JiraScreen", "Showing info: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Jira Issues", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.isRefreshing) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (orgId != null) {
                FloatingActionButton(onClick = { onToggleCreateProjectDialog(true) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_leads),
                        contentDescription = "Create Project"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Project type selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SegmentedButton(
                    selected = state.projectType == "jsm",
                    onClick = {
                        Log.d("JiraScreen", "Switching to JSM project type")
                        onSetProjectType("jsm")
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("JSM", fontSize = 12.sp)
                }
                SegmentedButton(
                    selected = state.projectType == "software",
                    onClick = {
                        Log.d("JiraScreen", "Switching to Software project type")
                        onSetProjectType("software")
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Software", fontSize = 12.sp)
                }
            }

            if (state.isLoading && state.issues.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                    Text(
                        text = "Loading Jira issues...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (state.issues.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Jira issues",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Create a project to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.issues, key = { it.key }) { issue ->
                        JiraIssueCard(
                            issue = issue,
                            onOpenInBrowser = { url ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("JiraScreen", "Failed to open URL", e)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (state.showCreateProjectDialog && orgId != null) {
        CreateProjectDialog(
            onDismiss = { onToggleCreateProjectDialog(false) },
            onCreateProject = { onCreateProject(orgId) },
            onCreateSoftwareProject = { onCreateSoftwareProject(orgId) }
        )
    }

    if (state.showCreateIssueDialog && state.selectedTicketId != null) {
        CreateIssueDialog(
            ticketId = state.selectedTicketId,
            onDismiss = { onToggleCreateIssueDialog(false, null) },
            onCreate = { onCreateIssueForTicket(state.selectedTicketId!!) }
        )
    }
}

@Composable
private fun JiraIssueCard(
    issue: JiraIssue,
    onOpenInBrowser: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Key and Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = issue.key,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        color = Brand600,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = issue.summary,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Gray900,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Status and Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Status: ${issue.status}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Gray700
                )
                issue.priority?.let {
                    Text(
                        text = "Priority: $it",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Gray700
                    )
                }
            }
            // Reporter
            issue.reporterName?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reporter: $it",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Gray500
                )
            }
            // Linked issue
            issue.linkedJiraSoftwareIssue?.let { linked ->
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val url = "https://lighthouse-crm.atlassian.net/browse/$linked"
                        onOpenInBrowser(url)
                    }
                ) {
                    Text("View Linked Issue: $linked", fontSize = 11.sp, color = Brand600)
                }
            }
            issue.linkedJsmTicket?.let { linked ->
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        val url = "https://lighthouse-crm.atlassian.net/browse/$linked"
                        onOpenInBrowser(url)
                    }
                ) {
                    Text("View Linked Ticket: $linked", fontSize = 11.sp, color = Brand600)
                }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: () -> Unit,
    onCreateSoftwareProject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Jira Project", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose project type:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCreateProject,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("JSM Project", fontSize = 12.sp)
                }
                Button(
                    onClick = onCreateSoftwareProject,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Software Project", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        dismissButton = null
    )
}

@Composable
private fun CreateIssueDialog(
    ticketId: String,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Jira Issue", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Create a Jira Software issue for ticket: $ticketId",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onCreate) {
                Text("Create Issue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

