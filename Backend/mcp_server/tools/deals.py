"""MCP Tools for Deals Management"""
from typing import Optional
from mcp_server.utils import API_BASE_URL, http_client, get_auth_headers


async def create_deal(
    name: str,
    accountId: Optional[str] = None,
    contactId: Optional[str] = None,
    amount: Optional[float] = None,
    currency: str = "USD",
    stageId: Optional[str] = None,
    stageName: Optional[str] = None,
    probability: Optional[float] = None,
    closeDate: Optional[str] = None,
    status: str = "open",
    tags: Optional[list[str]] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Create a new deal in the CRM.
    
    Args:
        name: Name of the deal
        accountId: Associated account ID (optional)
        contactId: Associated contact ID (optional)
        amount: Deal amount (optional)
        currency: Currency code (default: USD)
        stageId: Sales stage ID (optional)
        stageName: Sales stage name (optional)
        probability: Probability percentage (0-100, optional)
        closeDate: Expected close date in ISO format (optional)
        status: Deal status (default: open)
        tags: List of tags (optional)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Created deal information
    """
    url = f"{API_BASE_URL}/api/deals"
    payload = {
        "name": name,
        "currency": currency,
        "status": status,
    }
    if accountId:
        payload["accountId"] = accountId
    if contactId:
        payload["contactId"] = contactId
    if amount is not None:
        payload["amount"] = amount
    if stageId:
        payload["stageId"] = stageId
    if stageName:
        payload["stageName"] = stageName
    if probability is not None:
        payload["probability"] = probability
    if closeDate:
        payload["closeDate"] = closeDate
    if tags:
        payload["tags"] = tags
    
    response = await http_client.post(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def get_deals(
    skip: int = 0,
    limit: int = 100,
    auth_token: Optional[str] = None,
) -> list:
    """Get all deals for the current user's organization.
    
    Args:
        skip: Number of deals to skip (for pagination)
        limit: Maximum number of deals to return (default: 100)
        auth_token: Firebase authentication token (required)
    
    Returns:
        List of deals
    """
    url = f"{API_BASE_URL}/api/deals"
    params = {"skip": skip, "limit": limit}
    
    response = await http_client.get(
        url,
        params=params,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def update_deal(
    deal_id: str,
    name: Optional[str] = None,
    accountId: Optional[str] = None,
    contactId: Optional[str] = None,
    amount: Optional[float] = None,
    currency: Optional[str] = None,
    stageId: Optional[str] = None,
    stageName: Optional[str] = None,
    probability: Optional[float] = None,
    closeDate: Optional[str] = None,
    status: Optional[str] = None,
    tags: Optional[list[str]] = None,
    auth_token: Optional[str] = None,
) -> dict:
    """Update a deal.
    
    Args:
        deal_id: ID of the deal to update
        name: New name (optional)
        accountId: New account ID (optional)
        contactId: New contact ID (optional)
        amount: New amount (optional)
        currency: New currency (optional)
        stageId: New stage ID (optional)
        stageName: New stage name (optional)
        probability: New probability (optional)
        closeDate: New close date in ISO format (optional)
        status: New status (optional)
        tags: New tags list (optional)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Updated deal information
    """
    url = f"{API_BASE_URL}/api/deals/{deal_id}"
    payload = {}
    
    if name is not None:
        payload["name"] = name
    if accountId is not None:
        payload["accountId"] = accountId
    if contactId is not None:
        payload["contactId"] = contactId
    if amount is not None:
        payload["amount"] = amount
    if currency is not None:
        payload["currency"] = currency
    if stageId is not None:
        payload["stageId"] = stageId
    if stageName is not None:
        payload["stageName"] = stageName
    if probability is not None:
        payload["probability"] = probability
    if closeDate is not None:
        payload["closeDate"] = closeDate
    if status is not None:
        payload["status"] = status
    if tags is not None:
        payload["tags"] = tags
    
    response = await http_client.put(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def delete_deal(
    deal_id: str,
    auth_token: Optional[str] = None,
) -> dict:
    """Delete a deal.
    
    Args:
        deal_id: ID of the deal to delete
        auth_token: Firebase authentication token (required)
    
    Returns:
        Success message
    """
    url = f"{API_BASE_URL}/api/deals/{deal_id}"
    
    response = await http_client.delete(
        url,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


# Export tools list for registration
deals_tools = [
    create_deal,
    get_deals,
    update_deal,
    delete_deal,
]

