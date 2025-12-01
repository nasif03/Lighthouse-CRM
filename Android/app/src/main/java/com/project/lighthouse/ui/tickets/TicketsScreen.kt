package com.project.lighthouse.ui.tickets

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.project.lighthouse.data.model.TicketDto
import com.project.lighthouse.ui.common.StatusChip
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Blue600
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray600
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900
import com.project.lighthouse.ui.theme.Green600
import com.project.lighthouse.ui.theme.Yellow600

private val ticketStatuses = listOf("open", "in_progress", "resolved", "closed")
private val ticketPriorities = listOf("low", "medium", "high", "urgent")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TicketsScreen(
    state: TicketsState,
    orgId: String?,
    onRefresh: () -> Unit,
    onToggleCreateDialog: (Boolean) -> Unit,
    onUpdateCreateForm: (String?, String?, String?, String?, String?, String?, String?) -> Unit,
    onCreateTicket: (String) -> Unit,
    onToggleUpdateDialog: (Boolean, TicketDto?) -> Unit,
    onUpdateTicket: (String, String?, String?, String?, String?) -> Unit,
    onSetFilter: (String?, String?) -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onNavigateToCreateTicket: () -> Unit,
    onDismissMessage: () -> Unit,
    onViewDetails: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onBulkAssign: (String?) -> Unit,
    onBulkClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh
    )

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            Log.d("TicketsScreen", "Showing error: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
        state.infoMessage?.let {
            Log.d("TicketsScreen", "Showing info: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tickets", style = MaterialTheme.typography.titleLarge) },
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
            FloatingActionButton(onClick = onNavigateToCreateTicket) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_leads),
                    contentDescription = "Create Ticket"
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            when {
                state.isLoading && state.tickets.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                        Text(
                            text = "Loading tickets...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.tickets.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tickets yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Create your first ticket to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // Calculate stats
                    val stats = com.project.lighthouse.ui.tickets.calculateStats(state.tickets)

                    // Filter tickets
                    val filteredTickets = com.project.lighthouse.ui.tickets.filterTickets(
                        tickets = state.tickets,
                        searchQuery = state.searchQuery,
                        statusFilter = state.filterStatus,
                        priorityFilter = state.filterPriority
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Stats Dashboard (clickable to filter)
                        item {
                            com.project.lighthouse.ui.tickets.TicketStatsDashboard(
                                stats = stats,
                                currentFilterStatus = state.filterStatus,
                                onStatusClick = { status -> onSetFilter(status, state.filterPriority) }
                            )
                        }

                        // Search and Filters
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = state.searchQuery,
                                    onValueChange = onSetSearchQuery,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search by ticket ID, subject, customer name, or email...") },
                                    singleLine = true
                                )
                                TicketFilters(
                                    currentStatus = state.filterStatus,
                                    currentPriority = state.filterPriority,
                                    onSetFilter = onSetFilter
                                )
                            }
                        }

                        // Bulk actions bar (shown when tickets are selected and user is admin)
                        if (state.isAdmin && state.selectedTicketIds.isNotEmpty()) {
                            item {
                                BulkActionsBar(
                                    selectedCount = state.selectedTicketIds.size,
                                    assignableEmployees = state.assignableEmployees,
                                    isProcessing = state.isBulkActionInProgress,
                                    onBulkAssign = onBulkAssign,
                                    onBulkClose = onBulkClose,
                                    onClearSelection = onClearSelection
                                )
                            }
                        }

                        // Use a stable, unique key per ticket to avoid LazyColumn key collisions.
                        // Backend guarantees ticketNumber uniqueness, so prefer that over id.
                        items(filteredTickets, key = { it.ticketNumber }) { ticket ->
                            TicketCard(
                                ticket = ticket,
                                isAdmin = state.isAdmin,
                                assignableEmployees = state.assignableEmployees,
                                isSelected = state.selectedTicketIds.contains(ticket.id),
                                onUpdate = { status, priority, assignedTo, category ->
                                    onUpdateTicket(ticket.id, status, priority, assignedTo, category)
                                },
                                onOpenJira = { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("TicketsScreen", "Failed to open Jira URL", e)
                                    }
                                },
                                onToggleUpdateDialog = { onToggleUpdateDialog(true, ticket) },
                                onViewDetails = { onViewDetails(ticket.id) },
                                onToggleSelection = if (state.isAdmin) { { onToggleSelection(ticket.id) } } else null
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (state.showCreateDialog && orgId != null) {
        CreateTicketDialog(
            state = state.createTicketFormState,
            onDismiss = { onToggleCreateDialog(false) },
            onSubmit = { onCreateTicket(orgId) },
            onFieldChange = onUpdateCreateForm
        )
    }

    if (state.showUpdateDialog && state.selectedTicket != null) {
        UpdateTicketDialog(
            ticket = state.selectedTicket!!,
            isAdmin = state.isAdmin,
            assignableEmployees = state.assignableEmployees,
            onDismiss = { onToggleUpdateDialog(false, null) },
            onUpdate = { status, priority, assignedTo, category ->
                onUpdateTicket(state.selectedTicket!!.id, status, priority, assignedTo, category)
            }
        )
    }
}

