"""MCP Tools for Leads Management"""
from typing import Optional
from mcp_server.utils import API_BASE_URL, http_client, get_auth_headers


async def create_lead(
    name: str,
    email: str,
    source: str,
    status: str = "new",
    phone: Optional[str] = None,
    firstName: Optional[str] = None,
    lastName: Optional[str] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Create a new lead in the CRM.
    
    Args:
        name: Full name of the lead
        email: Email address of the lead
        source: Source of the lead (e.g., "website", "referral", "cold_call")
        status: Status of the lead (new, contacted, qualified, converted, lost). Defaults to "new"
        phone: Phone number (optional)
        firstName: First name (optional, will be parsed from name if not provided)
        lastName: Last name (optional, will be parsed from name if not provided)
        auth_token: Firebase authentication token (required for authenticated requests)
    
    Returns:
        Created lead information
    """
    url = f"{API_BASE_URL}/api/leads"
    payload = {
        "name": name,
        "email": email,
        "source": source,
        "status": status,
    }
    if phone:
        payload["phone"] = phone
    if firstName:
        payload["firstName"] = firstName
    if lastName:
        payload["lastName"] = lastName
    
    response = await http_client.post(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def get_leads(
    skip: int = 0,
    limit: int = 100,
    auth_token: Optional[str] = None,
) -> list:
    """Get all leads for the current user's organization.
    
    Args:
        skip: Number of leads to skip (for pagination)
        limit: Maximum number of leads to return (default: 100)
        auth_token: Firebase authentication token (required)
    
    Returns:
        List of leads
    """
    url = f"{API_BASE_URL}/api/leads"
    params = {"skip": skip, "limit": limit}
    
    response = await http_client.get(
        url,
        params=params,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def update_lead_status(
    lead_id: str,
    status: str,
    auth_token: Optional[str] = None,
) -> dict:
    """Update the status of a lead.
    
    Args:
        lead_id: ID of the lead to update
        status: New status (new, contacted, qualified, converted, lost)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Updated lead information
    """
    url = f"{API_BASE_URL}/api/leads/{lead_id}/status"
    payload = {"status": status}
    
    response = await http_client.patch(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def convert_lead_to_deal(
    lead_id: str,
    auth_token: Optional[str] = None,
) -> dict:
    """Convert a lead to Account, Contact, and Deal.
    
    Args:
        lead_id: ID of the lead to convert
        auth_token: Firebase authentication token (required)
    
    Returns:
        Conversion result with accountId, contactId, and dealId
    """
    url = f"{API_BASE_URL}/api/leads/{lead_id}/convert"
    
    response = await http_client.post(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def delete_lead(
    lead_id: str,
    auth_token: Optional[str] = None,
) -> dict:
    """Delete a lead.
    
    Args:
        lead_id: ID of the lead to delete
        auth_token: Firebase authentication token (required)
    
    Returns:
        Success message
    """
    url = f"{API_BASE_URL}/api/leads/{lead_id}"
    
    response = await http_client.delete(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


# Export tools list for registration
leads_tools = [
    create_lead,
    get_leads,
    update_lead_status,
    convert_lead_to_deal,
    delete_lead,
]

