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
from services.jira_service import (
    create_jira_software_issue,
    get_jsm_service_requests,
    get_jira_software_issues,
    get_jira_issue,
    get_jsm_service_request,
    link_jsm_to_jira_software
)
from config.database import organizations_collection, jira_integration_collection
from utils.query_filters import get_user_ids
from api.routes.jira import has_ticket_role
from api.routes.organizations import is_org_admin


async def create_jira_issue_from_ticket(
    ticket_id: str,
    context: Optional[Dict] = None
) -> str:
    """
    Create a Jira Software issue from a JSM ticket.
    
    Args:
        ticket_id: JSM issue key (e.g., "SR-123") of the ticket to convert to Jira Software issue
        context: MCP request context (for authentication)
    
    Returns:
        Success message with Jira Software issue key
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
            return "You do not have permission to create Jira Software issues for tickets."
        
        # Get organization
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org:
            return "Organization not found."
        
        # Check if organization has Jira Software project
        if not org.get("jiraSoftwareProjectKey"):
            return "Organization does not have a Jira Software project. Please create one first."
        
        # Get JSM ticket (ticket_id is now a JSM issue key)
        jsm_ticket = get_jsm_service_request(ticket_id)
        if not jsm_ticket:
            return "JSM ticket not found."
        
        # Verify ticket belongs to this organization's JSM project
        jsm_project_key = org.get("jiraProjectKey")
        if not jsm_project_key or not ticket_id.startswith(jsm_project_key):
            return "Ticket not found in this organization."
        
        # Check if Jira Software issue already exists for this ticket
        existing = jira_integration_collection.find_one({"ticketId": ticket_id})
        if existing:
            return format_success_response(
                "Jira Software issue already exists for this ticket",
                {
                    "issueKey": existing.get("jiraIssueKey"),
                    "issueUrl": f"https://lighthouse-crm.atlassian.net/browse/{existing.get('jiraIssueKey')}"
                }
            )
        
        jira_software_project_key = org.get("jiraSoftwareProjectKey")
        
        # Determine issue type (default to Task, but could be Bug or Story)
        issue_type = "Task"
        # Note: JSM doesn't store category in standard fields, so we default to Task
        
        # Create Jira Software issue
        summary = f"[{jsm_ticket.get('key')}] {jsm_ticket.get('summary')}"
        description = f"""JSM Service Request: {jsm_ticket.get('key')}
Customer: {jsm_ticket.get('reporterName')} ({jsm_ticket.get('reporterEmail')})
Priority: {jsm_ticket.get('priorityName', 'Medium')}

Description:
{jsm_ticket.get('description')}
"""
        
        issue_info = create_jira_software_issue(
            project_key=jira_software_project_key,
            summary=summary,
            description=description,
            issue_type=issue_type
        )
        
        if not issue_info:
            return "Failed to create Jira Software issue. Please check Jira configuration."
        
        # Link JSM ticket to Jira Software issue
        link_success = link_jsm_to_jira_software(ticket_id, issue_info["issueKey"])
        
        # Store integration record
        jira_integration_collection.insert_one({
            "orgId": org_id,
            "ticketId": ticket_id,  # JSM issue key
            "jiraIssueKey": issue_info["issueKey"],  # Jira Software issue key
            "jiraIssueId": issue_info["issueId"],
            "jiraProjectKey": jira_software_project_key,
            "syncDirection": "jsm_to_jira_software",
            "status": "active",
            "lastSyncedAt": datetime.utcnow(),
            "createdAt": datetime.utcnow(),
            "updatedAt": datetime.utcnow()
        })
        
        return format_success_response(
            f"Jira Software issue created successfully for JSM ticket {ticket_id}",
            {
                "issueKey": issue_info["issueKey"],
                "issueUrl": issue_info["issueUrl"],
                "jsmTicketKey": ticket_id,
                "linked": link_success
            }
        )
    except Exception as e:
        return format_error_response(e)


async def get_jira_issues_for_project(
    project_type: str = "jsm",  # "jsm" or "software"
    project_key: Optional[str] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Get Jira issues for a project (JSM Service Requests or Jira Software issues).
    
    Args:
        project_type: "jsm" for JSM Service Requests, "software" for Jira Software issues
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
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org:
            return "Organization not found."
        
        if not project_key:
            if project_type == "jsm":
                if not org.get("jiraProjectKey"):
                    return "Organization does not have a JSM project configured."
                project_key = org.get("jiraProjectKey")
            else:
                if not org.get("jiraSoftwareProjectKey"):
                    return "Organization does not have a Jira Software project configured."
                project_key = org.get("jiraSoftwareProjectKey")
        
        # Get issues based on project type
        if project_type == "jsm":
            issues = get_jsm_service_requests(project_key)
            
            # Get Jira Software issue links
            integrations = list(jira_integration_collection.find({"orgId": org_id}))
            jira_software_map = {intg.get("ticketId"): intg.get("jiraIssueKey") for intg in integrations}
            
            # Add linked Jira Software issue key to JSM tickets
            for issue in issues:
                issue["linkedJiraSoftwareIssue"] = jira_software_map.get(issue["key"])
        else:
            issues = get_jira_software_issues(project_key)
            
            # Get JSM ticket mappings
            integrations = list(jira_integration_collection.find({"orgId": org_id}))
            jsm_ticket_map = {intg.get("jiraIssueKey"): intg.get("ticketId") for intg in integrations}
            
            # Add JSM ticket key to Jira Software issues
            for issue in issues:
                issue["linkedJsmTicket"] = jsm_ticket_map.get(issue["key"])
        
        if not issues:
            return f"No {project_type} issues found for project {project_key}."
        
        return format_success_response(
            f"Found {len(issues)} {project_type} issue(s)",
            {"issues": issues}
        )
    except Exception as e:
        return format_error_response(e)


async def sync_ticket_to_jira(
    ticket_id: str,
    context: Optional[Dict] = None
) -> str:
    """
    Sync a JSM ticket to Jira Software (creates Jira Software issue from JSM ticket).
    
    Args:
        ticket_id: JSM issue key (e.g., "SR-123") of the ticket to sync
        context: MCP request context (for authentication)
    
    Returns:
        Success message
    """
    # This is essentially the same as create_jira_issue_from_ticket
    # but with a different name for clarity
    return await create_jira_issue_from_ticket(ticket_id, context)

