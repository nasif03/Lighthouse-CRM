package com.project.lighthouse.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.data.model.TicketDto
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray600
import com.project.lighthouse.ui.theme.Gray900
import com.project.lighthouse.ui.theme.Green600

data class TicketStats(
    val total: Int,
    val open: Int,
    val inProgress: Int,
    val resolved: Int,
    val closed: Int
)

@Composable
fun TicketStatsDashboard(stats: TicketStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Total",
            value = stats.total.toString(),
            color = Gray900,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Open",
            value = stats.open.toString(),
            color = androidx.compose.ui.graphics.Color(0xFFD97706),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "In Progress",
            value = stats.inProgress.toString(),
            color = androidx.compose.ui.graphics.Color(0xFF2563EB),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Resolved",
            value = stats.resolved.toString(),
            color = Green600,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Closed",
            value = stats.closed.toString(),
            color = Gray600,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    WebStyleCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 18.sp
            )
        }
    }
}

fun calculateStats(tickets: List<TicketDto>): TicketStats {
    return TicketStats(
        total = tickets.size,
        open = tickets.count { it.status == "open" },
        inProgress = tickets.count { it.status == "in_progress" },
        resolved = tickets.count { it.status == "resolved" },
        closed = tickets.count { it.status == "closed" }
    )
}

fun filterTickets(
    tickets: List<TicketDto>,
    searchQuery: String,
    statusFilter: String?,
    priorityFilter: String?
): List<TicketDto> {
    val priorityOrder = mapOf("urgent" to 4, "high" to 3, "medium" to 2, "low" to 1)
    
    return tickets
        .filter { ticket ->
            val matchesSearch = searchQuery.isBlank() || 
                ticket.ticketNumber.contains(searchQuery, ignoreCase = true) ||
                ticket.subject.contains(searchQuery, ignoreCase = true) ||
                ticket.name.contains(searchQuery, ignoreCase = true) ||
                ticket.email.contains(searchQuery, ignoreCase = true)
            
            val matchesStatus = statusFilter == null || ticket.status == statusFilter
            val matchesPriority = priorityFilter == null || ticket.priority == priorityFilter
            
            matchesSearch && matchesStatus && matchesPriority
        }
        .sortedWith(compareByDescending<TicketDto> { priorityOrder[it.priority] ?: 0 }
            .thenByDescending { it.updatedAt })
}

