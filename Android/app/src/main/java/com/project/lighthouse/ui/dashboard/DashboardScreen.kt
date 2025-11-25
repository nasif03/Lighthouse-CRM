package com.project.lighthouse.ui.dashboard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.DashboardSummary
import com.project.lighthouse.data.model.RecentContact
import com.project.lighthouse.data.model.RecentDeal
import com.project.lighthouse.data.model.RecentLead
import com.project.lighthouse.ui.theme.Blue100
import com.project.lighthouse.ui.theme.Blue500
import com.project.lighthouse.ui.theme.Blue700
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray100
import com.project.lighthouse.ui.theme.Gray200
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900
import com.project.lighthouse.ui.theme.Green100
import com.project.lighthouse.ui.theme.Green500
import com.project.lighthouse.ui.theme.Green600
import com.project.lighthouse.ui.theme.Green700
import com.project.lighthouse.ui.theme.Orange100
import com.project.lighthouse.ui.theme.Orange500
import com.project.lighthouse.ui.theme.Purple100
import com.project.lighthouse.ui.theme.Purple500
import com.project.lighthouse.ui.theme.Purple700
import com.project.lighthouse.ui.theme.Red100
import com.project.lighthouse.ui.theme.Red500
import com.project.lighthouse.ui.theme.Red700
import com.project.lighthouse.ui.theme.Yellow100
import com.project.lighthouse.ui.theme.Yellow500
import com.project.lighthouse.ui.theme.Yellow700
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                verticalArrangement = Arrangement.spacedBy(20.dp) // Reduced for mobile
            ) {
                // Summary Cards (4 cards in grid)
                item {
                    SummaryCardsGrid(summary = state.stats.summary)
                }

                // Revenue & Conversion Cards (3 cards)
                item {
                    RevenueCardsGrid(summary = state.stats.summary)
                }

                // Pipeline Cards (2 cards side-by-side)
                item {
                    PipelineCardsRow(
                        leadsByStatus = state.stats.leadsByStatus,
                        dealsByStage = state.stats.dealsByStage,
                        totalLeads = state.stats.summary.totalLeads,
                        totalDeals = state.stats.summary.totalDeals
                    )
                }

                // Recent Activity (3 cards side-by-side)
                item {
                    RecentActivityRow(
                        recentLeads = state.recentItems.recentLeads,
                        recentDeals = state.recentItems.recentDeals,
                        recentContacts = state.recentItems.recentContacts
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
        Text(text = "Loading dashboard...", style = MaterialTheme.typography.bodyMedium)
    }
}

// Summary Cards Grid (4 cards: Leads, Contacts, Deals, Accounts) - 2x2 grid for mobile
@Composable
private fun SummaryCardsGrid(summary: DashboardSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // First row: Leads and Contacts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total Leads",
                icon = "📋",
                value = "${summary.totalLeads}",
                subtitle = "${summary.recentLeads} new this week",
                showViewAll = true
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total Contacts",
                icon = "👥",
                value = "${summary.totalContacts}",
                subtitle = "${summary.recentContacts} new this week",
                showViewAll = true
            )
        }
        // Second row: Deals and Accounts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total Deals",
                icon = "💰",
                value = "${summary.totalDeals}",
                subtitle = "${summary.recentDeals} new this week",
                showViewAll = true
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total Accounts",
                icon = "🏢",
                value = "${summary.totalAccounts}",
                subtitle = "Active accounts",
                showViewAll = false
            )
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: String,
    value: String,
    subtitle: String,
    showViewAll: Boolean = false
) {
    WebStyleCard(modifier = modifier) {
        CardHeader(title = title, icon = icon)
        CardContent {
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Gray900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showViewAll) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "View all →",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Brand600
                    )
                }
            }
        }
    }
}

