"""Support tickets API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from typing import Optional
from models.ticket import CreateTicketRequest, TicketResponse, UpdateTicketRequest, AssignableEmployeeResponse
from api.dependencies import get_current_user
from config.database import organizations_collection, users_collection, roles_collection, jira_integration_collection
from config.settings import JIRA_SERVER
from utils.query_filters import get_user_ids, build_user_filter
from services.jira_service import (
    create_jsm_service_request,
    get_jsm_service_requests,
    get_jsm_service_request,
    update_jsm_service_request,
    create_jira_software_issue,
    link_jsm_to_jira_software
)

router = APIRouter(prefix="/api/tickets", tags=["tickets"])

def get_assigned_user_name(user_id: Optional[str]) -> Optional[str]:
    """Get assigned user's name"""
    if not user_id:
        return None
    try:
        user = users_collection.find_one({"_id": ObjectId(user_id)})
        if user:
            return user.get("name", "Unknown")
    except:
        pass
    return None

def get_user_id_from_jsm_account_id(jsm_account_id: Optional[str], org_id: str) -> Optional[str]:
    """Map JSM account ID to CRM user ID"""
    if not jsm_account_id:
        return None
    try:
        # Try to find user by email (JSM account ID might match email)
        # For now, we'll need to store JSM account IDs in user documents or use email matching
        # This is a simplified version - in production, you'd want to store JSM account IDs
        users = list(users_collection.find({
            "orgId": org_id
        }))
        # TODO: Implement proper JSM account ID mapping
        # For now, return None and handle assignment differently
        return None
    except:
        return None

def get_jsm_account_id_from_user_id(user_id: Optional[str]) -> Optional[str]:
    """Map CRM user ID to JSM account ID"""
    if not user_id:
        return None
    try:
        user = users_collection.find_one({"_id": ObjectId(user_id)})
        if user:
            # TODO: Store JSM account ID in user document
            # For now, we'll need to look it up via email
            email = user.get("email")
            if email:
                from services.jira_service import get_user_account_id
                return get_user_account_id(email)
        return None
    except:
        return None

def has_ticket_role(user_doc: dict, org_id: str) -> bool:
    """Check if user has ticket-related role (read:tickets, write:tickets, or admin:tickets permission)"""
    # Import here to avoid circular dependency
    from utils.permissions import is_super_admin
    
    # Super admin bypasses all permission checks
    if is_super_admin(user_doc):
        return True
    
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


def has_ticket_admin_permission(user_doc: dict, org_id: str) -> bool:
    """
    Check if user has admin-level ticket permission.

    This uses the shared permission utility so that either:
    - Org admins
    - Super admins
    - Or users with a role containing 'admin:tickets'
    are all treated as ticket administrators.
    """
    from utils.permissions import has_permission

    return has_permission(user_doc, org_id, ["admin:tickets"])

