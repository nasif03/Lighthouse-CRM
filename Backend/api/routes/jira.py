"""Jira integration API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from api.dependencies import get_current_user
from config.database import organizations_collection, jira_integration_collection, tickets_collection, roles_collection
from services.jira_service import (
    create_jira_project,
    create_jira_issue,
    get_jira_issues,
    get_jira_issue
)
from api.routes.organizations import is_org_admin
from utils.query_filters import get_user_ids

def has_ticket_role(user_doc: dict, org_id: str) -> bool:
    """Check if user has ticket-related role (read:tickets or write:tickets permission)"""
    user_id = str(user_doc["_id"])
    
    # Check if user is admin of the organization
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
    
    # Check if any of the user's roles have ticket permissions
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

router = APIRouter(prefix="/api/jira", tags=["jira"])

@router.post("/projects/{org_id}")
async def create_project_for_org(
    org_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Create a Jira project for an organization (admin only, first time)"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can create Jira projects")
        
        # Get organization
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found")
        
        # Check if Jira project already exists
        if org.get("jiraProjectKey"):
            return {
                "message": "Jira project already exists",
                "projectKey": org.get("jiraProjectKey"),
                "projectUrl": f"https://lighthouse-crm.atlassian.net/browse/{org.get('jiraProjectKey')}"
            }
        
        # Create Jira project
        admin_email = user_doc.get("email")
        project_info = create_jira_project(org.get("name", "Organization"), org_id, admin_email)
        
        if not project_info:
            raise HTTPException(status_code=500, detail="Failed to create Jira project")
        
        # Update organization with Jira project key
        organizations_collection.update_one(
            {"_id": ObjectId(org_id)},
            {"$set": {
                "jiraProjectKey": project_info["projectKey"],
                "jiraProjectId": project_info["projectId"],
                "updatedAt": datetime.utcnow()
            }}
        )
        
        return {
            "message": "Jira project created successfully",
            "projectKey": project_info["projectKey"],
            "projectName": project_info["projectName"],
            "projectUrl": project_info["projectUrl"]
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating Jira project: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create Jira project: {str(e)}")

@router.post("/tickets/{ticket_id}/create-issue")
async def create_issue_for_ticket(
    ticket_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Create a Jira issue for a ticket (admin or ticket role)"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Get ticket
        ticket = tickets_collection.find_one({"_id": ObjectId(ticket_id)})
        if not ticket:
            raise HTTPException(status_code=404, detail="Ticket not found")
        
        org_id = ticket.get("orgId")
        
        # Check if user has permission (admin or ticket role)
        has_permission = is_org_admin(user_doc, org_id) or has_ticket_role(user_doc, org_id)
        if not has_permission:
            raise HTTPException(
                status_code=403,
                detail="You do not have permission to create Jira issues for tickets"
            )
        
        # Check if issue already exists
        existing = jira_integration_collection.find_one({"ticketId": ticket_id})
        if existing:
            return {
                "message": "Jira issue already exists for this ticket",
                "issueKey": existing.get("jiraIssueKey"),
                "issueUrl": f"https://lighthouse-crm.atlassian.net/browse/{existing.get('jiraIssueKey')}"
            }
        
        # Get organization Jira project key
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org or not org.get("jiraProjectKey"):
            raise HTTPException(
                status_code=400,
                detail="Organization does not have a Jira project. Please create one first."
            )
        
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
            raise HTTPException(status_code=500, detail="Failed to create Jira issue")
        
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
        
        return {
            "message": "Jira issue created successfully",
            "issueKey": issue_info["issueKey"],
            "issueUrl": issue_info["issueUrl"]
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating Jira issue: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create Jira issue: {str(e)}")

@router.get("/issues")
async def get_issues(
    current_user: dict = Depends(get_current_user)
):
    """Get all Jira issues for the current user's organization (admin or ticket role)"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Get organization ID
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has permission (admin or ticket role)
        has_permission = is_org_admin(user_doc, org_id) or has_ticket_role(user_doc, org_id)
        if not has_permission:
            raise HTTPException(
                status_code=403,
                detail="You do not have permission to view Jira issues"
            )
        
        # Get organization Jira project key
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org or not org.get("jiraProjectKey"):
            return []
        
        project_key = org.get("jiraProjectKey")
        
        # Get Jira issues
        issues = get_jira_issues(project_key)
        
        # Get ticket mappings
        integrations = list(jira_integration_collection.find({"orgId": org_id}))
        ticket_map = {intg.get("jiraIssueKey"): intg.get("ticketId") for intg in integrations}
        
        # Add ticket ID to issues
        for issue in issues:
            issue["ticketId"] = ticket_map.get(issue["key"])
        
        return issues
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching Jira issues: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch Jira issues: {str(e)}")

@router.get("/issues/{issue_key}")
async def get_issue(
    issue_key: str,
    current_user: dict = Depends(get_current_user)
):
    """Get a single Jira issue by key (admin or ticket role)"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Get organization ID
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has permission (admin or ticket role)
        has_permission = is_org_admin(user_doc, org_id) or has_ticket_role(user_doc, org_id)
        if not has_permission:
            raise HTTPException(
                status_code=403,
                detail="You do not have permission to view Jira issues"
            )
        
        # Get issue
        issue = get_jira_issue(issue_key)
        if not issue:
            raise HTTPException(status_code=404, detail="Jira issue not found")
        
        # Get ticket mapping
        integration = jira_integration_collection.find_one({"jiraIssueKey": issue_key})
        if integration:
            issue["ticketId"] = integration.get("ticketId")
        
        return issue
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching Jira issue: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch Jira issue: {str(e)}")

