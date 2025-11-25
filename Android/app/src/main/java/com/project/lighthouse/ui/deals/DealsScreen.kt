package com.project.lighthouse.ui.deals

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.DealDto
import com.project.lighthouse.ui.common.StatusChip
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900
import java.text.NumberFormat
import java.util.Locale

private val dealStages = listOf(
    "prospecting",
    "qualification",
    "proposal",
    "negotiation",
    "closed-won",
    "closed-lost"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealsScreen(
    state: DealsState,
    onRefresh: () -> Unit,
    onCreateDeal: () -> Unit,
    onUpdateForm: (String?, String?, String?) -> Unit,
    onToggleDialog: (Boolean) -> Unit,
    onUpdateStage: (String, String, String?) -> Unit,
    onDeleteDeal: (String) -> Unit,
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
                title = { Text("Deals", style = MaterialTheme.typography.titleLarge) },
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
                    painter = painterResource(id = R.drawable.ic_deals),
                    contentDescription = "Add Deal"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading && state.deals.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = "Loading deals...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (state.deals.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_deals),
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No deals yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Create your first deal to get started",
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
                items(state.deals, key = { it.id }) { deal ->
                    DealCard(
                        deal = deal,
                        isBusy = state.actionInProgress == deal.id,
                        onUpdateStage = { stageId ->
                            onUpdateStage(deal.id, stageId, stageId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                        },
                        onDelete = { onDeleteDeal(deal.id) }
                    )
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateDealDialog(
            state = state.formState,
            onDismiss = { onToggleDialog(false) },
            onSubmit = onCreateDeal,
            onFieldChange = onUpdateForm
        )
    }
}

@Composable
private fun DealCard(
    deal: DealDto,
    isBusy: Boolean,
    onUpdateStage: (String) -> Unit,
    onDelete: () -> Unit
) {
    var stageMenuExpanded by remember { mutableStateOf(false) }
    
    val formattedAmount = deal.amount?.let {
        NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }.format(it)
    } ?: "N/A"

    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Name
            Text(
                text = deal.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                fontWeight = FontWeight.SemiBold,
                color = Gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Amount
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = Gray700
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stage Chip
            StatusChip(status = deal.stageId)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { stageMenuExpanded = true },
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600)
                ) {
                    Text("Change Stage", fontSize = 13.sp)
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
            
            // Stage Dropdown
            DropdownMenu(expanded = stageMenuExpanded, onDismissRequest = { stageMenuExpanded = false }) {
                dealStages.forEach { stage ->
                    DropdownMenuItem(
                        text = { Text(stage.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                        onClick = {
                            stageMenuExpanded = false
                            onUpdateStage(stage)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateDealDialog(
    state: DealFormState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onFieldChange: (String?, String?, String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Deal", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onFieldChange(it, null, null) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { onFieldChange(null, it, null) },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DealStageDropdown(
                    selectedStage = state.stageId,
                    onStageSelected = { onFieldChange(null, null, it) }
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

@Composable
private fun DealStageDropdown(
    selectedStage: String,
    onStageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Text("Stage: ${selectedStage.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            dealStages.forEach { stage ->
                DropdownMenuItem(
                    text = { Text(stage.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                    onClick = {
                        expanded = false
                        onStageSelected(stage)
                    }
                )
            }
        }
    }
}
