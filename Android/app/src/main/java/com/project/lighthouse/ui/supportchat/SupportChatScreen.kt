package com.project.lighthouse.ui.supportchat

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.data.model.SupportChatMessage
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    state: SupportChatState,
    onUpdateMessageInput: (String) -> Unit,
    onSendMessage: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            Log.d("SupportChatScreen", "Showing error: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
        state.infoMessage?.let {
            Log.d("SupportChatScreen", "Showing info: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Support Assistant", style = MaterialTheme.typography.titleLarge) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Messages list
            if (state.isLoading && state.messages.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = listState
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isUser = message.role == "user"
                        )
                    }
                    if (state.isSending) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    modifier = Modifier.widthIn(max = 300.dp),
                                    colors = CardDefaults.cardColors(containerColor = Gray400),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Thinking...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                            color = Gray900
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = state.messageInput,
                    onValueChange = onUpdateMessageInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask me anything about your CRM...", fontSize = 14.sp) },
                    singleLine = false,
                    maxLines = 4,
                    minLines = 1
                )
                IconButton(
                    onClick = onSendMessage,
                    enabled = state.messageInput.trim().isNotBlank() && !state.isSending
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Brand600
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: SupportChatMessage,
    isUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp, min = 100.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Brand600 else Gray400
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = if (isUser) Color.White else Gray900,
                    lineHeight = 20.sp
                )
                message.createdAt?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatMessageTime(it),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isUser) Color.White.copy(alpha = 0.8f) else Gray500
                    )
                }
            }
        }
    }
}

private fun formatMessageTime(timeString: String): String {
    return try {
        val instant = Instant.parse(timeString)
        val now = Instant.now()
        val diffInSeconds = (now.epochSecond - instant.epochSecond)

        when {
            diffInSeconds < 60 -> "Just now"
            diffInSeconds < 3600 -> "${diffInSeconds / 60}m ago"
            diffInSeconds < 86400 -> "${diffInSeconds / 3600}h ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US)
                instant.atZone(ZoneId.systemDefault()).format(formatter)
            }
        }
    } catch (e: Exception) {
        timeString
    }
}

