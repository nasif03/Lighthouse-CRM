"""MCP Tools for Calendar Management"""
from typing import Optional, List
from mcp_server.utils import API_BASE_URL, http_client, get_auth_headers


async def create_meeting(
    title: str,
    start_time: str,
    end_time: str,
    attendees: Optional[List[str]] = None,
    description: Optional[str] = None,
    timezone: str = "UTC",
    auth_token: Optional[str] = None,
) -> dict:
    """Create a calendar meeting/event.
    
    Args:
        title: Meeting title
        start_time: Start time in ISO 8601 format
        end_time: End time in ISO 8601 format
        attendees: List of attendee email addresses (optional)
        description: Meeting description (optional)
        timezone: IANA timezone (e.g., UTC, Asia/Dhaka). Defaults to UTC
        auth_token: Firebase authentication token (required)
    
    Returns:
        Created meeting information with event_id, hangout_link, etc.
    """
    url = f"{API_BASE_URL}/api/calendar/meetings"
    payload = {
        "title": title,
        "start_time": start_time,
        "end_time": end_time,
        "timezone": timezone,
    }
    if attendees:
        payload["attendees"] = attendees
    if description:
        payload["description"] = description
    
    response = await http_client.post(
        url,
        json=payload,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


async def get_meetings(
    time_min: Optional[str] = None,
    time_max: Optional[str] = None,
    max_results: int = 50,
    auth_token: Optional[str] = None,
) -> dict:
    """Get upcoming meetings from Google Calendar.
    
    Args:
        time_min: Minimum time in ISO 8601 format (optional)
        time_max: Maximum time in ISO 8601 format (optional)
        max_results: Maximum number of results (default: 50)
        auth_token: Firebase authentication token (required)
    
    Returns:
        Dictionary with "meetings" list containing meeting information
    """
    url = f"{API_BASE_URL}/api/calendar/meetings"
    params = {"max_results": max_results}
    if time_min:
        params["time_min"] = time_min
    if time_max:
        params["time_max"] = time_max
    
    response = await http_client.get(
        url,
        params=params,
        headers=get_auth_headers(auth_token)
    )
    response.raise_for_status()
    return response.json()


# Export tools list for registration
calendar_tools = [
    create_meeting,
    get_meetings,
]

