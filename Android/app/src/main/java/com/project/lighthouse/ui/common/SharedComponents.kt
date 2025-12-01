package com.project.lighthouse.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.ui.theme.Blue100
import com.project.lighthouse.ui.theme.Blue700
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray100
import com.project.lighthouse.ui.theme.Gray200
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900
import com.project.lighthouse.ui.theme.Green100
import com.project.lighthouse.ui.theme.Green700
import com.project.lighthouse.ui.theme.Purple100
import com.project.lighthouse.ui.theme.Purple700
import com.project.lighthouse.ui.theme.Red100
import com.project.lighthouse.ui.theme.Red700
import com.project.lighthouse.ui.theme.Yellow100
import com.project.lighthouse.ui.theme.Yellow700

// Web-style Card Component matching dashboard
@Composable
fun WebStyleCard(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, Gray200, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor ?: Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        content()
    }
}

@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = getStatusColor(status)
    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = textColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getStatusColor(status: String): Pair<Color, Color> {
    return when (status.lowercase()) {
        "new" -> Pair(Blue100, Blue700)
        "contacted" -> Pair(Yellow100, Yellow700)
        "qualified" -> Pair(Green100, Green700)
        "converted" -> Pair(Purple100, Purple700)
        "lost" -> Pair(Red100, Red700)
        "prospecting" -> Pair(Blue100, Blue700)
        "qualification" -> Pair(Yellow100, Yellow700)
        "proposal" -> Pair(Green100, Green700)
        "negotiation" -> Pair(Yellow100, Yellow700)
        "closed-won" -> Pair(Purple100, Purple700)
        "closed-lost" -> Pair(Red100, Red700)
        "open" -> Pair(Blue100, Blue700)
        "in_progress" -> Pair(Yellow100, Yellow700)
        "resolved" -> Pair(Green100, Green700)
        "closed" -> Pair(Gray100, Gray700)
        else -> Pair(Gray100, Gray700)
    }
}