@router.get("", response_model=list[TicketResponse])
async def get_tickets(
    skip: int = 0,
    limit: int = 100,
    status: str = None,
    priority: str = None,
    assignedTo: str = None,
    current_user: dict = Depends(get_current_user)
):
    """Get all tickets for the current user's organization - requires ticket role"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Get organization ID
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has ticket role
        if not has_ticket_role(user_doc, org_id):
            raise HTTPException(
                status_code=403, 
                detail="You do not have permission to view tickets. Contact your administrator to assign you a role with ticket permissions."
            )
        
        # Get organization JSM project
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org or not org.get("jiraProjectKey"):
            return []  # No JSM project configured
        
        project_key = org.get("jiraProjectKey")
        
        # Check if user is admin
        from api.routes.organizations import is_org_admin
        user_is_admin = is_org_admin(user_doc, org_id)
        user_id = str(user_doc["_id"])
        
        # Build JQL query - don't filter by issue type since JSM projects may use different types
        jql_parts = [f"project = {project_key}"]
        
        # Employees can only see tickets assigned to them (if we can map JSM account ID)
        # For now, show all tickets - assignment filtering can be added later
        # if not user_is_admin:
        #     jsm_account_id = get_jsm_account_id_from_user_id(user_id)
        #     if jsm_account_id:
        #         jql_parts.append(f"assignee = {jsm_account_id}")
        
        if status:
            # Map CRM status to JSM status
            status_map = {
                "open": "To Do",
                "in_progress": "In Progress",
                "resolved": "Done",
                "closed": "Closed"
            }
            jsm_status = status_map.get(status, status)
            jql_parts.append(f"status = '{jsm_status}'")
        
        if priority:
            # Map CRM priority to JSM priority
            priority_map = {
                "low": "Low",
                "medium": "Medium",
                "high": "High",
                "urgent": "Highest"
            }
            jsm_priority = priority_map.get(priority, priority)
            jql_parts.append(f"priority = '{jsm_priority}'")
        
        jql = " AND ".join(jql_parts) + " ORDER BY created DESC"
        
        # Fetch tickets from JSM
        jsm_tickets = get_jsm_service_requests(project_key, jql)
        
        # Apply pagination
        paginated_tickets = jsm_tickets[skip:skip + limit]
        
        # Get Jira Software issue links for all tickets
        from config.database import ticket_metadata_collection
        ticket_keys = [t["key"] for t in paginated_tickets]
        jira_integrations = list(jira_integration_collection.find({"jiraIssueKey": {"$in": ticket_keys}}))
        jira_map = {}
        for intg in jira_integrations:
            jsm_key = intg.get("jiraIssueKey")
            jira_key = intg.get("jiraIssueKey")  # This should be the Jira Software issue key
            if jsm_key and jira_key:
                jira_map[jsm_key] = {
                    "key": jira_key,
                    "url": f"{JIRA_SERVER}/browse/{jira_key}"
                }
        
        # Get customer and assignment metadata for all tickets
        ticket_metadata_list = list(ticket_metadata_collection.find({"jsmIssueKey": {"$in": ticket_keys}}))
        metadata_map = {}
        for meta in ticket_metadata_list:
            jsm_key = meta.get("jsmIssueKey")
            if jsm_key:
                metadata_map[jsm_key] = meta
        
        # Map JSM tickets to TicketResponse
        result = []
        for jsm_ticket in paginated_tickets:
            ticket_key = jsm_ticket["key"]
            
            # Get customer metadata and stored assignment from MongoDB (preferred),
            # or fallback to JSM reporter / JSM mapping when metadata is missing.
            metadata = metadata_map.get(ticket_key, {}) or {}
            customer_name = metadata.get("customerName") or jsm_ticket.get("reporterName", "")
            customer_email = metadata.get("customerEmail") or jsm_ticket.get("reporterEmail", "")
            customer_phone = metadata.get("customerPhone")
            customer_category = metadata.get("category")

            # Preferred source of assignment: what we stored in Mongo (set by update_ticket)
            assignee_user_id = metadata.get("assignedTo")
            assignee_name = metadata.get("assignedToName")

            # Fallback: derive from JSM if no stored assignment
            if not assignee_user_id and jsm_ticket.get("assigneeAccountId"):
                assignee_user_id = get_user_id_from_jsm_account_id(jsm_ticket.get("assigneeAccountId"), org_id)
                if assignee_user_id:
                    assignee_name = get_assigned_user_name(assignee_user_id)
            
            # Jira link: Use JSM ticket key as the primary link, or Jira Software issue if linked
            jira_software_key = jira_map.get(ticket_key, {}).get("key")
            jira_software_url = jira_map.get(ticket_key, {}).get("url")
            
            # Always set Jira link to JSM ticket (the ticket itself is in Jira)
            jira_issue_key = jira_software_key or ticket_key  # Use Jira Software key if linked, otherwise JSM key
            jira_issue_url = jira_software_url or f"{JIRA_SERVER}/browse/{ticket_key}"  # Always provide JSM ticket URL
            
            result.append(TicketResponse(
                id=ticket_key,  # Use JSM key as ID
                ticketNumber=ticket_key,  # JSM generates keys like SR-123
                orgId=org_id,
                name=customer_name,
                email=customer_email,
                phone=customer_phone,
                subject=jsm_ticket.get("summary", ""),
                description=jsm_ticket.get("description", ""),
                priority=jsm_ticket.get("priority", "medium"),
                category=customer_category,
                status=jsm_ticket.get("status", "open"),
                assignedTo=assignee_user_id,
                assignedToName=assignee_name,
                jiraIssueKey=jira_issue_key,
                jiraIssueUrl=jira_issue_url,
                createdAt=jsm_ticket.get("created", ""),
                updatedAt=jsm_ticket.get("updated", "")
            ))
        
        return result
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching tickets: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch tickets: {str(e)}")

@router.get("/check-admin", response_model=dict)
async def check_admin(
    current_user: dict = Depends(get_current_user)
):
    """Check if current user is admin of their organization"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        from api.routes.organizations import is_org_admin
        # Treat either org admins or users with 'admin:tickets' as ticket administrators
        user_is_admin = is_org_admin(user_doc, org_id) or has_ticket_admin_permission(user_doc, org_id)
        
        return {"isAdmin": user_is_admin}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error checking admin status: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to check admin status: {str(e)}")

