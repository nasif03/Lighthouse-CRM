package com.project.lighthouse.ui.leads

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.LeadDto
import com.project.lighthouse.ui.common.StatusChip
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900

private val leadStatuses = listOf("new", "contacted", "qualified", "converted", "lost")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    state: LeadsState,
    onRefresh: () -> Unit,
    onCreateLead: () -> Unit,
    onUpdateForm: (String?, String?, String?, String?) -> Unit,
    onToggleDialog: (Boolean) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onConvertLead: (String) -> Unit,
    onDeleteLead: (String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Leads", style = MaterialTheme.typography.titleLarge) },
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
            FloatingActionButton(onClick = { onToggleDialog(true) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_leads),
                    contentDescription = "Add Lead"
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading && state.leads.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = "Loading leads...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (state.leads.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_leads),
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No leads yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Create your first lead to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.leads, key = { it.id }) { lead ->
                    LeadCard(
                        lead = lead,
                        isBusy = state.actionInProgress == lead.id,
                        onUpdateStatus = { status -> onUpdateStatus(lead.id, status) },
                        onConvert = { onConvertLead(lead.id) },
                        onDelete = { onDeleteLead(lead.id) }
                    )
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateLeadDialog(
            state = state.formState,
            onDismiss = { onToggleDialog(false) },
            onSubmit = onCreateLead,
            onFieldChange = onUpdateForm
        )
    }
}

@Composable
private fun LeadCard(
    lead: LeadDto,
    isBusy: Boolean,
    onUpdateStatus: (String) -> Unit,
    onConvert: () -> Unit,
    onDelete: () -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Name
            Text(
                text = lead.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                fontWeight = FontWeight.SemiBold,
                color = Gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Email
            lead.email.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Phone
            lead.phone?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Source
            lead.source?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Source: $it",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Gray400
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status Chip
            StatusChip(status = lead.status)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status Dropdown
            StatusDropdownMenu(
                currentStatus = lead.status,
                isBusy = isBusy,
                onStatusSelected = onUpdateStatus
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConvert,
                    enabled = !isBusy && lead.status != "converted",
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600)
                ) {
                    Text("Convert", fontSize = 13.sp)
                }
                Button(
                    onClick = onDelete,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Delete", fontSize = 13.sp, color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun StatusDropdownMenu(
    currentStatus: String,
    isBusy: Boolean,
    onStatusSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentStatusDisplay = currentStatus.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
    }

    Log.d("LeadsScreen", "StatusDropdownMenu: currentStatus=$currentStatus, expanded=$expanded")

    Box {
        OutlinedButton(
            onClick = { 
                Log.d("LeadsScreen", "Status dropdown clicked, expanding")
                expanded = true 
            },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentStatusDisplay,
                    fontSize = 13.sp,
                    color = if (isBusy) Gray400 else Brand600
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Status dropdown",
                    tint = if (isBusy) Gray400 else Brand600
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { 
                Log.d("LeadsScreen", "Status dropdown dismissed")
                expanded = false 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            leadStatuses.forEach { status ->
                val statusDisplay = status.replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = statusDisplay,
                            fontSize = 13.sp,
                            color = if (currentStatus == status) Brand600 else Gray900
                        )
                    },
                    onClick = {
                        Log.d("LeadsScreen", "Status selected: $status")
                        expanded = false
                        if (currentStatus != status) {
                            onStatusSelected(status)
                        }
                    },
                    enabled = !isBusy && currentStatus != status
                )
            }
        }
    }
}

@Composable
private fun CreateLeadDialog(
    state: LeadFormState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onFieldChange: (String?, String?, String?, String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Lead", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onFieldChange(it, null, null, null) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onFieldChange(null, it, null, null) },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { onFieldChange(null, null, it, null) },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.source,
                    onValueChange = { onFieldChange(null, null, null, it) },
                    label = { Text("Source") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
