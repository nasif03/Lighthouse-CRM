package com.project.lighthouse.ui.contacts

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
import androidx.compose.material3.ButtonDefaults
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
import com.project.lighthouse.data.model.ContactDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    state: ContactsState,
    onRefresh: () -> Unit,
    onCreateContact: () -> Unit,
    onUpdateForm: (String?, String?, String?, String?, String?) -> Unit,
    onToggleDialog: (Boolean) -> Unit,
    onDeleteContact: (String) -> Unit,
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
                title = { Text("Contacts") },
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
                    painter = painterResource(id = R.drawable.ic_contacts),
                    contentDescription = "Add Contact"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading && state.contacts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = "Loading contacts...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (state.contacts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_contacts),
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No contacts yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Create your first contact to get started",
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
                items(state.contacts, key = { it.id }) { contact ->
                    ContactCard(
                        contact = contact,
                        onDelete = { onDeleteContact(contact.id) },
                        isBusy = state.actionInProgress == contact.id
                    )
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateContactDialog(
            state = state.formState,
            onDismiss = { onToggleDialog(false) },
            onSubmit = onCreateContact,
            onFieldChange = onUpdateForm
        )
    }
}

@Composable
private fun ContactCard(
    contact: ContactDto,
    isBusy: Boolean,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${contact.firstName.orEmpty()} ${contact.lastName.orEmpty()}".trim().ifBlank { contact.email },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(contact.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            contact.phone?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            contact.title?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.padding(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
private fun CreateContactDialog(
    state: ContactFormState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onFieldChange: (String?, String?, String?, String?, String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = { onFieldChange(it, null, null, null, null) },
                    label = { Text("First Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = { onFieldChange(null, it, null, null, null) },
                    label = { Text("Last Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { onFieldChange(null, null, it, null, null) },
                    label = { Text("Email") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { onFieldChange(null, null, null, it, null) },
                    label = { Text("Phone") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onFieldChange(null, null, null, null, it) },
                    label = { Text("Title") },
                    singleLine = true
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