// Revenue & Conversion Cards (3 cards) - Stack vertically on mobile
@Composable
private fun RevenueCardsGrid(summary: DashboardSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RevenueCard(
            title = "Total Deal Value",
            value = formatCurrency(summary.totalDealValue),
            subtitle = "All active deals",
            valueColor = Gray900
        )
        RevenueCard(
            title = "Won Deal Value",
            value = formatCurrency(summary.wonDealValue),
            subtitle = "Closed won deals",
            valueColor = Green600
        )
        RevenueCard(
            title = "Conversion Rate",
            value = "${summary.conversionRate.toInt()}%",
            subtitle = "Leads converted",
            valueColor = Brand600
        )
    }
}

@Composable
private fun RevenueCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        CardHeader(title = title)
        CardContent {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Gray500
            )
        }
    }
}

// Pipeline Cards (2 cards stacked vertically on mobile)
@Composable
private fun PipelineCardsRow(
    leadsByStatus: Map<String, Int>,
    dealsByStage: Map<String, Int>,
    totalLeads: Int,
    totalDeals: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Leads by Status Card
        WebStyleCard(modifier = Modifier.fillMaxWidth()) {
            CardHeader(title = "Leads by Status")
            CardContent {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    leadsByStatus.filter { it.key != "lost" }.forEach { (status, count) ->
                        PipelineRow(
                            label = status,
                            count = count,
                            total = totalLeads,
                            getStatusColor = { getStatusColor(it) },
                            getProgressColor = { getStatusProgressColor(it) }
                        )
                    }
                }
            }
        }

        // Deals by Stage Card
        WebStyleCard(modifier = Modifier.fillMaxWidth()) {
            CardHeader(title = "Deals by Stage")
            CardContent {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    dealsByStage.forEach { (stage, count) ->
                        PipelineRow(
                            label = stage,
                            count = count,
                            total = totalDeals,
                            getStatusColor = { getStageColor(it) },
                            getProgressColor = { getStageProgressColor(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineRow(
    label: String,
    count: Int,
    total: Int,
    getStatusColor: (String) -> Pair<Color, Color>,
    getProgressColor: (String) -> Color
) {
    val (bgColor, textColor) = getStatusColor(label)
    val progressColor = getProgressColor(label)
    val percentage = if (total > 0) (count.toFloat() / total) else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status Chip and Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(bgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = label.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        .replace("-", " "),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Count
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Gray700,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Gray200)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxSize()
                    .background(progressColor)
            )
        }
    }
}

// Recent Activity (3 cards stacked vertically on mobile)
@Composable
private fun RecentActivityRow(
    recentLeads: List<RecentLead>,
    recentDeals: List<RecentDeal>,
    recentContacts: List<RecentContact>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Recent Leads Card
        RecentActivityCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Recent Leads",
            items = recentLeads.map { lead ->
                RecentItemData(
                    id = lead.id,
                    name = lead.name,
                    subtitle = lead.email,
                    status = lead.status,
                    source = lead.source,
                    timestamp = lead.createdAt
                )
            },
            getStatusColor = { getStatusColor(it) }
        )

        // Recent Deals Card
        RecentActivityCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Recent Deals",
            items = recentDeals.map { deal ->
                RecentItemData(
                    id = deal.id,
                    name = deal.name,
                    subtitle = deal.stageName.ifBlank { deal.stageId },
                    amount = deal.amount,
                    currency = deal.currency,
                    stageId = deal.stageId,
                    stageName = deal.stageName,
                    timestamp = deal.createdAt
                )
            },
            getStatusColor = { getStageColor(it) }
        )

        // Recent Contacts Card
        RecentActivityCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Recent Contacts",
            items = recentContacts.map { contact ->
                RecentItemData(
                    id = contact.id,
                    name = contact.name,
                    subtitle = contact.email,
                    title = contact.title,
                    timestamp = contact.createdAt
                )
            }
        )
    }
}

