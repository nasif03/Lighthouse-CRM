"""MCP tools for support ticket management"""
from typing import Any, Dict, Optional
from datetime import datetime
from bson import ObjectId
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
get_user_ids_from_context = mcp_crm_utils.get_user_ids_from_context
format_error_response = mcp_crm_utils.format_error_response
format_success_response = mcp_crm_utils.format_success_response
parse_filters = mcp_crm_utils.parse_filters
from config.database import tickets_collection, organizations_collection, roles_collection
from utils.query_filters import get_user_ids


def has_ticket_role(user_doc: dict, org_id: str) -> bool:
    """Check if user has ticket-related role"""
    user_id = str(user_doc["_id"])
    
    # Check if user is admin
    org = organizations_collection.find_one({"_id": ObjectId(org_id)})
    if org and user_id in org.get("admins", []):
        return True
    
    # Check if user belongs to this organization
    user_org_ids = user_doc.get("orgId", [])
    if isinstance(user_org_ids, str):
        user_org_ids = [user_org_ids]
    
    if org_id not in user_org_ids:
        return False
    
    # Check if user has roles with ticket permissions
    role_ids = user_doc.get("roleIds", [])
    if not role_ids:
        return False
    
    roles = list(roles_collection.find({
        "_id": {"$in": [ObjectId(rid) for rid in role_ids if ObjectId.is_valid(rid)]},
        "orgId": org_id
    }))
    
    ticket_permissions = ["read:tickets", "write:tickets", "admin:tickets"]
    for role in roles:
        permissions = role.get("permissions", [])
        if any(perm in permissions for perm in ticket_permissions):
            return True
    
    return False


def generate_ticket_number(org_id: str) -> str:
    """Generate a unique ticket number"""
    date_prefix = datetime.utcnow().strftime("%Y%m%d")
    today_prefix = f"TKT-{date_prefix}-"
    
    today_tickets = tickets_collection.find({
        "orgId": org_id,
        "ticketNumber": {"$regex": f"^{today_prefix}"}
    }).sort("ticketNumber", -1).limit(1)
    
    last_ticket = list(today_tickets)
    if last_ticket and last_ticket[0].get("ticketNumber"):
        last_num = last_ticket[0]["ticketNumber"].split("-")[-1]
        try:
            next_num = int(last_num) + 1
        except ValueError:
            next_num = 1
    else:
        next_num = 1
    
    return f"{today_prefix}{next_num:04d}"


async def create_ticket(
    subject: str,
    description: str,
    name: str,
    email: str,
    priority: str = "medium",
    category: Optional[str] = None,
    phone: Optional[str] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Create a new support ticket.
    
    Args:
        subject: Ticket subject
        description: Ticket description
        name: Customer name
        email: Customer email
        priority: Priority level (low, medium, high, urgent)
        category: Ticket category (optional)
        phone: Customer phone (optional)
        context: MCP request context (for authentication)
    
    Returns:
        Success message with ticket ID
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_ids = get_user_ids_from_context(user_context)
        
        org_id = user_ids["orgId"]
        
        now = datetime.utcnow()
        ticket_number = generate_ticket_number(org_id)
        
        ticket_data = {
            "ticketNumber": ticket_number,
            "orgId": org_id,
            "name": name,
            "email": email,
            "phone": phone or "",
            "subject": subject,
            "description": description,
            "priority": priority,
            "category": category,
            "status": "open",
            "assignedTo": None,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = tickets_collection.insert_one(ticket_data)
        ticket_id = str(result.inserted_id)
        
        return format_success_response(
            f"Ticket created successfully: {ticket_number}",
            {
                "ticketId": ticket_id,
                "ticketNumber": ticket_number,
                "subject": subject,
                "priority": priority
            }
        )
    except Exception as e:
        return format_error_response(e)


async def get_tickets(
    filters: Optional[str] = None,
    limit: int = 50,
    context: Optional[Dict] = None
) -> str:
    """
    Get support tickets.
    
    Args:
        filters: Optional filter string (e.g., "status:open,priority:high")
        limit: Maximum number of tickets to return (default: 50)
        context: MCP request context (for authentication)
    
    Returns:
        Formatted list of tickets
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has ticket role
        if not has_ticket_role(user_doc, org_id):
            return "You do not have permission to view tickets. Contact your administrator."
        
        # Build filter
        query_filter = {"orgId": org_id}
        
        # Add additional filters
        if filters:
            additional = parse_filters(filters)
            query_filter.update(additional)
        
        # Query tickets
        cursor = tickets_collection.find(query_filter).sort("createdAt", -1).limit(limit)
        tickets = list(cursor)
        
        if not tickets:
            return "No tickets found matching the criteria."
        
        # Format results
        results = []
        for ticket in tickets:
            results.append({
                "id": str(ticket["_id"]),
                "ticketNumber": ticket.get("ticketNumber", ""),
                "subject": ticket.get("subject", ""),
                "name": ticket.get("name", ""),
                "email": ticket.get("email", ""),
                "priority": ticket.get("priority", "medium"),
                "status": ticket.get("status", "open"),
                "category": ticket.get("category"),
                "assignedTo": ticket.get("assignedTo"),
                "createdAt": ticket.get("createdAt").isoformat() if ticket.get("createdAt") else ""
            })
        
        return format_success_response(
            f"Found {len(results)} ticket(s)",
            {"tickets": results}
        )
    except Exception as e:
        return format_error_response(e)


async def update_ticket_status(
    ticket_id: str,
    status: str,
    context: Optional[Dict] = None
) -> str:
    """
    Update the status of a ticket.
    
    Args:
        ticket_id: ID of the ticket to update
        status: New status (open, in-progress, resolved, closed)
        context: MCP request context (for authentication)
    
    Returns:
        Success message
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has ticket role
        if not has_ticket_role(user_doc, org_id):
            return "You do not have permission to update tickets."
        
        # Check if ticket exists
        ticket = tickets_collection.find_one({"_id": ObjectId(ticket_id), "orgId": org_id})
        if not ticket:
            return "Ticket not found or you don't have permission to update it."
        
        # Update status
        tickets_collection.update_one(
            {"_id": ObjectId(ticket_id)},
            {"$set": {"status": status, "updatedAt": datetime.utcnow()}}
        )
        
        return format_success_response(
            f"Ticket status updated to '{status}'",
            {
                "ticketId": ticket_id,
                "ticketNumber": ticket.get("ticketNumber"),
                "newStatus": status
            }
        )
    except Exception as e:
        return format_error_response(e)

