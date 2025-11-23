package com.project.lighthouse.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.lighthouse.R
import com.project.lighthouse.data.model.OrganizationResponse
import com.project.lighthouse.data.model.TenantResponse

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
                title = { Text("Settings") },
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
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(userName, style = MaterialTheme.typography.bodyLarge)
                        Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Button(onClick = onLogout) {
                            Text("Sign Out")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Create Organization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = state.createOrgName,
                            onValueChange = { onUpdateCreateForm(it, null) },
                            label = { Text("Name") }
                        )
                        OutlinedTextField(
                            value = state.createOrgDomain,
                            onValueChange = { onUpdateCreateForm(null, it) },
                            label = { Text("Domain (optional)") }
                        )
                        Button(onClick = onCreateOrg, enabled = state.createOrgName.isNotBlank()) {
                            Text("Create")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Join Organization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = state.joinOrgEmail,
                            onValueChange = { onUpdateJoinForm(it, null) },
                            label = { Text("Your Email") }
                        )
                        OutlinedTextField(
                            value = state.joinOrgName,
                            onValueChange = { onUpdateJoinForm(null, it) },
                            label = { Text("Organization Name") }
                        )
                        Button(onClick = onJoinOrg, enabled = state.joinOrgEmail.isNotBlank() && state.joinOrgName.isNotBlank()) {
                            Text("Request Access")
                        }
                    }
                }
            }
            if (state.organizations.isNotEmpty()) {
                item {
                    Text("Your Organizations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(state.organizations, key = { it.id }) { org ->
                    OrganizationItem(org)
                }
            }
            state.tenants?.let { tenantList ->
                item {
                    Text("Active Tenant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(tenantList.tenants, key = { it.id }) { tenant ->
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
private fun OrganizationItem(org: OrganizationResponse) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(org.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            org.domain?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(tenant.name, style = MaterialTheme.typography.titleMedium)
                if (isActive) {
                    Text("Active", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (!isActive) {
                TextButton(onClick = onSwitch, enabled = !isSwitching) {
                    Text("Switch")
                }
            }
        }
    }
}

