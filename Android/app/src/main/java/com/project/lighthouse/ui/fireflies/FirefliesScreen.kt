package com.project.lighthouse.ui.fireflies

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.FirefliesTranscript
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirefliesScreen(
    state: FirefliesState,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    onSelectTranscript: (FirefliesTranscript?) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                title = {
                    Text(
                        text = "Fireflies AI",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading && state.transcripts.isEmpty()) {
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
                        text = "Loading transcripts...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Controls and Details Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Controls Card
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            ControlsCard(
                                state = state,
                                onRefresh = onRefresh,
                                onSync = onSync
                            )
                        }

                        // Details Card
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            TranscriptDetailsCard(
                                transcript = state.selectedTranscript,
                                onOpenTranscript = { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }

                // Transcripts List
                item {
                    Text(
                        text = "Recent transcripts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (state.transcripts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transcripts found. Use \"Sync from Fireflies\" to pull your recent meetings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray500
                            )
                        }
                    }
                } else {
                    items(state.transcripts, key = { it.id }) { transcript ->
                        TranscriptItem(
                            transcript = transcript,
                            isSelected = state.selectedTranscript?.id == transcript.id,
                            onClick = { onSelectTranscript(transcript) },
                            onOpenTranscript = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlsCard(
    state: FirefliesState,
    onRefresh: () -> Unit,
    onSync: () -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Fireflies AI transcripts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pull meeting summaries from Fireflies AI into Lighthouse. Use this panel to sync the latest conversations and review AI-generated notes.",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing && !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (state.isRefreshing) "Refreshing..." else "Refresh list",
                        fontSize = 13.sp
                    )
                }
                Button(
                    onClick = onSync,
                    enabled = !state.isSyncing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = if (state.isSyncing) "Syncing..." else "Sync from Fireflies",
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                state.lastFetchedAt?.let {
                    Text(
                        text = "Last refreshed: ${formatDate(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray400,
                        fontSize = 11.sp
                    )
                }
                state.lastSyncedAt?.let {
                    Text(
                        text = "Last synced: ${formatDate(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray400,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptDetailsCard(
    transcript: FirefliesTranscript?,
    onOpenTranscript: (String) -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Transcript details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (transcript != null) {
                Text(
                    text = transcript.title ?: "Untitled meeting",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(transcript.date * 1000),
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "OVERVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray500,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transcript.summary?.overview ?: "No overview provided.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray900
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "KEY POINTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray500,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transcript.summary?.shortSummary ?: "No highlights provided.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray900
                )
                transcript.transcriptUrl?.let { url ->
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onOpenTranscript(url) }) {
                        Text("View full transcript", fontSize = 13.sp)
                    }
                }
            } else {
                Text(
                    text = "Select a transcript from the list to see AI-generated summaries and open the full Fireflies document.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
        }
    }
}

@Composable
private fun TranscriptItem(
    transcript: FirefliesTranscript,
    isSelected: Boolean,
    onClick: () -> Unit,
    onOpenTranscript: (String) -> Unit
) {
    WebStyleCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = transcript.title ?: "Untitled meeting",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                fontWeight = FontWeight.SemiBold,
                color = Gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatDate(transcript.date * 1000),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Gray500
            )
            transcript.summary?.overview?.let { overview ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = truncate(overview, 140),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Gray500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            transcript.transcriptUrl?.let { url ->
                TextButton(onClick = { onOpenTranscript(url) }) {
                    Text("Open", fontSize = 12.sp)
                }
            } ?: run {
                Text(
                    text = "No link",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray400,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    return format.format(date)
}

private fun truncate(text: String, length: Int): String {
    return if (text.length > length) {
        "${text.take(length)}…"
    } else {
        text
    }
}

