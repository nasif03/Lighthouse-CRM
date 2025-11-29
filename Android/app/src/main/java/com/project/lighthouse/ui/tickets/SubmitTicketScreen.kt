package com.project.lighthouse.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Brand700
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900
import com.project.lighthouse.ui.theme.Green100
import com.project.lighthouse.ui.theme.Green600

private val priorities = listOf("low", "medium", "high", "urgent")
private val categories = listOf("technical", "billing", "account", "feature_request", "bug_report", "feedback", "other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitTicketScreen(
    state: SubmitTicketState,
    onUpdateField: (String, String) -> Unit,
    onSubmitTicket: () -> Unit,
    onResetSuccess: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Submit a Support Ticket", style = MaterialTheme.typography.titleLarge) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.success) {
                SuccessView(
                    ticketNumber = state.ticketNumber,
                    onReset = onResetSuccess,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                TicketForm(
                    state = state,
                    onUpdateField = onUpdateField,
                    onSubmitTicket = onSubmitTicket,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SuccessView(
    ticketNumber: String?,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WebStyleCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth(0.3f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Green600,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = "Ticket Submitted Successfully!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gray900
                )
                Text(
                    text = "Your support ticket has been created and our team will get back to you soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray700,
                    modifier = Modifier.fillMaxWidth()
                )
                ticketNumber?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6FF)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your Ticket Number:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Brand700
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please save this number for future reference",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray400,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Another Ticket", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun TicketForm(
    state: SubmitTicketState,
    onUpdateField: (String, String) -> Unit,
    onSubmitTicket: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Fill out the form below and we'll get back to you as soon as possible.",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
            fontSize = 14.sp
        )

        WebStyleCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onUpdateField("name", it) },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.name.isEmpty() && state.isSubmitting
                )

                // Email
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onUpdateField("email", it) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.email.isEmpty() && state.isSubmitting
                )

                // Phone
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { onUpdateField("phone", it) },
                    label = { Text("Phone (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Subject
                OutlinedTextField(
                    value = state.subject,
                    onValueChange = { onUpdateField("subject", it) },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.subject.isEmpty() && state.isSubmitting
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${state.subject.length}/200 characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray400,
                        fontSize = 11.sp
                    )
                }

                // Description
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onUpdateField("description", it) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 10,
                    isError = state.description.isEmpty() && state.isSubmitting
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${state.description.length}/5000 characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray400,
                        fontSize = 11.sp
                    )
                }

                // Priority and Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Priority
                    var priorityExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Priority",
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
                                            onUpdateField("priority", priority)
                                            priorityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Category
                    var categoryExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Category (optional)",
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
                                    text = state.category.ifEmpty { "Select category" },
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
                                        onUpdateField("category", "")
                                        categoryExpanded = false
                                    }
                                )
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.replace("_", " ").replaceFirstChar { it.titlecase() }, fontSize = 12.sp) },
                                        onClick = {
                                            onUpdateField("category", category)
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = onSubmitTicket,
                    enabled = !state.isSubmitting && state.name.isNotBlank() && state.email.isNotBlank() && state.subject.isNotBlank() && state.description.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp),
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

