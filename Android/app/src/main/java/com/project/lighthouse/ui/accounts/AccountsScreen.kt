package com.project.lighthouse.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.AccountDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    state: AccountsState,
    onRefresh: () -> Unit,
    onSubmitForm: () -> Unit,
    onUpdateForm: (String?, String?, String?, String?, String?) -> Unit,
    onToggleDialog: (Boolean, String?) -> Unit,
    onDeleteAccount: (String) -> Unit,
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
                title = { Text("Accounts") },
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
            FloatingActionButton(onClick = { onToggleDialog(true, null) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_accounts),
                    contentDescription = "Add Account"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading && state.accounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = "Loading accounts...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (state.accounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_accounts),
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No accounts yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Create your first account to get started",
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        onEdit = { onToggleDialog(true, account.id) },
                        onDelete = { onDeleteAccount(account.id) },
                        isBusy = state.actionInProgress == account.id
                    )
                }
            }
        }
    }

    if (state.showCreateDialog) {
        AccountDialog(
            state = state.formState,
            onDismiss = { onToggleDialog(false, null) },
            onSubmit = onSubmitForm,
            onFieldChange = onUpdateForm
        )
    }
}

@Composable
private fun AccountCard(
    account: AccountDto,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            account.domain?.takeIf { it.isNotBlank() }?.let {
                Text("Domain: $it", style = MaterialTheme.typography.bodyMedium)
            }
            account.industry?.takeIf { it.isNotBlank() }?.let {
                Text("Industry: $it", style = MaterialTheme.typography.bodyMedium)
            }
            account.phone?.takeIf { it.isNotBlank() }?.let {
                Text("Phone: $it", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Status: ${account.status}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.padding(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onEdit, enabled = !isBusy) {
                    Text("Edit")
                }
                TextButton(
                    onClick = onDelete,
                    enabled = !isBusy,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun AccountDialog(
    state: AccountFormState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onFieldChange: (String?, String?, String?, String?, String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.editingAccountId == null) "New Account" else "Edit Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onFieldChange(it, null, null, null, null) },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.domain,
                    onValueChange = { onFieldChange(null, it, null, null, null) },
                    label = { Text("Domain") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.industry,
                    onValueChange = { onFieldChange(null, null, it, null, null) },
                    label = { Text("Industry") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { onFieldChange(null, null, null, it, null) },
                    label = { Text("Phone") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.status,
                    onValueChange = { onFieldChange(null, null, null, null, it) },
                    label = { Text("Status") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !state.isSubmitting) {
                Text(if (state.editingAccountId == null) "Create" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