@Composable
private fun TicketFilters(
    currentStatus: String?,
    currentPriority: String?,
    onSetFilter: (String?, String?) -> Unit
) {
    var statusExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status filter
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { statusExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (currentStatus) {
                        null -> "All"
                        "open" -> "Open"
                        "in_progress" -> "In Progress"
                        "resolved" -> "Resolved"
                        "closed" -> "Closed"
                        else -> currentStatus.replaceFirstChar { it.titlecase() }
                    },
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter(null, currentPriority)
                        statusExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Open", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter("open", currentPriority)
                        statusExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("In Progress", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter("in_progress", currentPriority)
                        statusExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Resolved", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter("resolved", currentPriority)
                        statusExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Closed", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter("closed", currentPriority)
                        statusExpanded = false
                    }
                )
            }
        }

        // Priority filter
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { priorityExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (currentPriority) {
                        null -> "All"
                        "urgent" -> "Urgent"
                        "high" -> "High"
                        "medium" -> "Medium"
                        "low" -> "Low"
                        else -> currentPriority.replaceFirstChar { it.titlecase() }
                    },
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = priorityExpanded,
                onDismissRequest = { priorityExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter(currentStatus, null)
                        priorityExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Urgent", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter(currentStatus, "urgent")
                        priorityExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("High", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter(currentStatus, "high")
                        priorityExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Medium", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter(currentStatus, "medium")
                        priorityExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Low", fontSize = 12.sp) },
                    onClick = {
                        onSetFilter(currentStatus, "low")
                        priorityExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TicketCard(
    ticket: TicketDto,
    isAdmin: Boolean,
    assignableEmployees: List<com.project.lighthouse.data.model.AssignableEmployee>,
    isSelected: Boolean = false,
    onUpdate: (String?, String?, String?, String?) -> Unit,
    onOpenJira: (String) -> Unit,
    onToggleUpdateDialog: () -> Unit,
    onViewDetails: () -> Unit,
    onToggleSelection: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Selection checkbox (admin only)
            if (isAdmin && onToggleSelection != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Ticket number and subject
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.ticketNumber,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = Brand600,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ticket.subject,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Gray900,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusChip(status = ticket.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Customer info
            Text(
                text = "${ticket.name} (${ticket.email})",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Gray700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Priority
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Priority: ${ticket.priority}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Gray700
            )
            // Assigned to
            ticket.assignedToName?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assigned to: $it",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Gray500
                )
            }
            // Jira link
            ticket.jiraIssueUrl?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onOpenJira(url) }
                ) {
                    Text("View in Jira: ${ticket.jiraIssueKey}", fontSize = 11.sp, color = Brand600)
                }
            }
            // Update button
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onToggleUpdateDialog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Ticket", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CreateTicketDialog(
    state: CreateTicketFormState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onFieldChange: (String?, String?, String?, String?, String?, String?, String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Ticket", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onFieldChange(it, null, null, null, null, null, null) },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onFieldChange(null, it, null, null, null, null, null) },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { onFieldChange(null, null, it, null, null, null, null) },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.subject,
                    onValueChange = { onFieldChange(null, null, null, it, null, null, null) },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onFieldChange(null, null, null, null, it, null, null) },
                    label = { Text("Description") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Priority selector
                var priorityExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { priorityExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.priority.replaceFirstChar { it.titlecase() },
                            fontSize = 13.sp
                        )
                    }
                    DropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        ticketPriorities.forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.replaceFirstChar { it.titlecase() }, fontSize = 12.sp) },
                                onClick = {
                                    onFieldChange(null, null, null, null, null, priority, null)
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Category selector
                var categoryExpanded by remember { mutableStateOf(false) }
                val ticketCategories = listOf("technical", "billing", "account", "feature_request", "bug_report", "feedback", "other")
                Box {
                    OutlinedButton(
                        onClick = { categoryExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.category?.replace("_", " ")?.replaceFirstChar { it.titlecase() } ?: "Select category (optional)",
                            fontSize = 13.sp
                        )
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None", fontSize = 12.sp) },
                            onClick = {
                                onFieldChange(null, null, null, null, null, null, null)
                                categoryExpanded = false
                            }
                        )
                        ticketCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.replace("_", " ").replaceFirstChar { it.titlecase() }, fontSize = 12.sp) },
                                onClick = {
                                    onFieldChange(null, null, null, null, null, null, category)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !state.isSubmitting) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BulkActionsBar(
    selectedCount: Int,
    assignableEmployees: List<com.project.lighthouse.data.model.AssignableEmployee>,
    isProcessing: Boolean,
    onBulkAssign: (String?) -> Unit,
    onBulkClose: () -> Unit,
    onClearSelection: () -> Unit
) {
    var showAssignDialog by remember { mutableStateOf(false) }
    
    WebStyleCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$selectedCount ticket(s) selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAssignDialog = true },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Assign", fontSize = 12.sp)
                }
                Button(
                    onClick = onBulkClose,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Gray600)
                ) {
                    Text("Close", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onClearSelection,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
        }
    }
    
    if (showAssignDialog) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Assign Tickets") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onBulkAssign(null)
                            showAssignDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unassigned")
                    }
                    assignableEmployees.forEach { employee ->
                        TextButton(
                            onClick = {
                                onBulkAssign(employee.id)
                                showAssignDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(employee.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UpdateTicketDialog(
    ticket: TicketDto,
    isAdmin: Boolean,
    assignableEmployees: List<com.project.lighthouse.data.model.AssignableEmployee>,
    onDismiss: () -> Unit,
    onUpdate: (String?, String?, String?, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(ticket.status) }
    var selectedPriority by remember { mutableStateOf(ticket.priority) }
    var selectedAssignee by remember { mutableStateOf(ticket.assignedTo) }
    var statusExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var assigneeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Ticket", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Status dropdown
                Box {
                    OutlinedButton(
                        onClick = { statusExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedStatus.replaceFirstChar { it.titlecase() },
                            fontSize = 13.sp
                        )
                    }
                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        ticketStatuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.replaceFirstChar { it.titlecase() }, fontSize = 12.sp) },
                                onClick = {
                                    selectedStatus = status
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority dropdown
                Box {
                    OutlinedButton(
                        onClick = { priorityExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedPriority.replaceFirstChar { it.titlecase() },
                            fontSize = 13.sp
                        )
                    }
                    DropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        ticketPriorities.forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.replaceFirstChar { it.titlecase() }, fontSize = 12.sp) },
                                onClick = {
                                    selectedPriority = priority
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }

                // Assignee dropdown (admin only)
                if (isAdmin) {
                    Box {
                        OutlinedButton(
                            onClick = { assigneeExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = assignableEmployees.find { it.id == selectedAssignee }?.name
                                    ?: "Unassigned",
                                fontSize = 13.sp
                            )
                        }
                        DropdownMenu(
                            expanded = assigneeExpanded,
                            onDismissRequest = { assigneeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unassigned", fontSize = 12.sp) },
                                onClick = {
                                    selectedAssignee = null
                                    assigneeExpanded = false
                                }
                            )
                            assignableEmployees.forEach { employee ->
                                DropdownMenuItem(
                                    text = { Text(employee.name, fontSize = 12.sp) },
                                    onClick = {
                                        selectedAssignee = employee.id
                                        assigneeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onUpdate(
                        if (selectedStatus != ticket.status) selectedStatus else null,
                        if (selectedPriority != ticket.priority) selectedPriority else null,
                        if (selectedAssignee != ticket.assignedTo) selectedAssignee else null,
                        null
                    )
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
