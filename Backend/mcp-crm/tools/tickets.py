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
from config.database import organizations_collection, roles_collection
from utils.query_filters import get_user_ids
from services.jira_service import (
    create_jsm_service_request,
    get_jsm_service_requests,
    get_jsm_service_request,
    update_jsm_service_request,
    create_jira_software_issue,
    link_jsm_to_jira_software
)
from config.database import jira_integration_collection


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
    Create a new support ticket in JSM.
    
    Args:
        subject: Ticket subject
        description: Ticket description
        name: Customer name
        email: Customer email
        priority: Priority level (low, medium, high, urgent)
        category: Ticket category (optional, if bug_report or feature_request, will also create Jira Software issue)
        phone: Customer phone (optional)
        context: MCP request context (for authentication)
    
    Returns:
        Success message with JSM ticket key
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_ids = get_user_ids_from_context(user_context)
        
        org_id = user_ids["orgId"]
        
        # Get organization JSM project
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org or not org.get("jiraProjectKey"):
            return "Organization does not have JSM project configured. Please create one first."
        
        project_key = org.get("jiraProjectKey")
        
        # Create JSM Service Request
        jsm_description = f"""Customer: {name} ({email})
Phone: {phone or 'N/A'}
Priority: {priority}
Category: {category or 'N/A'}

Description:
{description}
"""
        
        jsm_ticket = create_jsm_service_request(
            project_key=project_key,
            summary=subject,
            description=jsm_description,
            reporter_email=email,
            reporter_name=name,
            priority=priority
        )
        
        if not jsm_ticket:
            return "Failed to create ticket in JSM."
        
        jsm_issue_key = jsm_ticket["issueKey"]
        
        # If category is bug_report or feature_request, also create Jira Software issue
        jira_software_issue_key = None
        if category in ["bug_report", "feature_request"]:
            jira_software_project_key = org.get("jiraSoftwareProjectKey")
            if jira_software_project_key:
                try:
                    issue_type = "Bug" if category == "bug_report" else "Story"
                    jira_summary = f"[{jsm_issue_key}] {subject}"
                    jira_description = f"""JSM Service Request: {jsm_issue_key}
Customer: {name} ({email})
Priority: {priority}

Description:
{description}
"""
                    
                    jira_issue_info = create_jira_software_issue(
                        project_key=jira_software_project_key,
                        summary=jira_summary,
                        description=jira_description,
                        issue_type=issue_type
                    )
                    
                    if jira_issue_info:
                        jira_software_issue_key = jira_issue_info["issueKey"]
                        # Link JSM ticket to Jira Software issue
                        link_jsm_to_jira_software(jsm_issue_key, jira_software_issue_key)
                        
                        # Store integration record
                        jira_integration_collection.insert_one({
                            "orgId": org_id,
                            "ticketId": jsm_issue_key,
                            "jiraIssueKey": jira_software_issue_key,
                            "jiraIssueId": jira_issue_info["issueId"],
                            "jiraProjectKey": jira_software_project_key,
                            "syncDirection": "jsm_to_jira_software",
                            "status": "active",
                            "lastSyncedAt": datetime.utcnow(),
                            "createdAt": datetime.utcnow(),
                            "updatedAt": datetime.utcnow()
                        })
                except Exception as e:
                    print(f"Failed to create Jira Software issue: {str(e)}")
                    # Don't fail ticket creation if Jira Software creation fails
        
        result_data = {
            "ticketId": jsm_issue_key,
            "ticketNumber": jsm_issue_key,  # JSM generates keys like SR-123
            "subject": subject,
            "priority": priority
        }
        
        if jira_software_issue_key:
            result_data["jiraSoftwareIssueKey"] = jira_software_issue_key
        
        return format_success_response(
            f"Ticket created successfully in JSM: {jsm_issue_key}",
            result_data
        )
    except Exception as e:
        return format_error_response(e)


