"""MCP Tools for Leads Management"""
from typing import Optional
from mcp_server.utils import API_BASE_URL, http_client, get_auth_headers


async def _find_lead_by_identifier(
    lead_id: Optional[str] = None,
    email: Optional[str] = None,
    name: Optional[str] = None,
    auth_token: Optional[str] = None,
) -> str:
    """
    Helper function to find a lead by ID, email, or name.
    Returns the lead ID.
    """
    # If lead_id is provided and doesn't look like an email, use it directly
    if lead_id and "@" not in lead_id:
        return lead_id
    
    # If lead_id looks like an email, treat it as email
    if lead_id and "@" in lead_id:
        email = lead_id
        lead_id = None
    
    # If we have an ID already, return it
    if lead_id:
        return lead_id
    
    # Need to look up by email or name
    if not email and not name:
        raise ValueError("Either lead_id, email, or name must be provided")
    
    # Get all leads and find the matching one
    url = f"{API_BASE_URL}/api/leads"
    params = {"skip": 0, "limit": 1000}
    
    response = await http_client.get(
        url,
        params=params,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    leads = response.json()
    
    # Find lead with matching email or name
    matching_lead = None
    for lead in leads:
        if email and lead.get("email", "").lower() == email.lower():
            matching_lead = lead
            break
        elif name and lead.get("name", "").lower() == name.lower():
            matching_lead = lead
            break
    
    if not matching_lead:
        identifier = email or name or lead_id
        raise ValueError(f"Lead with identifier '{identifier}' not found")
    
    found_id = matching_lead.get("id") or matching_lead.get("_id")
    if not found_id:
        raise ValueError("Lead found but missing ID")
    
    return found_id


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
    status: str,
    lead_id: Optional[str] = None,
    email: Optional[str] = None,
    name: Optional[str] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Update the status of a lead.
    
    Args:
        status: New status - must be one of: "new", "contacted", "qualified", "converted", "lost"
        lead_id: ID of the lead to update (optional if email or name is provided)
        email: Email address of the lead to update (optional if lead_id or name is provided)
        name: Name of the lead to update (optional if lead_id or email is provided)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Updated lead information
    
    Note:
        This updates the status field only. To convert a lead to Account/Contact/Deal,
        use convert_lead_to_deal instead.
    """
    # Validate status
    valid_statuses = ["new", "contacted", "qualified", "converted", "lost"]
    if status not in valid_statuses:
        raise ValueError(f"Invalid status '{status}'. Must be one of: {', '.join(valid_statuses)}")
    
    # Find the lead by ID, email, or name
    actual_lead_id = await _find_lead_by_identifier(
        lead_id=lead_id,
        email=email,
        name=name,
        auth_token=auth_token
    )
    
    url = f"{API_BASE_URL}/api/leads/{actual_lead_id}/status"
    payload = {"status": status}
    
    response = await http_client.patch(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def convert_lead_to_deal(
    lead_id: Optional[str] = None,
    email: Optional[str] = None,
    name: Optional[str] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Convert a lead to Account, Contact, and Deal.
    
    This creates a new Account, Contact, and Deal from the lead information.
    This is different from updating the lead status to "converted" - this actually
    creates the related entities in the CRM.
    
    Args:
        lead_id: ID of the lead to convert (optional if email or name is provided)
        email: Email address of the lead to convert (optional if lead_id or name is provided)
        name: Name of the lead to convert (optional if lead_id or email is provided)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Conversion result with accountId, contactId, and dealId
    
    Note:
        This is different from update_lead_status(status="converted").
        This function creates Account, Contact, and Deal entities.
        To just change the status field, use update_lead_status instead.
    """
    # Find the lead by ID, email, or name
    actual_lead_id = await _find_lead_by_identifier(
        lead_id=lead_id,
        email=email,
        name=name,
        auth_token=auth_token
    )
    
    # Now convert using the lead_id
    url = f"{API_BASE_URL}/api/leads/{actual_lead_id}/convert"
    
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

