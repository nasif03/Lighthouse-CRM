package com.project.lighthouse.ui.tickets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.data.model.AssignableEmployee
import com.project.lighthouse.data.model.TicketDto
import com.project.lighthouse.ui.common.StatusChip
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Blue100
import com.project.lighthouse.ui.theme.Blue700
import com.project.lighthouse.ui.theme.Gray100
import com.project.lighthouse.ui.theme.Gray200
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900
import com.project.lighthouse.ui.theme.Green100
import com.project.lighthouse.ui.theme.Green700
import com.project.lighthouse.ui.theme.Orange100
import com.project.lighthouse.ui.theme.Orange700
import com.project.lighthouse.ui.theme.Red100
import com.project.lighthouse.ui.theme.Red700
import com.project.lighthouse.ui.theme.Yellow100
import com.project.lighthouse.ui.theme.Yellow700
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    state: TicketDetailState,
    onRefresh: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onUpdatePriority: (String) -> Unit,
    onAssignTicket: (String?) -> Unit,
    onAddComment: (String, Boolean) -> Unit,
    onUpdateNewComment: (String) -> Unit,
    onToggleInternalNote: (Boolean) -> Unit,
    onToggleAssignModal: (Boolean) -> Unit,
    onToggleStatusModal: (Boolean) -> Unit,
    onUpdateSelectedAssignee: (String) -> Unit,
    onUpdateSelectedStatus: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.ticket?.ticketNumber ?: "Ticket Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading && state.ticket == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                    Text(
                        text = "Loading ticket...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (state.ticket != null) {
            // Use single scrollable column for mobile-friendly layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ticket Header
                item {
                    TicketHeaderCard(
                        ticket = state.ticket,
                        isAdmin = state.isAdmin,
                        onStatusClick = { onToggleStatusModal(true) },
                        onAssignClick = { onToggleAssignModal(true) }
                    )
                }

                // Description
                item {
                    TicketDescriptionCard(ticket = state.ticket)
                }

                // Customer Info
                item {
                    CustomerInfoCard(ticket = state.ticket)
                }

                // Assignment
                item {
                    AssignmentCard(
                        ticket = state.ticket,
                        isAdmin = state.isAdmin,
                        onAssignClick = { onToggleAssignModal(true) }
                    )
                }

                // Priority
                item {
                    PriorityCard(
                        ticket = state.ticket,
                        selectedPriority = state.selectedPriority,
                        onPriorityChange = onUpdatePriority
                    )
                }

                // Jira Integration
                item {
                    JiraIntegrationCard(
                        ticket = state.ticket,
                        onCreateJiraIssue = {
                            // TODO: Implement Jira issue creation
                        }
                    )
                }

                // Comments
                item {
                    TicketCommentsCard(
                        comments = state.comments,
                        newComment = state.newComment,
                        isInternalNote = state.isInternalNote,
                        onUpdateComment = onUpdateNewComment,
                        onToggleInternalNote = onToggleInternalNote,
                        onAddComment = { onAddComment(state.newComment, state.isInternalNote) }
                    )
                }
            }
        }
    }

    // Assign Modal
    if (state.showAssignModal) {
        AssignModal(
            employees = state.employees,
            selectedAssignee = state.selectedAssignee,
            onSelectAssignee = onUpdateSelectedAssignee,
            onAssign = { 
                val employeeId = if (state.selectedAssignee == "unassigned") null else state.selectedAssignee
                onAssignTicket(employeeId)
            },
            onDismiss = { onToggleAssignModal(false) },
            isUpdating = state.isUpdating
        )
    }

    // Status Modal
    if (state.showStatusModal) {
        StatusModal(
            selectedStatus = state.selectedStatus,
            onSelectStatus = onUpdateSelectedStatus,
            onUpdate = { onUpdateStatus(state.selectedStatus) },
            onDismiss = { onToggleStatusModal(false) },
            isUpdating = state.isUpdating
        )
    }
}

@Composable
private fun TicketHeaderCard(
    ticket: TicketDto,
    isAdmin: Boolean,
    onStatusClick: () -> Unit,
    onAssignClick: () -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.ticketNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                PriorityChip(priority = ticket.priority)
                StatusChip(status = ticket.status)
                Spacer(modifier = Modifier.weight(1f))
                if (isAdmin) {
                    Button(
                        onClick = onStatusClick,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Change Status", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onAssignClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Assign", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ticket.subject,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ticket.category?.let {
                    Text(
                        text = "Category: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
                Text(
                    text = "Created: ${formatDateTime(ticket.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
                Text(
                    text = "Updated: ${formatDateTime(ticket.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
        }
    }
}

@Composable
private fun TicketDescriptionCard(ticket: TicketDto) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray700
            )
        }
    }
}

