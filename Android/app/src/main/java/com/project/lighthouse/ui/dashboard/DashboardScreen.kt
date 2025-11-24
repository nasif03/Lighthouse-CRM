package com.project.lighthouse.ui.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.DashboardSummary
import com.project.lighthouse.data.model.RecentContact
import com.project.lighthouse.data.model.RecentDeal
import com.project.lighthouse.data.model.RecentLead

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    userName: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Log.e("DashboardScreen", "Dashboard error surfaced: $it")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(text = "Dashboard", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Welcome back, ${userName.ifBlank { "User" }}",
                        style = MaterialTheme.typography.bodyMedium,
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

        if (state.isLoading) {
            LoadingState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryGrid(summary = state.stats.summary)
                }
                item {
                    LeadsAndDealsSection(
                        leadsByStatus = state.stats.leadsByStatus,
                        dealsByStage = state.stats.dealsByStage
                    )
                }
                item {
                    SectionHeader(title = "Recent Leads", onRefresh = onRefresh, showButton = false)
                }
                items(state.recentItems.recentLeads) { lead ->
                    RecentCard(
                        title = lead.name.ifBlank { "Unnamed Lead" },
                        subtitle = lead.email,
                        meta = lead.status.uppercase(),
                        footer = lead.createdAt
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    SectionHeader(title = "Recent Deals", onRefresh = onRefresh, showButton = false)
                }
                items(state.recentItems.recentDeals) { deal ->
                    RecentCard(
                        title = deal.name.ifBlank { "Unnamed Deal" },
                        subtitle = deal.stageName.ifBlank { deal.stageId },
                        meta = formatCurrency(deal.amount, deal.currency),
                        footer = deal.createdAt
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    SectionHeader(title = "Recent Contacts", onRefresh = onRefresh, showButton = false)
                }
                items(state.recentItems.recentContacts) { contact ->
                    RecentCard(
                        title = contact.name.ifBlank { "Unnamed Contact" },
                        subtitle = contact.email,
                        meta = contact.title,
                        footer = contact.createdAt
                    )
                }
                item {
                    if (state.errorMessage != null) {
                        ErrorCard(message = state.errorMessage, onRetry = onRefresh)
                    }
                }
                item {
                    if (state.isRefreshing) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Loading your dashboard...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SummaryGrid(summary: DashboardSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(
            title = "Leads",
            value = summary.totalLeads.toString(),
            delta = summary.recentLeads
        )
        SummaryCard(
            title = "Contacts",
            value = summary.totalContacts.toString(),
            delta = summary.recentContacts
        )
        SummaryCard(
            title = "Deals",
            value = summary.totalDeals.toString(),
            delta = summary.recentDeals
        )
        SummaryCard(
            title = "Accounts",
            value = summary.totalAccounts.toString(),
            delta = summary.recentActivities
        )
        SummaryCard(
            title = "Deal Value",
            value = formatCurrency(summary.totalDealValue)
        )
        SummaryCard(
            title = "Win Rate",
            value = "${summary.conversionRate}%",
            valueColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    delta: Int? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            delta?.let {
                Text(
                    text = "+$it this week",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LeadsAndDealsSection(
    leadsByStatus: Map<String, Int>,
    dealsByStage: Map<String, Int>
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(title = "Pipeline Overview", onRefresh = {}, showButton = false)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Leads by Status", style = MaterialTheme.typography.labelLarge)
            leadsByStatus.forEach { (status, value) ->
                ProgressRow(label = status, value = value)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Deals by Stage", style = MaterialTheme.typography.labelLarge)
            dealsByStage.forEach { (stage, value) ->
                ProgressRow(label = stage, value = value)
            }
        }
    }
}

@Composable
private fun ProgressRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
        Text(text = value.toString(), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionHeader(title: String, onRefresh: () -> Unit, showButton: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (showButton) {
            Button(onClick = onRefresh) {
                Text(text = "Refresh")
            }
        }
    }
}

@Composable
private fun RecentCard(
    title: String,
    subtitle: String,
    meta: String,
    footer: String,
    elevation: Dp = 1.dp
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = elevation)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (meta.isNotBlank()) {
                Text(text = meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = footer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text(text = "Retry")
            }
        }
    }
}

private fun formatCurrency(amount: Double?, currency: String = "USD"): String {
    val value = amount ?: 0.0
    return "$currency ${"%,.0f".format(value)}"
}

