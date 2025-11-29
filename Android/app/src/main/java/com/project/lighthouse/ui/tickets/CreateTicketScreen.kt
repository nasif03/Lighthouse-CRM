package com.project.lighthouse.ui.tickets

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900

private const val MAX_SUBJECT_LENGTH = 200
private const val MAX_DESCRIPTION_LENGTH = 5000

private val categories = listOf(
    "technical",
    "billing",
    "account",
    "feature_request",
    "bug_report",
    "feedback",
    "other"
)

private val priorities = listOf("low", "medium", "high", "urgent")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketScreen(
    state: CreateTicketFormState,
    orgId: String?,
    userName: String?,
    userEmail: String?,
    errorMessage: String?,
    infoMessage: String?,
    onUpdateField: (String?, String?, String?, String?, String?, String?, String?) -> Unit,
    onSubmit: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToTicket: (String) -> Unit,
    onDismissMessage: () -> Unit,
    createdTicketId: String?,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage, infoMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
        infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    // Navigate to ticket detail after successful creation
    LaunchedEffect(createdTicketId) {
        createdTicketId?.let { ticketId ->
            onNavigateToTicket(ticketId)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Create Support Ticket", style = MaterialTheme.typography.titleLarge) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Fill out the form below to submit a support request",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500,
                fontSize = 14.sp
            )

            WebStyleCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Subject
                    OutlinedTextField(
                        value = state.subject,
                        onValueChange = { value ->
                            if (value.length <= MAX_SUBJECT_LENGTH) {
                                onUpdateField(null, null, null, value, null, null, null)
                            }
                        },
                        label = { Text("Subject / Title") },
                        placeholder = { Text("Brief summary of your issue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.subject.isEmpty() && state.isSubmitting
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${state.subject.length}/$MAX_SUBJECT_LENGTH characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray400,
                            fontSize = 11.sp
                        )
                    }

                    // Description
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = { value ->
                            if (value.length <= MAX_DESCRIPTION_LENGTH) {
                                onUpdateField(null, null, null, null, value, null, null)
                            }
                        },
                        label = { Text("Description / Details") },
                        placeholder = { Text("Please provide detailed information about your issue...") },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.description.isEmpty() && state.isSubmitting
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${state.description.length}/$MAX_DESCRIPTION_LENGTH characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray400,
                            fontSize = 11.sp
                        )
                    }

                    // Category and Priority Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Category
                        var categoryExpanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Category / Type",
                                style = MaterialTheme.typography.labelMedium,
                                color = Gray700,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { categoryExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.category?.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.titlecase() } ?: "Select category (optional)",
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                                DropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None", fontSize = 12.sp) },
                                        onClick = {
                                            onUpdateField(null, null, null, null, null, null, "")
                                            categoryExpanded = false
                                        }
                                    )
                                    categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category.replace("_", " ").replaceFirstChar { it.titlecase() }, fontSize = 12.sp) },
                                            onClick = {
                                                onUpdateField(null, null, null, null, null, null, category)
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Priority
                        var priorityExpanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Priority / Severity",
                                style = MaterialTheme.typography.labelMedium,
                                color = Gray700,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
                                    priorities.forEach { priority ->
                                        DropdownMenuItem(
                                            text = { Text(priority.replaceFirstChar { it.titlecase() }, fontSize = 12.sp) },
                                            onClick = {
                                                onUpdateField(null, null, null, null, null, priority, null)
                                                priorityExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Contact Information Section
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Contact Information",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray900,
                        fontSize = 14.sp
                    )

                    // Contact Name
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onUpdateField(it, null, null, null, null, null, null) },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.name.isEmpty() && state.isSubmitting
                    )

                    // Contact Email
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onUpdateField(null, it, null, null, null, null, null) },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.email.isEmpty() && state.isSubmitting
                    )

                    // Contact Phone
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = { onUpdateField(null, null, it, null, null, null, null) },
                        label = { Text("Phone") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Submit Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSubmitting
                ) {
                    Text("Cancel", fontSize = 14.sp)
                }
                Button(
                    onClick = { if (orgId != null) onSubmit(orgId) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSubmitting && orgId != null && state.subject.isNotBlank() && state.description.isNotBlank() && state.name.isNotBlank() && state.email.isNotBlank()
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(16.dp)
                                .fillMaxWidth(0.3f),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit Ticket", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

