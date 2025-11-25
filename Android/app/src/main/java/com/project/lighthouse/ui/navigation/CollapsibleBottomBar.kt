package com.project.lighthouse.ui.navigation

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination

@Composable
fun CollapsibleBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Log.d("CollapsibleBottomBar", "isExpanded=$isExpanded, currentDestination=${currentDestination?.route}")

    Box(modifier = modifier) {
        // Expanded menu - appears above the navigation bar
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(bottom = 56.dp), // Position above navigation bar
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MainDestination.bottomNavItems.filter { it != MainDestination.Dashboard }.forEach { destination ->
                        val selected = currentDestination?.route == destination.route
                        Button(
                            onClick = {
                                Log.d("CollapsibleBottomBar", "Navigating to ${destination.route}")
                                onNavigate(destination)
                                isExpanded = false // Collapse after selection
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = destination.iconRes),
                                    contentDescription = stringResource(id = destination.labelRes),
                                    modifier = Modifier.size(24.dp),
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(id = destination.labelRes),
                                    fontSize = 14.sp,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Navigation Bar
        NavigationBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            // Collapsed state: Show only Dashboard
            val dashboardSelected = currentDestination?.route == MainDestination.Dashboard.route
            NavigationBarItem(
                selected = dashboardSelected,
                onClick = {
                    Log.d("CollapsibleBottomBar", "Navigating to Dashboard")
                    onNavigate(MainDestination.Dashboard)
                },
                icon = {
                    Icon(
                        painter = painterResource(id = MainDestination.Dashboard.iconRes),
                        contentDescription = stringResource(id = MainDestination.Dashboard.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = MainDestination.Dashboard.labelRes),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                },
                alwaysShowLabel = true,
                modifier = Modifier.weight(1f)
            )

            // Collapse/Expand button - Always visible on the right side
            IconButton(
                onClick = {
                    if (isExpanded) {
                        Log.d("CollapsibleBottomBar", "Collapsing navigation - showing only Dashboard")
                        isExpanded = false
                    } else {
                        Log.d("CollapsibleBottomBar", "Expanding navigation - showing vertical menu")
                        isExpanded = true
                    }
                },
                modifier = Modifier.padding(start = 8.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