@Composable
private fun RecentActivityCard(
    modifier: Modifier = Modifier,
    title: String,
    items: List<RecentItemData>,
    getStatusColor: ((String) -> Pair<Color, Color>)? = null
) {
    WebStyleCard(modifier = modifier) {
        CardHeader(
            title = title,
            showViewAll = true
        )
        CardContent {
            if (items.isEmpty()) {
                Text(
                    text = "No recent ${title.lowercase().removePrefix("recent ")}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Gray500,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.forEachIndexed { index, item ->
                        RecentItemRow(item = item, getStatusColor = getStatusColor)
                        if (index < items.size - 1) {
                            HorizontalDivider(color = Gray100, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentItemRow(
    item: RecentItemData,
    getStatusColor: ((String) -> Pair<Color, Color>)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            // Name
            Text(
                text = item.name.ifBlank { "Unnamed" },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = Gray900,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Subtitle (email, stage, etc.)
            if (item.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Amount (for deals)
            if (item.amount != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(item.amount, item.currency ?: "USD"),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Gray700,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Title (for contacts)
            if (!item.title.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Status/Stage Chip
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val status = item.status ?: item.stageId ?: ""
                if (status.isNotBlank() && getStatusColor != null) {
                    val (bgColor, textColor) = getStatusColor(status)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(bgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!item.source.isNullOrBlank()) {
                    Text(
                        text = item.source ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Gray500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        // Timestamp
        Text(
            text = formatRelativeTime(item.timestamp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = Gray400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Web-style Card Component
@Composable
private fun WebStyleCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, Gray200, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        content()
    }
}

@Composable
private fun CardHeader(
    title: String,
    icon: String? = null,
    showViewAll: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // Reduced for mobile
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Gray900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                icon?.let {
                    Text(text = it, fontSize = 18.sp)
                }
            }
            if (showViewAll) {
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Brand600
                )
            }
        }
        HorizontalDivider(color = Gray200, thickness = 1.dp)
    }
}

@Composable
private fun CardContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp) // Reduced for mobile
    ) {
        content()
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

// Helper Functions
private fun formatCurrency(amount: Double?, currency: String = "USD"): String {
    val value = amount ?: 0.0
    return java.text.NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }.format(value)
}

private fun formatRelativeTime(dateString: String): String {
    return try {
        val date = Instant.parse(dateString).atZone(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        val diffInSeconds = (now.epochSecond - date.epochSecond)

        when {
            diffInSeconds < 60 -> "Just now"
            diffInSeconds < 3600 -> "${diffInSeconds / 60}m ago"
            diffInSeconds < 86400 -> "${diffInSeconds / 3600}h ago"
            diffInSeconds < 604800 -> "${diffInSeconds / 86400}d ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
                date.atZone(ZoneId.systemDefault()).format(formatter)
            }
        }
    } catch (e: Exception) {
        dateString
    }
}

private fun getStatusColor(status: String): Pair<Color, Color> {
    return when (status.lowercase()) {
        "new" -> Pair(Blue100, Blue700)
        "contacted" -> Pair(Yellow100, Yellow700)
        "qualified" -> Pair(Green100, Green700)
        "converted" -> Pair(Purple100, Purple700)
        else -> Pair(Gray100, Gray700)
    }
}

private fun getStatusProgressColor(status: String): Color {
    return when (status.lowercase()) {
        "new" -> Blue500
        "contacted" -> Yellow500
        "qualified" -> Green500
        "converted" -> Purple500
        else -> Gray500
    }
}

private fun getStageColor(stage: String): Pair<Color, Color> {
    return when (stage.lowercase()) {
        "prospecting" -> Pair(Blue100, Blue700)
        "qualification" -> Pair(Yellow100, Yellow700)
        "proposal" -> Pair(Green100, Green700)
        "negotiation" -> Pair(Orange100, Orange500)
        "closed-won" -> Pair(Purple100, Purple700)
        "closed-lost" -> Pair(Red100, Red700)
        else -> Pair(Gray100, Gray700)
    }
}

private fun getStageProgressColor(stage: String): Color {
    return when (stage.lowercase()) {
        "prospecting" -> Blue500
        "qualification" -> Yellow500
        "proposal" -> Green500
        "negotiation" -> Orange500
        "closed-won" -> Purple500
        "closed-lost" -> Red500
        else -> Gray500
    }
}

// Data class for recent items
private data class RecentItemData(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val status: String? = null,
    val source: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val stageId: String? = null,
    val stageName: String? = null,
    val title: String? = null,
    val timestamp: String
)
