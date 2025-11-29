package com.project.lighthouse.ui.calendar

import android.content.Intent
import android.net.Uri
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
import com.project.lighthouse.data.model.CalendarMeeting
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray900
import com.project.lighthouse.ui.theme.Green600
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarState,
    onRefresh: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    val upcomingMeetings = getUpcomingMeetings(state.meetings)
    val pastMeetings = getPastMeetings(state.meetings)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Calendar & Meetings",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "View and join your scheduled meetings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
        if (state.isLoading && state.meetings.isEmpty()) {
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
                        text = "Loading meetings...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (state.errorMessage?.contains("not connected") == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Google Calendar Not Connected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Please connect your Google account in the Gmail section to view and manage meetings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray400
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
                item {
                    Text(
                        text = "Upcoming Meetings (${upcomingMeetings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (upcomingMeetings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No upcoming meetings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray500
                            )
                        }
                    }
                } else {
                    items(upcomingMeetings, key = { it.eventId }) { meeting ->
                        MeetingCard(
                            meeting = meeting,
                            isPast = false,
                            onJoinMeeting = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            onViewInCalendar = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                if (pastMeetings.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Past Meetings (${pastMeetings.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(pastMeetings.take(10), key = { it.eventId }) { meeting ->
                        MeetingCard(
                            meeting = meeting,
                            isPast = true,
                            onJoinMeeting = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            onViewInCalendar = { url ->
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
private fun MeetingCard(
    meeting: CalendarMeeting,
    isPast: Boolean,
    onJoinMeeting: (String) -> Unit,
    onViewInCalendar: (String) -> Unit
) {
    WebStyleCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = meeting.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                fontWeight = FontWeight.SemiBold,
                color = Gray900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatMeetingTime(meeting.startTime, meeting.endTime),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Gray500
            )

            meeting.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Gray500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (meeting.attendees.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Attendees: ${meeting.attendees.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Gray400
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isPast && meeting.hangoutLink != null) {
                    Button(
                        onClick = { onJoinMeeting(meeting.hangoutLink!!) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Green600)
                    ) {
                        Text("Join Meeting", fontSize = 13.sp)
                    }
                }
                if (meeting.htmlLink != null) {
                    Button(
                        onClick = { onViewInCalendar(meeting.htmlLink!!) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("View in Calendar", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun formatMeetingTime(startTime: String, endTime: String): String {
    return try {
        val start = Instant.parse(startTime).atZone(ZoneId.systemDefault())
        val end = Instant.parse(endTime).atZone(ZoneId.systemDefault())
        
        val startFormatted = start.format(DateTimeFormatter.ofPattern("EEE, MMM d, h:mm a"))
        val endFormatted = end.format(DateTimeFormatter.ofPattern("h:mm a"))
        
        "$startFormatted - $endFormatted"
    } catch (e: Exception) {
        "$startTime - $endTime"
    }
}

private fun getUpcomingMeetings(meetings: List<CalendarMeeting>): List<CalendarMeeting> {
    val now = Instant.now()
    return meetings.filter { meeting ->
        try {
            val start = Instant.parse(meeting.startTime)
            start >= now && meeting.status == "confirmed"
        } catch (e: Exception) {
            false
        }
    }.sortedBy { meeting ->
        try {
            Instant.parse(meeting.startTime).toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }
}

private fun getPastMeetings(meetings: List<CalendarMeeting>): List<CalendarMeeting> {
    val now = Instant.now()
    return meetings.filter { meeting ->
        try {
            val start = Instant.parse(meeting.startTime)
            start < now || meeting.status != "confirmed"
        } catch (e: Exception) {
            false
        }
    }.sortedByDescending { meeting ->
        try {
            Instant.parse(meeting.startTime).toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }
}

