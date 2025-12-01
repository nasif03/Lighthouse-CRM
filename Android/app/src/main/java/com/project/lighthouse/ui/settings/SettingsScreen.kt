package com.project.lighthouse.ui.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.OrganizationResponse
import com.project.lighthouse.data.model.TenantResponse
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray200
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    userName: String,
    userEmail: String,
    onRefresh: () -> Unit,
    onCreateOrg: () -> Unit,
    onJoinOrg: () -> Unit,
    onUpdateCreateForm: (String?, String?) -> Unit,
    onUpdateJoinForm: (String?, String?) -> Unit,
    onSwitchTenant: (String) -> Unit,
    onLogout: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(painter = painterResource(id = R.drawable.ic_refresh), contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                WebStyleCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = Gray900
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = Gray900
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Gray500
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Sign Out", fontSize = 14.sp)
                        }
                    }
                }
            }
            
            // Create Organization Card
            item {
                WebStyleCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Create Organization",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = Gray900
                        )
                        OutlinedTextField(
                            value = state.createOrgName,
                            onValueChange = { onUpdateCreateForm(it, null) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.createOrgDomain,
                            onValueChange = { onUpdateCreateForm(null, it) },
                            label = { Text("Domain (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = onCreateOrg,
                            enabled = state.createOrgName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create", fontSize = 14.sp)
                        }
                    }
                }
            }
            
            // Join Organization Card
            item {
                WebStyleCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Join Organization",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = Gray900
                        )
                        OutlinedTextField(
                            value = state.joinOrgEmail,
                            onValueChange = { onUpdateJoinForm(it, null) },
                            label = { Text("Your Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.joinOrgName,
                            onValueChange = { onUpdateJoinForm(null, it) },
                            label = { Text("Organization Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = onJoinOrg,
                            enabled = state.joinOrgEmail.isNotBlank() && state.joinOrgName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Request Access", fontSize = 14.sp)
                        }
                    }
                }
            }
            
            // Tenants List
            state.tenants?.let { tenantList ->
                item {
                    Text(
                        text = "Active Tenant",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = Gray900,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(
                    tenantList.tenants,
                    key = { tenant -> "tenant-${tenant.id}" }
                ) { tenant ->
                    TenantItem(
                        tenant = tenant,
                        isActive = tenantList.activeTenantId == tenant.id,
                        isSwitching = state.isSwitchingTenant,
                        onSwitch = { onSwitchTenant(tenant.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TenantItem(
    tenant: TenantResponse,
    isActive: Boolean,
    isSwitching: Boolean,
    onSwitch: () -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tenant.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Gray900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isActive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Brand600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (!isActive) {
                Button(
                    onClick = onSwitch,
                    enabled = !isSwitching,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand600)
                ) {
                    if (isSwitching) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Switch", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
