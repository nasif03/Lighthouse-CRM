package com.project.lighthouse.ui.chat

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.data.model.ChatChannel
import com.project.lighthouse.data.model.ChatMessage
import com.project.lighthouse.data.model.ChatUser
import com.project.lighthouse.ui.common.WebStyleCard
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
fun ChatScreen(
    state: ChatState,
    onRefreshChannels: () -> Unit,
    onSelectChannel: (ChatChannel) -> Unit,
    onToggleUserSelection: (Boolean) -> Unit,
    onCreateChannelWithUser: (String) -> Unit,
    onUpdateMessageInput: (String) -> Unit,
    onSendMessage: () -> Unit,
    onGoBackToChannelList: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            Log.d("ChatScreen", "Showing error: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
        state.infoMessage?.let {
            Log.d("ChatScreen", "Showing info: $it")
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    // Auto-scroll to bottom when new messages arrive
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.selectedChannel != null) {
                            state.selectedChannel.name ?: state.selectedChannel.otherMember?.name ?: "Chat"
                        } else if (state.showUserSelection) {
                            "Select User"
                        } else {
                            "Chat"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (state.selectedChannel != null || state.showUserSelection) {
                        IconButton(onClick = {
                            if (state.showUserSelection) {
                                onToggleUserSelection(false)
                            } else {
                                onGoBackToChannelList()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (state.showChannelList && !state.showUserSelection) {
                        IconButton(onClick = { onToggleUserSelection(true) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when {
            state.showUserSelection -> {
                UserSelectionView(
                    users = state.availableUsers,
                    isLoading = state.isLoading,
                    onUserSelected = { userId ->
                        onCreateChannelWithUser(userId)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            state.selectedChannel != null -> {
                ConversationView(
                    channel = state.selectedChannel,
                    messages = state.messages,
                    messageInput = state.messageInput,
                    isSending = state.isSendingMessage,
                    isLoading = state.isLoading,
                    onUpdateMessageInput = onUpdateMessageInput,
                    onSendMessage = onSendMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                ChannelListView(
                    channels = state.channels,
                    isLoading = state.isLoading,
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefreshChannels,
                    onChannelSelected = onSelectChannel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ChannelListView(
    channels: List<ChatChannel>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onChannelSelected: (ChatChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && channels.isEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            Text(
                text = "Loading channels...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else if (channels.isEmpty()) {
        Column(
            modifier = modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No conversations yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Start a new conversation to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelSelected(channel) }
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: ChatChannel,
    onClick: () -> Unit
) {
    WebStyleCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            val displayName = channel.name ?: channel.otherMember?.name
            val displayInitial = displayName?.take(1)?.uppercase() ?: "?"
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brand600),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayInitial,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName ?: "Direct Message",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Gray900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                channel.lastMessage?.let { message ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Gray700,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }
            if (channel.unreadCount > 0) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Brand600),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationView(
    channel: ChatChannel,
    messages: List<ChatMessage>,
    messageInput: String,
    isSending: Boolean,
    isLoading: Boolean,
    onUpdateMessageInput: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Messages list
        if (isLoading && messages.isEmpty()) {
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
                reverseLayout = false
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message, isOwn = true) // TODO: Check if message is from current user
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
                value = messageInput,
                onValueChange = onUpdateMessageInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...", fontSize = 14.sp) },
                singleLine = false,
                maxLines = 4,
                minLines = 1
            )
            IconButton(
                onClick = onSendMessage,
                enabled = messageInput.trim().isNotBlank() && !isSending
            ) {
                if (isSending) {
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

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isOwn: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp, min = 100.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOwn) Brand600 else Gray400
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwn) 16.dp else 4.dp,
                bottomEnd = if (isOwn) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = if (isOwn) Color.White else Gray900,
                    lineHeight = 20.sp
                )
                message.createdAt?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatMessageTime(it),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isOwn) Color.White.copy(alpha = 0.8f) else Gray500
                    )
                }
            }
        }
    }
}

@Composable
private fun UserSelectionView(
    users: List<ChatUser>,
    isLoading: Boolean,
    onUserSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (users.isEmpty()) {
        Column(
            modifier = modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No users available",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    onClick = { onUserSelected(user.id) }
                )
            }
        }
    }
}

@Composable
private fun UserCard(
    user: ChatUser,
    onClick: () -> Unit
) {
    WebStyleCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brand600),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Gray900
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Gray700
                )
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

