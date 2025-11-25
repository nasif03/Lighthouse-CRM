package com.project.lighthouse.ui.meetings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AssistChip
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
import com.project.lighthouse.data.model.FirefliesTranscript
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    state: MeetingsState,
    onRefresh: () -> Unit,
    onToggleCreateMeetingDialog: (Boolean) -> Unit,
    onUpdateCreateMeetingForm: (String?, String?, String?, String?, List<String>?, String?) -> Unit,
    onAddAttendee: (String) -> Unit,
    onRemoveAttendee: (String) -> Unit,
    onCreateMeeting: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            Log.d("MeetingsScreen", "Showing error: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
        state.infoMessage?.let {
            Log.d("MeetingsScreen", "Showing info: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    // Open meeting link when created
    LaunchedEffect(state.createdMeeting) {
        state.createdMeeting?.hangoutLink?.let { link ->
            Log.d("MeetingsScreen", "Opening meeting link: $link")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("MeetingsScreen", "Failed to open meeting link", e)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Meetings", style = MaterialTheme.typography.titleLarge) },
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
            FloatingActionButton(onClick = { onToggleCreateMeetingDialog(true) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_leads), // Using leads icon as placeholder
                    contentDescription = "Create Meeting"
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading && state.transcripts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = "Loading meetings...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (state.transcripts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No meeting transcripts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Create a meeting or wait for transcripts to sync",
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
                items(state.transcripts, key = { it.id }) { transcript ->
                    MeetingTranscriptCard(transcript = transcript)
                }
            }
        }
    }

    if (state.showCreateMeetingDialog) {
        CreateMeetingDialog(
            state = state.createMeetingFormState,
            onDismiss = { onToggleCreateMeetingDialog(false) },
            onSubmit = onCreateMeeting,
            onFieldChange = onUpdateCreateMeetingForm,
            onAddAttendee = onAddAttendee,
            onRemoveAttendee = onRemoveAttendee
        )
    }
}

@Composable
private fun MeetingTranscriptCard(transcript: FirefliesTranscript) {
    val dateTime = try {
        Instant.ofEpochSecond(transcript.date)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
    } catch (e: Exception) {
        "Unknown date"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            Text(
                text = transcript.title ?: "Untitled Meeting",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.SemiBold,
                color = Gray900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Date
            Text(
                text = dateTime,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Gray500
            )
            // Summary
            transcript.summary?.shortSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Gray700,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Transcript URL
            transcript.transcriptUrl?.takeIf { it.isNotBlank() }?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                val context = LocalContext.current
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("MeetingsScreen", "Failed to open transcript URL", e)
                        }
                    }
                ) {
                    Text("View Transcript", fontSize = 12.sp, color = Brand600)
                }
            }
        }
    }
}

@Composable
private fun CreateMeetingDialog(
    state: CreateMeetingFormState,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onFieldChange: (String?, String?, String?, String?, List<String>?, String?) -> Unit,
    onAddAttendee: (String) -> Unit,
    onRemoveAttendee: (String) -> Unit
) {
    var newAttendeeEmail by remember { androidx.compose.runtime.mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Meeting", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onFieldChange(it, null, null, null, null, null) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.startTime,
                    onValueChange = { onFieldChange(null, it, null, null, null, null) },
                    label = { Text("Start Time (ISO 8601)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.endTime,
                    onValueChange = { onFieldChange(null, null, it, null, null, null) },
                    label = { Text("End Time (ISO 8601)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onFieldChange(null, null, null, it, null, null) },
                    label = { Text("Description") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                // Attendees
                Text("Attendees", style = MaterialTheme.typography.labelMedium, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newAttendeeEmail,
                        onValueChange = { newAttendeeEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        onAddAttendee(newAttendeeEmail)
                        newAttendeeEmail = ""
                    }) {
                        Text("Add", fontSize = 12.sp)
                    }
                }
                // Attendee chips
                if (state.attendees.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.attendees.forEach { email ->
                            AssistChip(
                                onClick = { onRemoveAttendee(email) },
                                label = { Text(email, fontSize = 11.sp) }
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