async def get_tickets(
    filters: Optional[str] = None,
    limit: int = 50,
    context: Optional[Dict] = None
) -> str:
    """
    Get support tickets from JSM.
    
    Args:
        filters: Optional filter string (e.g., "status:open,priority:high")
        limit: Maximum number of tickets to return (default: 50)
        context: MCP request context (for authentication)
    
    Returns:
        Formatted list of JSM tickets
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
        
        # Get organization JSM project
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org or not org.get("jiraProjectKey"):
            return "Organization does not have JSM project configured."
        
        project_key = org.get("jiraProjectKey")
        
        # Build JQL query
        jql_parts = [f"project = {project_key}", "issuetype = 'Service Request'"]
        
        # Parse filters and add to JQL
        if filters:
            filter_dict = parse_filters(filters)
            if filter_dict.get("status"):
                status_map = {
                    "open": "To Do",
                    "in_progress": "In Progress",
                    "resolved": "Done",
                    "closed": "Closed"
                }
                jsm_status = status_map.get(filter_dict["status"], filter_dict["status"])
                jql_parts.append(f"status = '{jsm_status}'")
            
            if filter_dict.get("priority"):
                priority_map = {
                    "low": "Low",
                    "medium": "Medium",
                    "high": "High",
                    "urgent": "Highest"
                }
                jsm_priority = priority_map.get(filter_dict["priority"], filter_dict["priority"])
                jql_parts.append(f"priority = '{jsm_priority}'")
        
        jql = " AND ".join(jql_parts) + " ORDER BY created DESC"
        
        # Get tickets from JSM
        jsm_tickets = get_jsm_service_requests(project_key, jql)
        
        if not jsm_tickets:
            return "No tickets found matching the criteria."
        
        # Limit results
        limited_tickets = jsm_tickets[:limit]
        
        # Get Jira Software issue links
        ticket_keys = [t["key"] for t in limited_tickets]
        integrations = list(jira_integration_collection.find({"ticketId": {"$in": ticket_keys}}))
        jira_software_map = {intg.get("ticketId"): intg.get("jiraIssueKey") for intg in integrations}
        
        # Format results
        results = []
        for jsm_ticket in limited_tickets:
            results.append({
                "id": jsm_ticket["key"],
                "ticketNumber": jsm_ticket["key"],
                "subject": jsm_ticket.get("summary", ""),
                "name": jsm_ticket.get("reporterName", ""),
                "email": jsm_ticket.get("reporterEmail", ""),
                "priority": jsm_ticket.get("priority", "medium"),
                "status": jsm_ticket.get("status", "open"),
                "category": None,  # JSM doesn't store category in standard fields
                "assignedTo": None,  # Would need to map JSM account ID to user ID
                "createdAt": jsm_ticket.get("created", ""),
                "jiraSoftwareIssueKey": jira_software_map.get(jsm_ticket["key"])
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
    Update the status of a JSM ticket.
    
    Args:
        ticket_id: JSM issue key (e.g., "SR-123") of the ticket to update
        status: New status (open, in_progress, resolved, closed)
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
        
        # Get organization JSM project
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org or not org.get("jiraProjectKey"):
            return "Organization does not have JSM project configured."
        
        project_key = org.get("jiraProjectKey")
        
        # Verify ticket belongs to this organization's project
        if not ticket_id.startswith(project_key):
            return "Ticket not found in this organization."
        
        # Get ticket from JSM
        jsm_ticket = get_jsm_service_request(ticket_id)
        if not jsm_ticket:
            return "Ticket not found in JSM."
        
        # Update status in JSM
        updated_ticket = update_jsm_service_request(
            issue_key=ticket_id,
            status=status
        )
        
        if not updated_ticket:
            return "Failed to update ticket status in JSM."
        
        return format_success_response(
            f"Ticket status updated to '{status}'",
            {
                "ticketId": ticket_id,
                "ticketNumber": ticket_id,
                "newStatus": status
            }
        )
    except Exception as e:
        return format_error_response(e)

