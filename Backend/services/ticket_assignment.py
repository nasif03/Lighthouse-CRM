"""Ticket auto-assignment service"""
from typing import Optional
from bson import ObjectId
from config.database import users_collection, tickets_collection


def get_assignable_employees(org_id: str) -> list[dict]:
    """Get all employees with ticket roles who can be assigned tickets"""
    # Import here to avoid circular dependency
    from api.routes.tickets import has_ticket_role
    from config.database import organizations_collection
    
    # Get organization to check admins
    org = organizations_collection.find_one({"_id": ObjectId(org_id)})
    admin_ids = org.get("admins", []) if org else []
    
    # Get all users - we'll filter by orgId and ticket role
    all_users = list(users_collection.find({}))
    
    # Filter employees with ticket roles in this organization
    assignable = []
    for user in all_users:
        user_id = str(user["_id"])
        user_org_ids = user.get("orgId", [])
        
        # Normalize orgIds
        if isinstance(user_org_ids, str):
            user_org_ids = [user_org_ids]
        elif user_org_ids is None:
            user_org_ids = []
        
        # Check if user belongs to this org or is an admin
        belongs_to_org = org_id in user_org_ids
        is_admin = user_id in admin_ids
        
        if (belongs_to_org or is_admin) and has_ticket_role(user, org_id):
            assignable.append(user)
    
    return assignable


def auto_assign_ticket(org_id: str, ticket_id: Optional[str] = None) -> Optional[str]:
    """
    Auto-assign a ticket to an employee using round-robin algorithm.
    
    Args:
        org_id: Organization ID
        ticket_id: Optional ticket ID (for logging)
    
    Returns:
        User ID of assigned employee, or None if no assignable employees
    """
    assignable_employees = get_assignable_employees(org_id)
    
    if not assignable_employees:
        return None
    
    # Get current ticket counts for each employee (for load balancing)
    employee_ticket_counts = {}
    for emp in assignable_employees:
        emp_id = str(emp["_id"])
        # Count open/in_progress tickets assigned to this employee
        count = tickets_collection.count_documents({
            "orgId": org_id,
            "assignedTo": emp_id,
            "status": {"$in": ["open", "in_progress"]}
        })
        employee_ticket_counts[emp_id] = count
    
    # Sort by ticket count (least loaded first)
    sorted_employees = sorted(
        assignable_employees,
        key=lambda emp: employee_ticket_counts.get(str(emp["_id"]), 0)
    )
    
    # Assign to employee with least tickets (round-robin style)
    if sorted_employees:
        assigned_employee_id = str(sorted_employees[0]["_id"])
        return assigned_employee_id
    
    return None

