"""MCP tools for Gmail integration"""
from typing import Any, Dict, Optional
# Import utils using path workaround
import sys
from pathlib import Path
backend_dir = Path(__file__).parent.parent.parent
sys.path.insert(0, str(backend_dir))
import importlib.util
spec = importlib.util.spec_from_file_location("mcp_crm_utils", backend_dir / "mcp-crm" / "utils.py")
mcp_crm_utils = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mcp_crm_utils)
get_user_context = mcp_crm_utils.get_user_context
validate_user_context = mcp_crm_utils.validate_user_context
format_error_response = mcp_crm_utils.format_error_response
format_success_response = mcp_crm_utils.format_success_response

from services.gmail import get_messages, send_message, is_authenticated

# Import leads tool
spec = importlib.util.spec_from_file_location("mcp_crm_leads", backend_dir / "mcp-crm" / "tools" / "leads.py")
mcp_crm_leads = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mcp_crm_leads)
create_lead = mcp_crm_leads.create_lead


async def send_email(
    to: str,
    subject: str,
    body: str,
    context: Optional[Dict] = None
) -> str:
    """
    Send an email via Gmail.
    
    Args:
        to: Recipient email address
        subject: Email subject
        body: Email body/content
        context: MCP request context (for authentication)
    
    Returns:
        Success message with email ID
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        
        email = user_context.get("email")
        if not email:
            return "User email not found in context."
        
        # Check if Gmail is authenticated
        if not is_authenticated(email):
            return "Gmail is not authenticated. Please authenticate Gmail first through the CRM interface."
        
        # Send email
        result = send_message(email, to, subject, body)
        
        return format_success_response(
            f"Email sent successfully to {to}",
            {
                "messageId": result.get("id"),
                "threadId": result.get("threadId"),
                "to": to,
                "subject": subject
            }
        )
    except Exception as e:
        return format_error_response(e)


async def get_emails(
    query: str = "",
    limit: int = 10,
    context: Optional[Dict] = None
) -> str:
    """
    Search and retrieve Gmail messages.
    
    Args:
        query: Gmail search query (e.g., "from:example@gmail.com", "subject:meeting")
        limit: Maximum number of emails to return (default: 10)
        context: MCP request context (for authentication)
    
    Returns:
        Formatted list of emails
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        
        email = user_context.get("email")
        if not email:
            return "User email not found in context."
        
        # Check if Gmail is authenticated
        if not is_authenticated(email):
            return "Gmail is not authenticated. Please authenticate Gmail first through the CRM interface."
        
        # Get messages
        messages = get_messages(email, max_results=limit, query=query)
        
        if not messages:
            return "No emails found matching the query."
        
        # Format results
        results = []
        for msg in messages:
            results.append({
                "id": msg.get("id"),
                "subject": msg.get("subject", ""),
                "from": msg.get("from", ""),
                "date": msg.get("date", ""),
                "snippet": msg.get("snippet", ""),
                "body": msg.get("body", "")[:500]  # Truncate body for display
            })
        
        return format_success_response(
            f"Found {len(results)} email(s)",
            {"emails": results}
        )
    except Exception as e:
        return format_error_response(e)


async def create_lead_from_email(
    email_id: str,
    context: Optional[Dict] = None
) -> str:
    """
    Extract lead information from a Gmail email and create a lead in the CRM.
    
    Args:
        email_id: Gmail message ID
        context: MCP request context (for authentication)
    
    Returns:
        Success message with created lead ID
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        
        email = user_context.get("email")
        if not email:
            return "User email not found in context."
        
        # Check if Gmail is authenticated
        if not is_authenticated(email):
            return "Gmail is not authenticated. Please authenticate Gmail first."
        
        # Get the specific email
        messages = get_messages(email, max_results=1, query=f"rfc822msgid:{email_id}")
        if not messages:
            # Try to get by ID directly
            messages = get_messages(email, max_results=100)
            messages = [m for m in messages if m.get("id") == email_id]
        
        if not messages:
            return f"Email with ID {email_id} not found."
        
        message = messages[0]
        
        # Extract information
        from_email = message.get("from", "")
        subject = message.get("subject", "")
        body = message.get("body", "")
        
        # Parse email address from "Name <email@example.com>" format
        email_address = from_email
        if "<" in from_email and ">" in from_email:
            email_address = from_email.split("<")[1].split(">")[0].strip()
            name = from_email.split("<")[0].strip()
        else:
            name = email_address.split("@")[0]
        
        # Create lead
        lead_result = await create_lead(
            name=name,
            email=email_address,
            source="email",
            status="new",
            context=context
        )
        
        return format_success_response(
            f"Lead created from email: {subject}",
            {
                "emailId": email_id,
                "from": from_email,
                "subject": subject,
                "leadCreated": lead_result
            }
        )
    except Exception as e:
        return format_error_response(e)