@router.get("/assignable-employees", response_model=list[AssignableEmployeeResponse])
async def get_assignable_employees(
    current_user: dict = Depends(get_current_user)
):
    """Get list of employees with ticket roles who can be assigned to tickets"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Get organization ID
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has ticket role
        if not has_ticket_role(user_doc, org_id):
            raise HTTPException(
                status_code=403, 
                detail="You do not have permission to view assignable employees."
            )
        
        # Get all users in the organization (optimized query)
        # Query users that belong to this organization
        all_users = list(users_collection.find({
            "$or": [
                {"orgId": org_id},
                {"orgId": {"$elemMatch": {"$eq": org_id}}}
            ]
        }))
        
        # Filter employees with ticket roles
        assignable_employees = []
        for emp in all_users:
            emp_org_ids = emp.get("orgId", [])
            if isinstance(emp_org_ids, str):
                emp_org_ids = [emp_org_ids]
            elif emp_org_ids is None:
                continue
            
            # Verify user belongs to this org and has ticket role
            if org_id in emp_org_ids and has_ticket_role(emp, org_id):
                assignable_employees.append(AssignableEmployeeResponse(
                    id=str(emp["_id"]),
                    name=emp.get("name", ""),
                    email=emp.get("email", ""),
                    picture=emp.get("picture")
                ))
        
        return assignable_employees
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching assignable employees: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch assignable employees: {str(e)}")

@router.get("/{ticket_id}", response_model=TicketResponse)
async def get_ticket(
    ticket_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Get a single ticket by ID (JSM issue key) - requires ticket role"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Get organization ID
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has ticket role
        if not has_ticket_role(user_doc, org_id):
            raise HTTPException(
                status_code=403, 
                detail="You do not have permission to view tickets."
            )
        
        # Get organization JSM project
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org or not org.get("jiraProjectKey"):
            raise HTTPException(status_code=404, detail="Organization JSM project not configured")
        
        project_key = org.get("jiraProjectKey")
        
        # Check if user is admin
        from api.routes.organizations import is_org_admin
        user_is_admin = is_org_admin(user_doc, org_id)
        user_id = str(user_doc["_id"])
        
        # Fetch ticket from JSM (ticket_id is now a JSM issue key like "SR-123")
        jsm_ticket = get_jsm_service_request(ticket_id)
        
        if not jsm_ticket:
            raise HTTPException(status_code=404, detail="Ticket not found")
        
        # Verify ticket belongs to this organization's project
        if not ticket_id.startswith(project_key):
            raise HTTPException(status_code=404, detail="Ticket not found")
        
        # Employees can only view tickets assigned to them (if we can map)
        # For now, allow all users with ticket role to view
        # if not user_is_admin:
        #     assignee_user_id = get_user_id_from_jsm_account_id(jsm_ticket.get("assigneeAccountId"), org_id)
        #     if assignee_user_id != user_id:
        #         raise HTTPException(
        #             status_code=403,
        #             detail="You can only view tickets assigned to you."
        #         )
        
        # Get customer metadata and stored assignment from MongoDB (preferred)
        from config.settings import JIRA_SERVER
        from config.database import ticket_metadata_collection
        metadata = ticket_metadata_collection.find_one({"jsmIssueKey": ticket_id})
        customer_name = metadata.get("customerName") if metadata else None
        customer_email = metadata.get("customerEmail") if metadata else None
        customer_phone = metadata.get("customerPhone") if metadata else None
        customer_category = metadata.get("category") if metadata else None

        # Preferred assignment source: what we stored in Mongo (set by update_ticket)
        assignee_user_id = metadata.get("assignedTo") if metadata else None
        assigned_to_name = metadata.get("assignedToName") if metadata else None

        # Fallback: try to derive assignment from JSM if nothing is stored
        if not assignee_user_id and jsm_ticket.get("assigneeAccountId"):
            assignee_user_id = get_user_id_from_jsm_account_id(jsm_ticket.get("assigneeAccountId"), org_id)
            if assignee_user_id:
                assigned_to_name = get_assigned_user_name(assignee_user_id)
        
        # Use customer metadata if available, otherwise fallback to JSM reporter
        final_name = customer_name or jsm_ticket.get("reporterName", "")
        final_email = customer_email or jsm_ticket.get("reporterEmail", "")
        
        # Get Jira Software issue info if linked
        jira_software_key = None
        jira_software_url = None
        jira_integration = jira_integration_collection.find_one({"jiraIssueKey": ticket_id})
        if jira_integration:
            # This integration links JSM ticket to Jira Software issue
            # The jiraIssueKey in integration is the Jira Software issue key
            jira_software_key = jira_integration.get("jiraIssueKey")
            if jira_software_key and jira_software_key != ticket_id:
                jira_software_url = f"{JIRA_SERVER}/browse/{jira_software_key}"
        
        # Always set Jira link to JSM ticket (the ticket itself is in Jira)
        jira_issue_key = jira_software_key or ticket_id  # Use Jira Software key if linked, otherwise JSM key
        jira_issue_url = jira_software_url or f"{JIRA_SERVER}/browse/{ticket_id}"  # Always provide JSM ticket URL
        
        return TicketResponse(
            id=jsm_ticket["key"],
            ticketNumber=jsm_ticket["key"],
            orgId=org_id,
            name=final_name,
            email=final_email,
            phone=customer_phone,
            subject=jsm_ticket.get("summary", ""),
            description=jsm_ticket.get("description", ""),
            priority=jsm_ticket.get("priority", "medium"),
            category=customer_category,
            status=jsm_ticket.get("status", "open"),
            assignedTo=assignee_user_id,
            assignedToName=assigned_to_name,
            jiraIssueKey=jira_issue_key,
            jiraIssueUrl=jira_issue_url,
            createdAt=jsm_ticket.get("created", ""),
            updatedAt=jsm_ticket.get("updated", "")
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching ticket: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch ticket: {str(e)}")

@router.put("/{ticket_id}", response_model=TicketResponse)
async def update_ticket(
    ticket_id: str,
    request: UpdateTicketRequest,
    current_user: dict = Depends(get_current_user)
):
    """Update a ticket (status, priority, assignment) - requires ticket write role"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Get organization ID
        user_ids = get_user_ids(user_doc)
        org_id = user_ids["orgId"]
        
        # Check if user has ticket write role
        if not has_ticket_role(user_doc, org_id):
            raise HTTPException(
                status_code=403, 
                detail="You do not have permission to update tickets."
            )
        
        # Get organization JSM project
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org or not org.get("jiraProjectKey"):
            raise HTTPException(status_code=404, detail="Organization JSM project not configured")
        
        project_key = org.get("jiraProjectKey")
        
        # Check if user is ticket admin (only admins can assign tickets)
        # Ticket admins are either org admins or users with 'admin:tickets' permission.
        from api.routes.organizations import is_org_admin
        user_is_admin = is_org_admin(user_doc, org_id) or has_ticket_admin_permission(user_doc, org_id)
        user_id = str(user_doc["_id"])
        
        # Fetch ticket from JSM
        jsm_ticket = get_jsm_service_request(ticket_id)
        
        if not jsm_ticket:
            raise HTTPException(status_code=404, detail="Ticket not found")
        
        # Verify ticket belongs to this organization's project
        if not ticket_id.startswith(project_key):
            raise HTTPException(status_code=404, detail="Ticket not found")
        
        # Employees can only update tickets assigned to them (unless they're admin)
        # For now, allow all users with ticket role to update
        # if not user_is_admin:
        #     assignee_user_id = get_user_id_from_jsm_account_id(jsm_ticket.get("assigneeAccountId"), org_id)
        #     if assignee_user_id != user_id:
        #         raise HTTPException(
        #             status_code=403,
        #             detail="You can only update tickets assigned to you."
        #         )
        
        # Only ticket admins or users with write:tickets permission can assign tickets.
        # This allows agents with write:tickets to assign while still respecting ticket admins.
        from utils.permissions import has_permission
        can_assign = user_is_admin or has_permission(user_doc, org_id, ["write:tickets", "admin:tickets"])

        if request.assignedTo is not None and not can_assign:
            raise HTTPException(
                status_code=403,
                detail="Only administrators can assign or reassign tickets."
            )
        
        # Validate assignedTo if provided (admin only) and prepare Jira assignee accountId
        assignee_account_id = None
        employee = None
        if request.assignedTo and user_is_admin:
            # Verify the employee exists
            employee = users_collection.find_one({"_id": ObjectId(request.assignedTo)})
            
            if not employee:
                raise HTTPException(status_code=404, detail="Employee not found")
            
            # Check if employee belongs to this organization
            employee_org_ids = employee.get("orgId", [])
            if isinstance(employee_org_ids, str):
                employee_org_ids = [employee_org_ids]
            elif employee_org_ids is None:
                employee_org_ids = []
            
            if org_id not in employee_org_ids:
                raise HTTPException(status_code=403, detail="Employee does not belong to this organization")
            
            # Verify employee has ticket role
            if not has_ticket_role(employee, org_id):
                raise HTTPException(
                    status_code=400,
                    detail="This employee does not have ticket permissions. Please assign them a role with ticket permissions (read:tickets or write:tickets) first."
                )
            
            # Get JSM account ID for the employee (best-effort for Jira)
            assignee_account_id = get_jsm_account_id_from_user_id(request.assignedTo)
        
        # Update ticket in JSM (status/priority/assignee in Jira) - best-effort
        updated_jsm_ticket = update_jsm_service_request(
            issue_key=ticket_id,
            status=request.status,
            priority=request.priority,
            assignee_account_id=assignee_account_id
        )
        
        if not updated_jsm_ticket:
            raise HTTPException(status_code=500, detail="Failed to update ticket in JSM")
        
        # Get customer metadata and existing assignment from MongoDB (preferred)
        from config.settings import JIRA_SERVER
        from config.database import ticket_metadata_collection
        metadata = ticket_metadata_collection.find_one({"jsmIssueKey": ticket_id}) or {}
        customer_name = metadata.get("customerName") if metadata else None
        customer_email = metadata.get("customerEmail") if metadata else None
        customer_phone = metadata.get("customerPhone") if metadata else None
        customer_category = metadata.get("category") if metadata else None

        # Determine CRM-side assignment based on the incoming request:
        # - If assignedTo is present (even if null), that becomes the new CRM assignment.
        # - Otherwise, keep existing metadata assignment.
        assignee_user_id = None
        assigned_to_name = None

        assignment_field_sent = "assignedTo" in request.__fields_set__

        if assignment_field_sent:
            if request.assignedTo:
                assignee_user_id = request.assignedTo
                # Prefer using the employee document we already fetched
                if employee:
                    assigned_to_name = employee.get("name") or get_assigned_user_name(assignee_user_id)
                else:
                    assigned_to_name = get_assigned_user_name(assignee_user_id)
            else:
                # Explicit unassign
                assignee_user_id = None
                assigned_to_name = None

            from datetime import datetime
            now = datetime.utcnow()
            ticket_metadata_collection.update_one(
                {"jsmIssueKey": ticket_id},
                {
                    "$set": {
                        "jsmIssueKey": ticket_id,
                        "orgId": org_id,
                        "assignedTo": assignee_user_id,
                        "assignedToName": assigned_to_name,
                        "updatedAt": now
                    }
                },
                upsert=True
            )
        else:
            # No explicit CRM assignment change; keep any existing metadata assignment
            assignee_user_id = metadata.get("assignedTo")
            assigned_to_name = metadata.get("assignedToName")
        
        # Use customer metadata if available, otherwise fallback to JSM reporter
        final_name = customer_name or updated_jsm_ticket.get("reporterName", "")
        final_email = customer_email or updated_jsm_ticket.get("reporterEmail", "")
        
        # Get Jira Software issue info if linked
        jira_software_key = None
        jira_software_url = None
        jira_integration = jira_integration_collection.find_one({"jiraIssueKey": ticket_id})
        if jira_integration:
            jira_software_key = jira_integration.get("jiraIssueKey")
            if jira_software_key and jira_software_key != ticket_id:
                jira_software_url = f"{JIRA_SERVER}/browse/{jira_software_key}"
        
        # Always set Jira link to JSM ticket (the ticket itself is in Jira)
        jira_issue_key = jira_software_key or ticket_id  # Use Jira Software key if linked, otherwise JSM key
        jira_issue_url = jira_software_url or f"{JIRA_SERVER}/browse/{ticket_id}"  # Always provide JSM ticket URL
        
        return TicketResponse(
            id=updated_jsm_ticket["key"],
            ticketNumber=updated_jsm_ticket["key"],
            orgId=org_id,
            name=final_name,
            email=final_email,
            phone=customer_phone,
            subject=updated_jsm_ticket.get("summary", ""),
            description=updated_jsm_ticket.get("description", ""),
            priority=updated_jsm_ticket.get("priority", "medium"),
            category=customer_category,
            status=updated_jsm_ticket.get("status", "open"),
            assignedTo=assignee_user_id,
            assignedToName=assigned_to_name,
            jiraIssueKey=jira_issue_key,
            jiraIssueUrl=jira_issue_url,
            createdAt=updated_jsm_ticket.get("created", ""),
            updatedAt=updated_jsm_ticket.get("updated", "")
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating ticket: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to update ticket: {str(e)}")

@router.post("", response_model=TicketResponse)
async def create_ticket(request: CreateTicketRequest):
    """
    Create a new support ticket (PUBLIC ENDPOINT - No authentication required)
    Customers can submit tickets via this endpoint
    All tickets are created in JSM. Bug reports and feature requests are also linked to Jira Software.
    """
    try:
        # Validate that organization exists
        org = organizations_collection.find_one({"_id": ObjectId(request.orgId)})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found")
        
        # Check if organization has JSM project configured
        if not org.get("jiraProjectKey"):
            raise HTTPException(status_code=400, detail="Organization does not have JSM project configured. Please contact administrator.")
        
        project_key = org.get("jiraProjectKey")
        
        # Create JSM Service Request (always)
        summary = request.subject
        description = f"""Customer: {request.name} ({request.email})
Phone: {request.phone or 'N/A'}
Priority: {request.priority or 'medium'}
Category: {request.category or 'N/A'}

Description:
{request.description}
"""
        
        jsm_ticket = create_jsm_service_request(
            project_key=project_key,
            summary=summary,
            description=description,
            reporter_email=request.email,
            reporter_name=request.name,
            priority=request.priority or "medium"
        )
        
        if not jsm_ticket:
            raise HTTPException(status_code=500, detail="Failed to create ticket in JSM")
        
        jsm_issue_key = jsm_ticket["issueKey"]
        now = datetime.utcnow()
        
        # Store customer metadata in MongoDB (linked to JSM issue key)
        from config.database import ticket_metadata_collection
        ticket_metadata_collection.insert_one({
            "jsmIssueKey": jsm_issue_key,
            "orgId": request.orgId,
            "customerName": request.name,
            "customerEmail": request.email,
            "customerPhone": request.phone,
            "category": request.category,
            "createdAt": now,
            "updatedAt": now
        })
        
        # If category is bug_report or feature_request, also create Jira Software issue
        jira_software_issue_key = None
        jira_software_issue_url = None
        
        if request.category in ["bug_report", "feature_request"]:
            # Check if organization has Jira Software project
            jira_software_project_key = org.get("jiraSoftwareProjectKey")
            
            if jira_software_project_key:
                try:
                    issue_type = "Bug" if request.category == "bug_report" else "Story"
                    jira_summary = f"[{jsm_issue_key}] {request.subject}"
                    jira_description = f"""JSM Service Request: {jsm_issue_key}
Customer: {request.name} ({request.email})
Priority: {request.priority or 'medium'}

Description:
{request.description}
"""
                    
                    jira_issue_info = create_jira_software_issue(
                        project_key=jira_software_project_key,
                        summary=jira_summary,
                        description=jira_description,
                        issue_type=issue_type
                    )
                    
                    if jira_issue_info:
                        jira_software_issue_key = jira_issue_info["issueKey"]
                        jira_software_issue_url = jira_issue_info["issueUrl"]
                        
                        # Link JSM ticket to Jira Software issue
                        link_success = link_jsm_to_jira_software(jsm_issue_key, jira_software_issue_key)
                        
                        if link_success:
                            # Store integration record
                            jira_integration_collection.insert_one({
                                "orgId": request.orgId,
                                "ticketId": jsm_issue_key,  # JSM issue key
                                "jiraIssueKey": jira_software_issue_key,  # Jira Software issue key
                                "jiraIssueId": jira_issue_info["issueId"],
                                "jiraProjectKey": jira_software_project_key,
                                "syncDirection": "jsm_to_jira_software",
                                "status": "active",
                                "lastSyncedAt": datetime.utcnow(),
                                "createdAt": datetime.utcnow(),
                                "updatedAt": datetime.utcnow()
                            })
                except Exception as e:
                    print(f"Failed to create Jira Software issue for ticket {jsm_issue_key}: {str(e)}")
                    import traceback
                    traceback.print_exc()
                    # Don't fail ticket creation if Jira Software creation fails
        
        # Get the created JSM ticket details
        created_jsm_ticket = get_jsm_service_request(jsm_issue_key)
        if not created_jsm_ticket:
            raise HTTPException(status_code=500, detail="Failed to retrieve created ticket")
        
        # Get assigned user info (if any)
        assignee_user_id = get_user_id_from_jsm_account_id(created_jsm_ticket.get("assigneeAccountId"), request.orgId)
        assigned_to_name = None
        if assignee_user_id:
            assigned_to_name = get_assigned_user_name(assignee_user_id)
        
        # Always set Jira link to JSM ticket (the ticket itself is in Jira)
        jira_issue_key = jira_software_issue_key or jsm_issue_key  # Use Jira Software key if linked, otherwise JSM key
        jira_issue_url = jira_software_issue_url or f"{JIRA_SERVER}/browse/{jsm_issue_key}"  # Always provide JSM ticket URL
        
        return TicketResponse(
            id=created_jsm_ticket["key"],
            ticketNumber=created_jsm_ticket["key"],  # JSM generates keys like SR-123
            orgId=request.orgId,
            name=request.name,  # Use customer name from request (stored in metadata)
            email=request.email,  # Use customer email from request (stored in metadata)
            phone=request.phone,  # Use customer phone from request (stored in metadata)
            subject=created_jsm_ticket.get("summary", request.subject),
            description=created_jsm_ticket.get("description", request.description),
            priority=created_jsm_ticket.get("priority", request.priority or "medium"),
            category=request.category,
            status=created_jsm_ticket.get("status", "open"),
            assignedTo=assignee_user_id,
            assignedToName=assigned_to_name,
            jiraIssueKey=jira_issue_key,
            jiraIssueUrl=jira_issue_url,
            createdAt=created_jsm_ticket.get("created", now.isoformat()),
            updatedAt=created_jsm_ticket.get("updated", now.isoformat())
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating ticket: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create ticket: {str(e)}")
