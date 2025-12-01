"""MCP Tools for Tickets Management"""
from typing import Optional
from mcp_server.utils import API_BASE_URL, http_client, get_auth_headers


async def create_ticket(
    orgId: str,
    name: str,
    email: str,
    subject: str,
    description: str,
    phone: Optional[str] = None,
    priority: str = "medium",
    category: Optional[str] = None,
) -> dict:
    """Create a new support ticket (public endpoint, no auth required).
    
    Args:
        orgId: Organization ID
        name: Customer name
        email: Customer email
        subject: Ticket subject
        description: Ticket description
        phone: Customer phone number (optional)
        priority: Priority level (low, medium, high, urgent). Defaults to "medium"
        category: Ticket category (optional, e.g., technical, billing, feature_request)
    
    Returns:
        Created ticket information
    """
    url = f"{API_BASE_URL}/api/tickets"
    payload = {
        "orgId": orgId,
        "name": name,
        "email": email,
        "subject": subject,
        "description": description,
        "priority": priority,
    }
    if phone:
        payload["phone"] = phone
    if category:
        payload["category"] = category
    
    response = await http_client.post(
        url,
        json=payload,
        headers=get_auth_headers()
    )
    response.raise_for_status()
    return response.json()


async def get_tickets(
    skip: int = 0,
    limit: int = 100,
    status: Optional[str] = None,
    priority: Optional[str] = None,
    assignedTo: Optional[str] = None,
    auth_token: Optional[str] = None,
) -> list:
    """Get all tickets for the current user's organization.
    
    Args:
        skip: Number of tickets to skip (for pagination)
        limit: Maximum number of tickets to return (default: 100)
        status: Filter by status (open, in_progress, resolved, closed)
        priority: Filter by priority (low, medium, high, urgent)
        assignedTo: Filter by assigned user ID
        auth_token: Firebase authentication token (required)
    
    Returns:
        List of tickets
    """
    url = f"{API_BASE_URL}/api/tickets"
    params = {"skip": skip, "limit": limit}
    if status:
        params["status"] = status
    if priority:
        params["priority"] = priority
    if assignedTo:
        params["assignedTo"] = assignedTo
    
    response = await http_client.get(
        url,
        params=params,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def get_ticket(
    ticket_id: str,
    auth_token: Optional[str] = None,
) -> dict:
    """Get a single ticket by ID.
    
    Args:
        ticket_id: Ticket ID (JSM issue key)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Ticket information
    """
    url = f"{API_BASE_URL}/api/tickets/{ticket_id}"
    
    response = await http_client.get(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def update_ticket(
    ticket_id: str,
    status: Optional[str] = None,
    priority: Optional[str] = None,
    assignedTo: Optional[str] = None,
    category: Optional[str] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Update a ticket (status, priority, assignment).
    
    Args:
        ticket_id: Ticket ID (JSM issue key)
        status: New status (open, in_progress, resolved, closed)
        priority: New priority (low, medium, high, urgent)
        assignedTo: User ID of assigned employee (admin only)
        category: New category (optional)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Updated ticket information
    """
    url = f"{API_BASE_URL}/api/tickets/{ticket_id}"
    payload = {}
    
    if status is not None:
        payload["status"] = status
    if priority is not None:
        payload["priority"] = priority
    if assignedTo is not None:
        payload["assignedTo"] = assignedTo
    if category is not None:
        payload["category"] = category
    
    response = await http_client.put(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def get_assignable_employees(
    auth_token: Optional[str] = None,
) -> list:
    """Get list of employees who can be assigned to tickets.
    
    Args:
        auth_token: Firebase authentication token (required)
    
    Returns:
        List of assignable employees
    """
    url = f"{API_BASE_URL}/api/tickets/assignable-employees"
    
    response = await http_client.get(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


# Export tools list for registration
tickets_tools = [
    create_ticket,
    get_tickets,
    get_ticket,
    update_ticket,
    get_assignable_employees,
]

