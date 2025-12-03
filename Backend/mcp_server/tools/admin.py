"""MCP Tools for Administration (Organizations, Employees, Roles)"""
from typing import Optional, List
from mcp_server.utils import API_BASE_URL, http_client, get_auth_headers


# Organization Tools
async def get_organizations(
    auth_token: Optional[str] = None,
) -> list:
    """Get all organizations the current user owns or belongs to.
    
    Args:
        auth_token: Firebase authentication token (required)
    
    Returns:
        List of organizations
    """
    url = f"{API_BASE_URL}/api/organizations"
    
    response = await http_client.get(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def create_organization(
    name: str,
    domain: Optional[str] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Create a new organization (tenant).
    
    Args:
        name: Organization name
        domain: Organization domain (optional, will be generated from name if not provided)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Created organization information
    """
    url = f"{API_BASE_URL}/api/organizations"
    payload = {"name": name}
    if domain:
        payload["domain"] = domain
    
    response = await http_client.post(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def update_organization(
    org_id: str,
    name: Optional[str] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Update an organization (admin only).
    
    Args:
        org_id: Organization ID
        name: New organization name (optional)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Updated organization information
    """
    url = f"{API_BASE_URL}/api/organizations/{org_id}"
    payload = {}
    if name:
        payload["name"] = name
    
    response = await http_client.put(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


# Employee Tools
async def get_employees(
    org_id: str,
    auth_token: Optional[str] = None,
) -> list:
    """Get all employees (users) in an organization (admin only).
    
    Args:
        org_id: Organization ID
        auth_token: Firebase authentication token (required)
    
    Returns:
        List of employees
    """
    url = f"{API_BASE_URL}/api/organizations/{org_id}/employees"
    
    response = await http_client.get(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def create_employee(
    org_id: str,
    email: str,
    name: str,
    roleIds: Optional[List[str]] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Add an employee (user) to an organization (admin only).
    
    Args:
        org_id: Organization ID
        email: Employee email address
        name: Employee name
        roleIds: List of role IDs to assign (optional)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Created/updated employee information
    """
    url = f"{API_BASE_URL}/api/organizations/{org_id}/employees"
    payload = {
        "email": email,
        "name": name,
    }
    if roleIds:
        payload["roleIds"] = roleIds
    
    response = await http_client.post(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def update_employee(
    org_id: str,
    employee_id: str,
    name: Optional[str] = None,
    roleIds: Optional[List[str]] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Update employee (assign roles, update name) (admin only).
    
    Args:
        org_id: Organization ID
        employee_id: Employee/User ID
        name: New name (optional)
        roleIds: New list of role IDs (optional)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Updated employee information
    """
    url = f"{API_BASE_URL}/api/organizations/{org_id}/employees/{employee_id}"
    payload = {}
    if name:
        payload["name"] = name
    if roleIds is not None:
        payload["roleIds"] = roleIds
    
    response = await http_client.put(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def remove_employee(
    org_id: str,
    employee_id: str,
    auth_token: Optional[str] = None,
) -> dict:
    """Remove employee from organization (admin only).
    
    Args:
        org_id: Organization ID
        employee_id: Employee/User ID to remove
        auth_token: Firebase authentication token (required)
    
    Returns:
        Success message
    """
    url = f"{API_BASE_URL}/api/organizations/{org_id}/employees/{employee_id}"
    
    response = await http_client.delete(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


# Role Tools
async def get_roles(
    org_id: str,
    auth_token: Optional[str] = None,
) -> list:
    """Get all roles in an organization.
    
    Args:
        org_id: Organization ID
        auth_token: Firebase authentication token (required)
    
    Returns:
        List of roles
    """
    url = f"{API_BASE_URL}/api/organizations/{org_id}/roles"
    
    response = await http_client.get(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def create_role(
    org_id: str,
    name: str,
    permissions: List[str],
    auth_token: Optional[str] = None,
) -> dict:
    """Create a new role in an organization (admin only).
    
    Args:
        org_id: Organization ID
        name: Role name
        permissions: List of permissions (e.g., ["read:leads", "write:deals"])
        auth_token: Firebase authentication token (required)
    
    Returns:
        Created role information
    """
    url = f"{API_BASE_URL}/api/organizations/{org_id}/roles"
    payload = {
        "name": name,
        "permissions": permissions,
    }
    
    response = await http_client.post(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def update_role(
    org_id: str,
    role_id: str,
    name: Optional[str] = None,
    permissions: Optional[List[str]] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Update a role (admin only).
    
    Args:
        org_id: Organization ID
        role_id: Role ID
        name: New role name (optional)
        permissions: New list of permissions (optional)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Updated role information
    """
    url = f"{API_BASE_URL}/api/organizations/{org_id}/roles/{role_id}"
    payload = {}
    if name:
        payload["name"] = name
    if permissions is not None:
        payload["permissions"] = permissions
    
    response = await http_client.put(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def delete_role(
    org_id: str,
    role_id: str,
    auth_token: Optional[str] = None,
) -> dict:
    """Delete a role (admin only).
    
    Args:
        org_id: Organization ID
        role_id: Role ID to delete
        auth_token: Firebase authentication token (required)
    
    Returns:
        Success message
    """
    url = f"{API_BASE_URL}/api/organizations/{org_id}/roles/{role_id}"
    
    response = await http_client.delete(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


# Tenant/Organization Switching Tools
async def get_tenants(
    auth_token: Optional[str] = None,
) -> dict:
    """Get all organizations (tenants) the user belongs to and the active tenant. Use this when users ask: "Which organization am I in?", "What organizations do I belong to?", "Show me my organizations", "What is my current organization?", or similar questions about their organization/tenant context.
    
    Args:
        auth_token: Firebase authentication token (required)
    
    Returns:
        Dictionary with tenants list and activeTenantId
    """
    url = f"{API_BASE_URL}/api/tenants"
    
    response = await http_client.get(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def switch_tenant(
    tenant_id: str,
    auth_token: Optional[str] = None,
) -> dict:
    """Switch the active organization (tenant) for the current user. Use this when users ask: "Switch to organization X", ...
    """
    url = f"{API_BASE_URL}/api/tenants/switch"
    # include both keys in case API expects tenantId (camelCase) or tenant_id (snake_case)
    payload = {"tenant_id": tenant_id, "tenantId": tenant_id}
    
    response = await http_client.post(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    # if non-2xx, return readable debug info instead of raising silently
    if response.status_code >= 400:
        try:
            body = response.json()
        except Exception:
            body = response.text
        raise RuntimeError(f"switch_tenant failed: {response.status_code} {body}")
    return response.json()


# Export tools list for registration
admin_tools = [
    get_organizations,
    create_organization,
    update_organization,
    get_employees,
    create_employee,
    update_employee,
    remove_employee,
    get_roles,
    create_role,
    update_role,
    delete_role,
    get_tenants,
    switch_tenant,
]

