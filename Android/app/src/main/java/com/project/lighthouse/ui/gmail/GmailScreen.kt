package com.project.lighthouse.ui.gmail

import android.util.Log
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.GmailMessage
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailScreen(
    state: GmailState,
    onRefresh: () -> Unit,
    onAuthenticate: (String?, String?, String?) -> Unit,
    onToggleSendEmailDialog: (Boolean) -> Unit,
    onUpdateSendEmailForm: (String?, String?, String?) -> Unit,
    onSendEmail: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            Log.d("GmailScreen", "Showing error: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
        state.infoMessage?.let {
            Log.d("GmailScreen", "Showing info: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Gmail", style = MaterialTheme.typography.titleLarge) },
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
            if (state.isAuthenticated) {
                FloatingActionButton(onClick = { onToggleSendEmailDialog(true) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_leads), // Using leads icon as placeholder
                        contentDescription = "Send Email"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading && state.messages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = "Loading Gmail...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (!state.isAuthenticated) {
            // Authentication required
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Gmail Not Connected",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Connect your Gmail account to view and send emails",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = {
                        Log.d("GmailScreen", "Authenticate button clicked")
                        state.authorizationUrl?.let { url ->
                            // In a real app, you'd open this URL in a browser or WebView
                            Log.d("GmailScreen", "Authorization URL: $url")
                            // For now, just trigger authentication with empty params
                            // The backend should handle OAuth flow
                            onAuthenticate(null, null, null)
                        } ?: run {
                            onAuthenticate(null, null, null)
                        }
                    }
                ) {
                    Text("Connect Gmail")
                }
            }
        } else if (state.messages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No messages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Your Gmail inbox is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                items(state.messages, key = { it.id }) { message ->
                    GmailMessageCard(message = message)
                }
            }
        }
    }

    if (state.showSendEmailDialog) {
        SendEmailDialog(
            state = state.sendEmailFormState,
            onDismiss = { onToggleSendEmailDialog(false) },
            onSubmit = onSendEmail,
            onFieldChange = onUpdateSendEmailForm
        )
    }
}

@Composable
private fun GmailMessageCard(message: GmailMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Subject
            Text(
                text = message.subject.ifBlank { "(No subject)" },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.SemiBold,
                color = Gray900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            // From
            Text(
                text = "From: ${message.from}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Gray700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Date
            Text(
                text = message.date,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Gray500
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Snippet
            Text(
                text = message.snippet.ifBlank { "(No preview)" },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Gray700,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SendEmailDialog(
    state: SendEmailFormState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onFieldChange: (String?, String?, String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Email", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.to,
                    onValueChange = { onFieldChange(it, null, null) },
                    label = { Text("To") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.subject,
                    onValueChange = { onFieldChange(null, it, null) },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.body,
                    onValueChange = { onFieldChange(null, null, it) },
                    label = { Text("Body") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !state.isSubmitting) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

