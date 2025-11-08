"""Support tickets API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from typing import Optional
from models.ticket import CreateTicketRequest, TicketResponse, UpdateTicketRequest, AssignableEmployeeResponse
from api.dependencies import get_current_user
from config.database import tickets_collection, organizations_collection, users_collection, roles_collection
from utils.query_filters import get_user_ids, build_user_filter

router = APIRouter(prefix="/api/tickets", tags=["tickets"])

def generate_ticket_number(org_id: str) -> str:
    """Generate a unique ticket number in format: TKT-YYYYMMDD-XXXX"""
    from datetime import datetime
    date_prefix = datetime.utcnow().strftime("%Y%m%d")
    
    # Find the highest ticket number for today for this org
    today_prefix = f"TKT-{date_prefix}-"
    today_tickets = tickets_collection.find({
        "orgId": org_id,
        "ticketNumber": {"$regex": f"^{today_prefix}"}
    }).sort("ticketNumber", -1).limit(1)
    
    last_ticket = list(today_tickets)
    if last_ticket and last_ticket[0].get("ticketNumber"):
        # Extract the number part and increment
        last_num = last_ticket[0]["ticketNumber"].split("-")[-1]
        try:
            next_num = int(last_num) + 1
        except ValueError:
            next_num = 1
    else:
        next_num = 1
    
    return f"{today_prefix}{next_num:04d}"

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
        
        # Check if user is admin
        from api.routes.organizations import is_org_admin
        user_is_admin = is_org_admin(user_doc, org_id)
        user_id = str(user_doc["_id"])
        
        # Build query filter
        query = {"orgId": org_id}
        
        # Employees can only see tickets assigned to them
        # Admins can see all tickets
        if not user_is_admin:
            query["assignedTo"] = user_id
        
        if status:
            query["status"] = status
        if priority:
            query["priority"] = priority
        if assignedTo:
            query["assignedTo"] = assignedTo
        
        # Fetch tickets
        tickets = list(tickets_collection.find(query).sort("createdAt", -1).skip(skip).limit(limit))
        
        # Get assigned user names
        assigned_user_ids = [t.get("assignedTo") for t in tickets if t.get("assignedTo")]
        assigned_users = {}
        if assigned_user_ids:
            users = list(users_collection.find({
                "_id": {"$in": [ObjectId(uid) for uid in assigned_user_ids if ObjectId.is_valid(uid)]}
            }))
            assigned_users = {str(u["_id"]): u.get("name", "Unknown") for u in users}
        
        # Get Jira issue info for all tickets
        from config.database import jira_integration_collection
        from config.settings import JIRA_SERVER
        ticket_ids = [str(ticket["_id"]) for ticket in tickets]
        jira_integrations = list(jira_integration_collection.find({"ticketId": {"$in": ticket_ids}}))
        jira_map = {}
        for intg in jira_integrations:
            ticket_id = intg.get("ticketId")
            issue_key = intg.get("jiraIssueKey")
            if ticket_id and issue_key:
                jira_map[ticket_id] = {
                    "key": issue_key,
                    "url": f"{JIRA_SERVER}/browse/{issue_key}"
                }
        
        return [
            TicketResponse(
                id=str(ticket["_id"]),
                ticketNumber=ticket.get("ticketNumber", ""),
                orgId=ticket.get("orgId", ""),
                name=ticket.get("name", ""),
                email=ticket.get("email", ""),
                phone=ticket.get("phone"),
                subject=ticket.get("subject", ""),
                description=ticket.get("description", ""),
                priority=ticket.get("priority", "medium"),
                category=ticket.get("category"),
                status=ticket.get("status", "open"),
                assignedTo=ticket.get("assignedTo"),
                assignedToName=assigned_users.get(ticket.get("assignedTo")) if ticket.get("assignedTo") else None,
                jiraIssueKey=jira_map.get(str(ticket["_id"]), {}).get("key"),
                jiraIssueUrl=jira_map.get(str(ticket["_id"]), {}).get("url"),
                createdAt=ticket.get("createdAt").isoformat() if ticket.get("createdAt") else "",
                updatedAt=ticket.get("updatedAt").isoformat() if ticket.get("updatedAt") else ""
            )
            for ticket in tickets
        ]
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
        user_is_admin = is_org_admin(user_doc, org_id)
        
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
    """Get a single ticket by ID - requires ticket role"""
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
        
        # Validate ticket_id is a valid ObjectId
        if not ObjectId.is_valid(ticket_id):
            raise HTTPException(status_code=400, detail="Invalid ticket ID format")
        
        # Check if user is admin
        from api.routes.organizations import is_org_admin
        user_is_admin = is_org_admin(user_doc, org_id)
        user_id = str(user_doc["_id"])
        
        # Fetch ticket
        ticket = tickets_collection.find_one({
            "_id": ObjectId(ticket_id),
            "orgId": org_id
        })
        
        if not ticket:
            raise HTTPException(status_code=404, detail="Ticket not found")
        
        # Employees can only view tickets assigned to them
        if not user_is_admin:
            if ticket.get("assignedTo") != user_id:
                raise HTTPException(
                    status_code=403,
                    detail="You can only view tickets assigned to you."
                )
        
        # Get assigned user name if assigned
        assigned_to_name = None
        if ticket.get("assignedTo"):
            assigned_user = users_collection.find_one({"_id": ObjectId(ticket["assignedTo"])})
            if assigned_user:
                assigned_to_name = assigned_user.get("name", "Unknown")
        
        # Get Jira issue info if linked
        from config.database import jira_integration_collection
        jira_issue_key = None
        jira_issue_url = None
        jira_integration = jira_integration_collection.find_one({"ticketId": ticket_id})
        if jira_integration:
            jira_issue_key = jira_integration.get("jiraIssueKey")
            if jira_issue_key:
                from config.settings import JIRA_SERVER
                jira_issue_url = f"{JIRA_SERVER}/browse/{jira_issue_key}"
        
        return TicketResponse(
            id=str(ticket["_id"]),
            ticketNumber=ticket.get("ticketNumber", ""),
            orgId=ticket.get("orgId", ""),
            name=ticket.get("name", ""),
            email=ticket.get("email", ""),
            phone=ticket.get("phone"),
            subject=ticket.get("subject", ""),
            description=ticket.get("description", ""),
            priority=ticket.get("priority", "medium"),
            category=ticket.get("category"),
            status=ticket.get("status", "open"),
            assignedTo=ticket.get("assignedTo"),
            assignedToName=assigned_to_name,
            jiraIssueKey=jira_issue_key,
            jiraIssueUrl=jira_issue_url,
            createdAt=ticket.get("createdAt").isoformat() if ticket.get("createdAt") else "",
            updatedAt=ticket.get("updatedAt").isoformat() if ticket.get("updatedAt") else ""
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
        
        # Check if user is admin (only admins can assign tickets)
        from api.routes.organizations import is_org_admin
        user_is_admin = is_org_admin(user_doc, org_id)
        user_id = str(user_doc["_id"])
        
        # Fetch ticket
        ticket = tickets_collection.find_one({
            "_id": ObjectId(ticket_id),
            "orgId": org_id
        })
        
        if not ticket:
            raise HTTPException(status_code=404, detail="Ticket not found")
        
        # Employees can only update tickets assigned to them (unless they're admin)
        if not user_is_admin:
            if ticket.get("assignedTo") != user_id:
                raise HTTPException(
                    status_code=403,
                    detail="You can only update tickets assigned to you."
                )
            # Employees cannot change assignment
            if request.assignedTo is not None and request.assignedTo != ticket.get("assignedTo"):
                raise HTTPException(
                    status_code=403,
                    detail="Only administrators can assign or reassign tickets."
                )
        
        # Only admins can assign tickets
        if request.assignedTo is not None and not user_is_admin:
            raise HTTPException(
                status_code=403,
                detail="Only administrators can assign or reassign tickets."
            )
        
        # Validate assignedTo if provided (admin only)
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
        
        # Build update data
        update_data = {"updatedAt": datetime.utcnow()}
        
        if request.status:
            update_data["status"] = request.status
        if request.priority:
            update_data["priority"] = request.priority
        if request.assignedTo is not None:
            update_data["assignedTo"] = request.assignedTo if request.assignedTo else None
        if request.category is not None:
            update_data["category"] = request.category
        
        # Update ticket
        tickets_collection.update_one(
            {"_id": ObjectId(ticket_id)},
            {"$set": update_data}
        )
        
        # Fetch updated ticket
        updated_ticket = tickets_collection.find_one({"_id": ObjectId(ticket_id)})
        
        # Get assigned user name if assigned
        assigned_to_name = None
        if updated_ticket.get("assignedTo"):
            assigned_user = users_collection.find_one({"_id": ObjectId(updated_ticket["assignedTo"])})
            if assigned_user:
                assigned_to_name = assigned_user.get("name", "Unknown")
        
        # Get Jira issue info if linked
        from config.database import jira_integration_collection
        jira_issue_key = None
        jira_issue_url = None
        jira_integration = jira_integration_collection.find_one({"ticketId": ticket_id})
        if jira_integration:
            jira_issue_key = jira_integration.get("jiraIssueKey")
            if jira_issue_key:
                from config.settings import JIRA_SERVER
                jira_issue_url = f"{JIRA_SERVER}/browse/{jira_issue_key}"
        
        return TicketResponse(
            id=str(updated_ticket["_id"]),
            ticketNumber=updated_ticket.get("ticketNumber", ""),
            orgId=updated_ticket.get("orgId", ""),
            name=updated_ticket.get("name", ""),
            email=updated_ticket.get("email", ""),
            phone=updated_ticket.get("phone"),
            subject=updated_ticket.get("subject", ""),
            description=updated_ticket.get("description", ""),
            priority=updated_ticket.get("priority", "medium"),
            category=updated_ticket.get("category"),
            status=updated_ticket.get("status", "open"),
            assignedTo=updated_ticket.get("assignedTo"),
            assignedToName=assigned_to_name,
            jiraIssueKey=jira_issue_key,
            jiraIssueUrl=jira_issue_url,
            createdAt=updated_ticket.get("createdAt").isoformat() if updated_ticket.get("createdAt") else "",
            updatedAt=updated_ticket.get("updatedAt").isoformat() if updated_ticket.get("updatedAt") else ""
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
    """
    try:
        # Validate that organization exists
        org = organizations_collection.find_one({"_id": ObjectId(request.orgId)})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found")
        
        now = datetime.utcnow()
        ticket_number = generate_ticket_number(request.orgId)
        
        ticket_data = {
            "ticketNumber": ticket_number,
            "orgId": request.orgId,
            "name": request.name,
            "email": request.email,
            "phone": request.phone or "",
            "subject": request.subject,
            "description": request.description,
            "priority": request.priority or "medium",
            "category": request.category or "",
            "status": "open",
            "assignedTo": None,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = tickets_collection.insert_one(ticket_data)
        ticket_id = str(result.inserted_id)
        
        # Auto-assign ticket to an employee with ticket role
        assigned_employee_id = None
        try:
            from services.ticket_assignment import auto_assign_ticket
            assigned_employee_id = auto_assign_ticket(request.orgId, ticket_id)
            if assigned_employee_id:
                tickets_collection.update_one(
                    {"_id": ObjectId(ticket_id)},
                    {"$set": {"assignedTo": assigned_employee_id, "updatedAt": datetime.utcnow()}}
                )
                ticket_data["assignedTo"] = assigned_employee_id
        except Exception as e:
            print(f"Failed to auto-assign ticket {ticket_id}: {str(e)}")
            # Don't fail ticket creation if auto-assignment fails
        
        # Auto-create Jira issue if organization has Jira project
        jira_issue_key = None
        jira_issue_url = None
        try:
            if org.get("jiraProjectKey"):
                from services.jira_service import create_jira_issue
                from config.database import jira_integration_collection
                
                project_key = org.get("jiraProjectKey")
                issue_type = "Task"
                if request.category == "bug_report":
                    issue_type = "Bug"
                elif request.category == "feature_request":
                    issue_type = "Story"
                
                summary = f"[{ticket_number}] {request.subject}"
                description = f"""Ticket Number: {ticket_number}
Customer: {request.name} ({request.email})
Priority: {request.priority or 'medium'}
Category: {request.category or 'N/A'}

Description:
{request.description}
"""
                
                issue_info = create_jira_issue(project_key, summary, description, issue_type)
                if issue_info:
                    jira_issue_key = issue_info["issueKey"]
                    jira_issue_url = issue_info["issueUrl"]
                    jira_integration_collection.insert_one({
                        "orgId": request.orgId,
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
        except Exception as e:
            print(f"Failed to create Jira issue for ticket {ticket_id}: {str(e)}")
            import traceback
            traceback.print_exc()
            # Don't fail ticket creation if Jira creation fails
        
        # Get assigned user name
        assigned_to_name = get_assigned_user_name(assigned_employee_id) if assigned_employee_id else None
        
        return TicketResponse(
            id=ticket_id,
            ticketNumber=ticket_number,
            orgId=request.orgId,
            name=request.name,
            email=request.email,
            phone=request.phone,
            subject=request.subject,
            description=request.description,
            priority=request.priority or "medium",
            category=request.category,
            status="open",
            assignedTo=assigned_employee_id,
            assignedToName=assigned_to_name,
            jiraIssueKey=jira_issue_key,
            jiraIssueUrl=jira_issue_url,
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating ticket: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create ticket: {str(e)}")
