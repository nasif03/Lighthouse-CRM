"""MCP tools for Jira integration"""
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
from services.jira_service import create_jira_issue, get_jira_issues, get_jira_issue
from config.database import tickets_collection, organizations_collection, jira_integration_collection
from utils.query_filters import get_user_ids
from api.routes.jira import has_ticket_role
from api.routes.organizations import is_org_admin


async def create_jira_issue_from_ticket(
    ticket_id: str,
    context: Optional[Dict] = None
) -> str:
    """
    Create a Jira issue from a support ticket.
    
    Args:
        ticket_id: ID of the ticket to convert to Jira issue
        context: MCP request context (for authentication)
    
    Returns:
        Success message with Jira issue key
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has permission
        has_permission = is_org_admin(user_doc, org_id) or has_ticket_role(user_doc, org_id)
        if not has_permission:
            return "You do not have permission to create Jira issues for tickets."
        
        # Get ticket
        ticket = tickets_collection.find_one({"_id": ObjectId(ticket_id), "orgId": org_id})
        if not ticket:
            return "Ticket not found or you don't have permission to access it."
        
        # Check if issue already exists
        existing = jira_integration_collection.find_one({"ticketId": ticket_id})
        if existing:
            return format_success_response(
                "Jira issue already exists for this ticket",
                {
                    "issueKey": existing.get("jiraIssueKey"),
                    "issueUrl": f"https://lighthouse-crm.atlassian.net/browse/{existing.get('jiraIssueKey')}"
                }
            )
        
        # Get organization Jira project key
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org or not org.get("jiraProjectKey"):
            return "Organization does not have a Jira project. Please create one first."
        
        project_key = org.get("jiraProjectKey")
        
        # Determine issue type based on ticket category
        issue_type = "Task"
        if ticket.get("category") == "bug_report":
            issue_type = "Bug"
        elif ticket.get("category") == "feature_request":
            issue_type = "Story"
        
        # Create Jira issue
        summary = f"[{ticket.get('ticketNumber')}] {ticket.get('subject')}"
        description = f"""
Ticket Number: {ticket.get('ticketNumber')}
Customer: {ticket.get('name')} ({ticket.get('email')})
Priority: {ticket.get('priority', 'medium')}
Category: {ticket.get('category', 'N/A')}

Description:
{ticket.get('description')}
"""
        
        issue_info = create_jira_issue(project_key, summary, description, issue_type)
        
        if not issue_info:
            return "Failed to create Jira issue. Please check Jira configuration."
        
        # Store integration record
        jira_integration_collection.insert_one({
            "orgId": org_id,
            "ticketId": ticket_id,
            "jiraIssueKey": issue_info["issueKey"],
            "jiraIssueId": issue_info["issueId"],
            "jiraProjectKey": project_key,
            "syncDirection": "ticket_to_jira",
            "status": "active",
            "lastSyncedAt": datetime.utcnow(),
            "createdAt": datetime.utcnow(),
            "updatedAt": datetime.utcnow()
        })
        
        return format_success_response(
            f"Jira issue created successfully for ticket {ticket.get('ticketNumber')}",
            {
                "issueKey": issue_info["issueKey"],
                "issueUrl": issue_info["issueUrl"],
                "ticketId": ticket_id,
                "ticketNumber": ticket.get("ticketNumber")
            }
        )
    except Exception as e:
        return format_error_response(e)


async def get_jira_issues_for_project(
    project_key: Optional[str] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Get Jira issues for a project.
    
    Args:
        project_key: Jira project key (optional, uses org's project if not provided)
        context: MCP request context (for authentication)
    
    Returns:
        Formatted list of Jira issues
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has permission
        has_permission = is_org_admin(user_doc, org_id) or has_ticket_role(user_doc, org_id)
        if not has_permission:
            return "You do not have permission to view Jira issues."
        
        # Get project key from org if not provided
        if not project_key:
            org = organizations_collection.find_one({"_id": ObjectId(org_id)})
            if not org or not org.get("jiraProjectKey"):
                return "Organization does not have a Jira project configured."
            project_key = org.get("jiraProjectKey")
        
        # Get issues
        issues = get_jira_issues(project_key)
        
        if not issues:
            return f"No Jira issues found for project {project_key}."
        
        # Get ticket mappings
        integrations = list(jira_integration_collection.find({"orgId": org_id}))
        ticket_map = {intg.get("jiraIssueKey"): intg.get("ticketId") for intg in integrations}
        
        # Add ticket ID to issues
        for issue in issues:
            issue["ticketId"] = ticket_map.get(issue["key"])
        
        return format_success_response(
            f"Found {len(issues)} Jira issue(s)",
            {"issues": issues}
        )
    except Exception as e:
        return format_error_response(e)


async def sync_ticket_to_jira(
    ticket_id: str,
    context: Optional[Dict] = None
) -> str:
    """
    Sync a ticket to Jira (creates or updates Jira issue).
    
    Args:
        ticket_id: ID of the ticket to sync
        context: MCP request context (for authentication)
    
    Returns:
        Success message
    """
    # This is essentially the same as create_jira_issue_from_ticket
    # but with a different name for clarity
    return await create_jira_issue_from_ticket(ticket_id, context)

