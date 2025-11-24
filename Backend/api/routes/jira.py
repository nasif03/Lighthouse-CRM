"""Jira integration API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from api.dependencies import get_current_user
from config.database import organizations_collection, jira_integration_collection, roles_collection
from services.jira_service import (
    create_jsm_service_project,
    create_jira_software_project,
    create_jira_software_issue,
    get_jsm_service_requests,
    get_jira_software_issues,
    get_jira_issue,
    get_jsm_service_request
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
    """Create a JSM Service Project for an organization (admin only, first time)"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can create JSM projects")
        
        # Get organization
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found")
        
        # Check if JSM project already exists
        if org.get("jiraProjectKey"):
            return {
                "message": "JSM project already exists",
                "projectKey": org.get("jiraProjectKey"),
                "projectUrl": f"https://lighthouse-crm.atlassian.net/browse/{org.get('jiraProjectKey')}"
            }
        
        # Create JSM Service Project
        admin_email = user_doc.get("email")
        project_info = create_jsm_service_project(org.get("name", "Organization"), org_id, admin_email)
        
        if not project_info:
            raise HTTPException(status_code=500, detail="Failed to create JSM project")
        
        # Update organization with JSM project info
        update_data = {
            "jiraProjectKey": project_info["projectKey"],
            "jiraProjectId": project_info["projectId"],
            "updatedAt": datetime.utcnow()
        }
        
        # Add service desk ID if available
        if project_info.get("serviceDeskId"):
            update_data["jsmServiceDeskId"] = project_info["serviceDeskId"]
        
        organizations_collection.update_one(
            {"_id": ObjectId(org_id)},
            {"$set": update_data}
        )
        
        return {
            "message": "JSM Service Project created successfully",
            "projectKey": project_info["projectKey"],
            "projectName": project_info["projectName"],
            "projectUrl": project_info["projectUrl"],
            "serviceDeskId": project_info.get("serviceDeskId")
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating JSM project: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create JSM project: {str(e)}")

@router.post("/software/projects/{org_id}")
async def create_jira_software_project_for_org(
    org_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Create a Jira Software project for development (admin only, optional)"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can create Jira Software projects")
        
        # Get organization
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found")
        
        # Check if Jira Software project already exists
        if org.get("jiraSoftwareProjectKey"):
            return {
                "message": "Jira Software project already exists",
                "projectKey": org.get("jiraSoftwareProjectKey"),
                "projectUrl": f"https://lighthouse-crm.atlassian.net/browse/{org.get('jiraSoftwareProjectKey')}"
            }
        
        # Create Jira Software Project
        admin_email = user_doc.get("email")
        project_info = create_jira_software_project(org.get("name", "Organization"), org_id, admin_email)
        
        if not project_info:
            raise HTTPException(status_code=500, detail="Failed to create Jira Software project")
        
        # Update organization with Jira Software project info
        organizations_collection.update_one(
            {"_id": ObjectId(org_id)},
            {"$set": {
                "jiraSoftwareProjectKey": project_info["projectKey"],
                "jiraSoftwareProjectId": project_info["projectId"],
                "updatedAt": datetime.utcnow()
            }}
        )
        
        return {
            "message": "Jira Software project created successfully",
            "projectKey": project_info["projectKey"],
            "projectName": project_info["projectName"],
            "projectUrl": project_info["projectUrl"]
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating Jira Software project: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create Jira Software project: {str(e)}")

@router.post("/tickets/{ticket_id}/create-issue")
async def create_issue_for_ticket(
    ticket_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Create a Jira Software issue for a JSM ticket (admin or ticket role)"""
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
                detail="You do not have permission to create Jira Software issues for tickets"
            )
        
        # Get organization
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found")
        
        # Check if organization has Jira Software project
        if not org.get("jiraSoftwareProjectKey"):
            raise HTTPException(
                status_code=400,
                detail="Organization does not have a Jira Software project. Please create one first."
            )
        
        # Get JSM ticket (ticket_id is now a JSM issue key like "SR-123")
        jsm_ticket = get_jsm_service_request(ticket_id)
        if not jsm_ticket:
            raise HTTPException(status_code=404, detail="JSM ticket not found")
        
        # Verify ticket belongs to this organization's JSM project
        jsm_project_key = org.get("jiraProjectKey")
        if not jsm_project_key or not ticket_id.startswith(jsm_project_key):
            raise HTTPException(status_code=404, detail="Ticket not found in this organization")
        
        # Check if Jira Software issue already exists for this ticket
        existing = jira_integration_collection.find_one({"ticketId": ticket_id})
        if existing:
            return {
                "message": "Jira Software issue already exists for this ticket",
                "issueKey": existing.get("jiraIssueKey"),
                "issueUrl": f"https://lighthouse-crm.atlassian.net/browse/{existing.get('jiraIssueKey')}"
            }
        
        jira_software_project_key = org.get("jiraSoftwareProjectKey")
        
        # Determine issue type (default to Task, but could be Bug or Story based on ticket)
        issue_type = "Task"
        # Note: JSM doesn't store category in standard fields, so we default to Task
        # In production, you might want to add custom fields to JSM
        
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
            raise HTTPException(status_code=500, detail="Failed to create Jira Software issue")
        
        # Link JSM ticket to Jira Software issue
        from services.jira_service import link_jsm_to_jira_software
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
        
        return {
            "message": "Jira Software issue created and linked successfully",
            "issueKey": issue_info["issueKey"],
            "issueUrl": issue_info["issueUrl"],
            "jsmTicketKey": ticket_id,
            "linked": link_success
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating Jira Software issue: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create Jira Software issue: {str(e)}")

@router.get("/issues")
async def get_issues(
    project_type: str = "jsm",  # "jsm" or "software"
    current_user: dict = Depends(get_current_user)
):
    """Get all Jira issues for the current user's organization (admin or ticket role)
    
    project_type: "jsm" for JSM Service Requests, "software" for Jira Software issues
    """
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
        
        # Get organization
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org:
            return []
        
        if project_type == "jsm":
            # Get JSM Service Requests
            project_key = org.get("jiraProjectKey")
            if not project_key:
                return []
            
            issues = get_jsm_service_requests(project_key)
            
            # Get Jira Software issue links
            integrations = list(jira_integration_collection.find({"orgId": org_id}))
            jira_software_map = {intg.get("ticketId"): intg.get("jiraIssueKey") for intg in integrations}
            
            # Add linked Jira Software issue key to JSM tickets
            for issue in issues:
                issue["linkedJiraSoftwareIssue"] = jira_software_map.get(issue["key"])
        else:
            # Get Jira Software issues
            project_key = org.get("jiraSoftwareProjectKey")
            if not project_key:
                return []
            
            issues = get_jira_software_issues(project_key)
            
            # Get JSM ticket mappings
            integrations = list(jira_integration_collection.find({"orgId": org_id}))
            jsm_ticket_map = {intg.get("jiraIssueKey"): intg.get("ticketId") for intg in integrations}
            
            # Add JSM ticket key to Jira Software issues
            for issue in issues:
                issue["linkedJsmTicket"] = jsm_ticket_map.get(issue["key"])
        
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
    """Get a single Jira issue by key (works for both JSM and Jira Software issues)"""
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
        
        # Get issue (automatically detects JSM vs Jira Software)
        issue = get_jira_issue(issue_key)
        if not issue:
            raise HTTPException(status_code=404, detail="Jira issue not found")
        
        # Get linked issue/ticket mapping
        # If this is a JSM ticket, find linked Jira Software issue
        # If this is a Jira Software issue, find linked JSM ticket
        integration = jira_integration_collection.find_one({"$or": [
            {"ticketId": issue_key},  # JSM ticket key
            {"jiraIssueKey": issue_key}  # Jira Software issue key
        ]})
        
        if integration:
            if issue.get("issueType") == "Service Request":
                # This is a JSM ticket, find linked Jira Software issue
                issue["linkedJiraSoftwareIssue"] = integration.get("jiraIssueKey")
            else:
                # This is a Jira Software issue, find linked JSM ticket
                issue["linkedJsmTicket"] = integration.get("ticketId")
        
        return issue
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching Jira issue: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch Jira issue: {str(e)}")