@Composable
private fun TicketCommentsCard(
    comments: List<TicketComment>,
    newComment: String,
    isInternalNote: Boolean,
    onUpdateComment: (String) -> Unit,
    onToggleInternalNote: (Boolean) -> Unit,
    onAddComment: () -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Comments & Updates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet. Be the first to add a comment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    comments.forEach { comment ->
                        CommentItem(comment = comment)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Add Comment Form
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Checkbox(
                    checked = isInternalNote,
                    onCheckedChange = onToggleInternalNote
                )
                Text(
                    text = "Internal note (not visible to customer)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray700
                )
            }
            OutlinedTextField(
                value = newComment,
                onValueChange = onUpdateComment,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (isInternalNote) "Add an internal note..." else "Add a comment or reply...") },
                minLines = 4,
                maxLines = 4
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${newComment.length}/5000 characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray400,
                    fontSize = 11.sp
                )
                Button(
                    onClick = onAddComment,
                    enabled = newComment.trim().isNotEmpty()
                ) {
                    Text(if (isInternalNote) "Add Note" else "Add Comment", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: TicketComment) {
    val bgColor = when {
        comment.isInternal -> Yellow100
        comment.authorType == "agent" -> Blue100
        else -> Gray100
    }
    val borderColor = when {
        comment.isInternal -> Yellow700
        comment.authorType == "agent" -> Blue700
        else -> Gray200
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = comment.author,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray900
                    )
                    if (comment.isInternal) {
                        Box(
                            modifier = Modifier
                                .background(Yellow700, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Internal Note",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (comment.authorType == "agent" && !comment.isInternal) {
                        Box(
                            modifier = Modifier
                                .background(Blue700, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Agent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Text(
                    text = formatDateTime(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodySmall,
                color = Gray700
            )
        }
    }
}

@Composable
private fun CustomerInfoCard(ticket: TicketDto) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Customer Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(label = "Name", value = ticket.name)
                InfoRow(label = "Email", value = ticket.email)
                ticket.phone?.let {
                    InfoRow(label = "Phone", value = it)
                }
            }
        }
    }
}

@Composable
private fun AssignmentCard(
    ticket: TicketDto,
    isAdmin: Boolean,
    onAssignClick: () -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Assignment",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = "Assigned To", value = ticket.assignedToName ?: "Unassigned")
            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAssignClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Reassign", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PriorityCard(
    ticket: TicketDto,
    selectedPriority: String,
    onPriorityChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Priority",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityChip(priority = selectedPriority)
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("low", "medium", "high", "urgent").forEach { priority ->
                        DropdownMenuItem(
                            text = { Text(getPriorityLabel(priority)) },
                            onClick = {
                                onPriorityChange(priority)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JiraIntegrationCard(
    ticket: TicketDto,
    onCreateJiraIssue: () -> Unit
) {
    val context = LocalContext.current
    
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Jira Integration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (ticket.jiraIssueKey != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Jira Issue",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    TextButton(
                        onClick = {
                            val url = ticket.jiraIssueUrl ?: "#"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    ) {
                        Text(ticket.jiraIssueKey ?: "", fontSize = 13.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "No Jira issue linked",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Button(
                        onClick = onCreateJiraIssue,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Create Jira Issue", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Gray500,
            fontSize = 12.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray900,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PriorityChip(priority: String) {
    val (bgColor, textColor) = getPriorityColor(priority)
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = getPriorityLabel(priority),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun getPriorityColor(priority: String): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    return when (priority.lowercase()) {
        "low" -> Pair(Gray100, Gray700)
        "medium" -> Pair(Blue100, Blue700)
        "high" -> Pair(Orange100, Orange700)
        "urgent" -> Pair(Red100, Red700)
        else -> Pair(Gray100, Gray700)
    }
}

private fun getPriorityLabel(priority: String): String {
    return when (priority.lowercase()) {
        "low" -> "Low"
        "medium" -> "Medium"
        "high" -> "High"
        "urgent" -> "Urgent"
        else -> priority
    }
}

@Composable
private fun AssignModal(
    employees: List<AssignableEmployee>,
    selectedAssignee: String,
    onSelectAssignee: (String) -> Unit,
    onAssign: () -> Unit,
    onDismiss: () -> Unit,
    isUpdating: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Ticket") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Assign To", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = if (selectedAssignee == "unassigned") "Unassigned" else employees.find { it.id == selectedAssignee }?.name ?: "Unassigned",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unassigned") },
                            onClick = {
                                onSelectAssignee("unassigned")
                                expanded = false
                            }
                        )
                        employees.forEach { employee ->
                            DropdownMenuItem(
                                text = { Text(employee.name) },
                                onClick = {
                                    onSelectAssignee(employee.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAssign, enabled = !isUpdating) {
                Text(if (isUpdating) "Assigning..." else "Assign")
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
private fun StatusModal(
    selectedStatus: String,
    onSelectStatus: (String) -> Unit,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    isUpdating: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Status", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = getStatusLabel(selectedStatus),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("open", "in_progress", "resolved", "closed").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(getStatusLabel(status)) },
                                onClick = {
                                    onSelectStatus(status)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdate, enabled = !isUpdating) {
                Text(if (isUpdating) "Updating..." else "Update Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getStatusLabel(status: String): String {
    return when (status.lowercase()) {
        "open" -> "Open"
        "in_progress" -> "In Progress"
        "resolved" -> "Resolved"
        "closed" -> "Closed"
        else -> status
    }
}

private fun formatDateTime(dateString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateString)
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        if (date != null) {
            SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

