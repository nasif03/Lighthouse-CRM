package com.project.lighthouse.ui.administration

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lighthouse.data.model.EmployeeDto
import com.project.lighthouse.data.model.RoleDto
import com.project.lighthouse.ui.common.WebStyleCard
import com.project.lighthouse.ui.theme.Brand600
import com.project.lighthouse.ui.theme.Gray400
import com.project.lighthouse.ui.theme.Gray500
import com.project.lighthouse.ui.theme.Gray700
import com.project.lighthouse.ui.theme.Gray900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministrationScreen(
    state: AdministrationState,
    onSelectOrganization: (String) -> Unit,
    onUpdateNewEmployeeName: (String) -> Unit,
    onUpdateNewEmployeeEmail: (String) -> Unit,
    onToggleRoleSelection: (String) -> Unit,
    onAddEmployee: () -> Unit,
    onStartEditingEmployee: (String) -> Unit,
    onCancelEditingEmployee: () -> Unit,
    onToggleEditingRoleSelection: (String) -> Unit,
    onUpdateEmployeeRoles: () -> Unit,
    onUpdateNewRoleName: (String) -> Unit,
    onTogglePermission: (String) -> Unit,
    onAddRole: () -> Unit,
    onStartEditingRole: (RoleDto) -> Unit,
    onCancelEditingRole: () -> Unit,
    onUpdateRole: () -> Unit,
    onDeleteRole: (String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

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
                title = { Text("Administration", style = MaterialTheme.typography.titleLarge) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            state.isLoadingOrganizations -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading organizations...", color = Gray500)
                }
            }
            state.selectedOrgId == null -> {
                // Organization selection
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (state.organizations.isEmpty()) {
                        Text(
                            text = "No organizations found. Please create or join an organization first.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Text(
                            text = "Select an organization to manage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gray900,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.organizations) { org ->
                                WebStyleCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectOrganization(org.id) }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = org.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Gray900
                                        )
                                        org.domain?.takeIf { it.isNotBlank() }?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Gray500,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Main content with employees and roles
                when (state.uiState) {
                    is AdministrationUiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading employees and roles...", color = Gray500)
                        }
                    }
                    is AdministrationUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.uiState.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is AdministrationUiState.Empty -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No employees or roles found. Start by adding employees and creating roles.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Gray500,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is AdministrationUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Employees Section
                            item {
                                EmployeesSection(
                                    state = state,
                                    onUpdateNewEmployeeName = onUpdateNewEmployeeName,
                                    onUpdateNewEmployeeEmail = onUpdateNewEmployeeEmail,
                                    onToggleRoleSelection = onToggleRoleSelection,
                                    onAddEmployee = onAddEmployee,
                                    onStartEditingEmployee = onStartEditingEmployee,
                                    onCancelEditingEmployee = onCancelEditingEmployee,
                                    onToggleEditingRoleSelection = onToggleEditingRoleSelection,
                                    onUpdateEmployeeRoles = onUpdateEmployeeRoles
                                )
                            }
                            
                            // Roles Section
                            item {
                                RolesSection(
                                    state = state,
                                    onUpdateNewRoleName = onUpdateNewRoleName,
                                    onTogglePermission = onTogglePermission,
                                    onAddRole = onAddRole,
                                    onStartEditingRole = onStartEditingRole,
                                    onCancelEditingRole = onCancelEditingRole,
                                    onUpdateRole = onUpdateRole,
                                    onDeleteRole = { showDeleteConfirm = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Role Confirmation Dialog
    showDeleteConfirm?.let { roleId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Role") },
            text = { Text("Are you sure you want to delete this role?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteRole(roleId)
                    showDeleteConfirm = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmployeesSection(
    state: AdministrationState,
    onUpdateNewEmployeeName: (String) -> Unit,
    onUpdateNewEmployeeEmail: (String) -> Unit,
    onToggleRoleSelection: (String) -> Unit,
    onAddEmployee: () -> Unit,
    onStartEditingEmployee: (String) -> Unit,
    onCancelEditingEmployee: () -> Unit,
    onToggleEditingRoleSelection: (String) -> Unit,
    onUpdateEmployeeRoles: () -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Employees",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )

            // Add Employee Form
            HorizontalDivider()
            Text(
                text = "Add Employee",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray700,
                fontSize = 14.sp
            )
            OutlinedTextField(
                value = state.newEmployeeName,
                onValueChange = onUpdateNewEmployeeName,
                label = { Text("Employee Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.newEmployeeEmail,
                onValueChange = onUpdateNewEmployeeEmail,
                label = { Text("Employee Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                text = "Roles",
                style = MaterialTheme.typography.labelMedium,
                color = Gray700,
                fontSize = 12.sp
            )
            val roleScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .verticalScroll(roleScrollState),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.roles.forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = state.selectedRoleIds.contains(role.id),
                            onCheckedChange = { onToggleRoleSelection(role.id) }
                        )
                        Text(
                            text = role.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray700,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Button(
                onClick = onAddEmployee,
                enabled = state.newEmployeeName.isNotBlank() && state.newEmployeeEmail.isNotBlank() && !state.isAddingEmployee,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isAddingEmployee) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Add Employee", fontSize = 13.sp)
                }
            }

            // Employee List
            HorizontalDivider()
            Text(
                text = "Employee List",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray700,
                fontSize = 14.sp
            )
            if (state.employees.isEmpty()) {
                Text("No employees found.", fontSize = 12.sp, color = Gray500)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.employees.forEach { employee ->
                        EmployeeItem(
                            employee = employee,
                            roles = state.roles,
                            isEditing = state.editingEmployeeId == employee.id,
                            editingRoleIds = state.editingEmployeeRoles,
                            onStartEditing = { onStartEditingEmployee(employee.id) },
                            onCancelEditing = onCancelEditingEmployee,
                            onToggleRole = onToggleEditingRoleSelection,
                            onSave = onUpdateEmployeeRoles
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeItem(
    employee: EmployeeDto,
    roles: List<RoleDto>,
    isEditing: Boolean,
    editingRoleIds: List<String>,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onToggleRole: (String) -> Unit,
    onSave: () -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = employee.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )
            Text(
                text = employee.email,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500,
                fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (employee.isAdmin) {
                    Text(
                        text = "Admin",
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand600,
                        fontSize = 10.sp
                    )
                }
                val roleNames = employee.roleIds.mapNotNull { roleId ->
                    roles.find { it.id == roleId }?.name
                }
                if (roleNames.isNotEmpty()) {
                    Text(
                        text = "Roles: ${roleNames.joinToString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gray400,
                        fontSize = 10.sp
                    )
                }
            }
            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                val editRoleScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .verticalScroll(editRoleScrollState),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    roles.forEach { role ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = editingRoleIds.contains(role.id),
                                onCheckedChange = { onToggleRole(role.id) }
                            )
                            Text(
                                text = role.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save", fontSize = 12.sp)
                    }
                    TextButton(onClick = onCancelEditing, modifier = Modifier.weight(1f)) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                }
            } else {
                Button(
                    onClick = onStartEditing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Edit Roles", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RolesSection(
    state: AdministrationState,
    onUpdateNewRoleName: (String) -> Unit,
    onTogglePermission: (String) -> Unit,
    onAddRole: () -> Unit,
    onStartEditingRole: (RoleDto) -> Unit,
    onCancelEditingRole: () -> Unit,
    onUpdateRole: () -> Unit,
    onDeleteRole: (String) -> Unit
) {
    WebStyleCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Roles & Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )

            // Create/Edit Role Form
            HorizontalDivider()
            Text(
                text = if (state.editingRoleId != null) "Edit Role" else "Create Role",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray700,
                fontSize = 14.sp
            )
            OutlinedTextField(
                value = state.newRoleName,
                onValueChange = onUpdateNewRoleName,
                label = { Text("Role Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.labelMedium,
                color = Gray700,
                fontSize = 12.sp
            )
            val permissionScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .verticalScroll(permissionScrollState),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                availablePermissions.forEach { permission ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.newRolePermissions.contains(permission),
                            onCheckedChange = { onTogglePermission(permission) }
                        )
                        Text(
                            text = permission,
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray700,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.editingRoleId != null) {
                    Button(
                        onClick = onCancelEditingRole,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }
                    Button(
                        onClick = onUpdateRole,
                        enabled = state.newRoleName.isNotBlank() && !state.isUpdatingRole,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isUpdatingRole) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Update Role", fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onAddRole,
                        enabled = state.newRoleName.isNotBlank() && !state.isAddingRole,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isAddingRole) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create Role", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Roles List
            HorizontalDivider()
            Text(
                text = "Roles List",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Gray700,
                fontSize = 14.sp
            )
            if (state.roles.isEmpty()) {
                Text("No roles found. Create one above.", fontSize = 12.sp, color = Gray500)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.roles.forEach { role ->
                        RoleItem(
                            role = role,
                            onDelete = { onDeleteRole(role.id) },
                            onEdit = { roleToEdit ->
                                // Start editing this role
                                onStartEditingRole(roleToEdit)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleItem(
    role: RoleDto,
    onDelete: () -> Unit,
    onEdit: (RoleDto) -> Unit
) {
    WebStyleCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(role) }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = role.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray900
                    )
                    if (role.permissions.isNotEmpty()) {
                        Text(
                            text = "Permissions: ${role.permissions.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "No permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray400,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onEdit(role) },
                        colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Edit", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

