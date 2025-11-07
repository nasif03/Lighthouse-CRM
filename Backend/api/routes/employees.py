"""Employee/User management API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from models.employee import EmployeeResponse, CreateEmployeeRequest, UpdateEmployeeRequest
from api.dependencies import get_current_user
from config.database import users_collection, organizations_collection, roles_collection

router = APIRouter(prefix="/api/organizations/{org_id}/employees", tags=["employees"])

def is_org_admin(user_doc: dict, org_id: str) -> bool:
    """Check if user is admin of the organization"""
    user_id = str(user_doc["_id"])
    org = organizations_collection.find_one({"_id": ObjectId(org_id)})
    if not org:
        return False
    admins = org.get("admins", [])
    return user_id in admins

@router.get("", response_model=list[EmployeeResponse])
async def get_employees(
    org_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Get all employees (users) in an organization - only admins can view"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can view employees")
        
        # Get all users in this organization
        # Users can belong to multiple orgs, so check if org_id is in their orgId array
        # Handle both string and array formats for orgId
        # Use $in operator which works for both string (exact match) and array (contains)
        employees_cursor = users_collection.find({
            "$or": [
                {"orgId": org_id},  # Exact match for string
                {"orgId": {"$in": [org_id]}}  # Contains for array (MongoDB handles this)
            ]
        })
        
        # Filter to ensure we only get users that actually have this org_id
        # (needed because MongoDB query might match incorrectly when orgId is array)
        employees = []
        for emp in employees_cursor:
            emp_org_ids = emp.get("orgId", [])
            if isinstance(emp_org_ids, str):
                emp_org_ids = [emp_org_ids]
            elif emp_org_ids is None:
                continue
            
            if org_id in emp_org_ids:
                employees.append(emp)
        
        return [
            EmployeeResponse(
                id=str(emp["_id"]),
                name=emp.get("name", ""),
                email=emp.get("email", ""),
                picture=emp.get("picture"),
                roleIds=emp.get("roleIds", []),
                isAdmin=str(emp["_id"]) in (organizations_collection.find_one({"_id": ObjectId(org_id)}) or {}).get("admins", []),
                createdAt=emp.get("createdAt").isoformat() if emp.get("createdAt") else ""
            )
            for emp in employees
        ]
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching employees: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch employees: {str(e)}")

@router.post("", response_model=EmployeeResponse)
async def create_employee(
    org_id: str,
    request: CreateEmployeeRequest,
    current_user: dict = Depends(get_current_user)
):
    """Add an employee (user) to an organization - only admins can add employees"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can add employees")
        
        # Check if organization exists
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found")
        
        now = datetime.utcnow()
        
        # Check if user already exists
        existing_user = users_collection.find_one({"email": request.email})
        
        if existing_user:
            # User exists, add to organization
            user_id = str(existing_user["_id"])
            user_org_ids = existing_user.get("orgId", [])
            
            # Convert to list if string
            if isinstance(user_org_ids, str):
                user_org_ids = [user_org_ids]
            
            # Add org if not already in list
            if org_id not in user_org_ids:
                user_org_ids.append(org_id)
                users_collection.update_one(
                    {"_id": ObjectId(user_id)},
                    {"$set": {"orgId": user_org_ids, "updatedAt": now}}
                )
            
            # Update roleIds if provided
            if request.roleIds:
                # Verify roles belong to this organization
                valid_roles = list(roles_collection.find({
                    "_id": {"$in": [ObjectId(rid) for rid in request.roleIds if ObjectId.is_valid(rid)]},
                    "orgId": org_id
                }))
                valid_role_ids = [str(role["_id"]) for role in valid_roles]
                
                users_collection.update_one(
                    {"_id": ObjectId(user_id)},
                    {"$set": {"roleIds": valid_role_ids, "updatedAt": now}}
                )
        else:
            # Create new user
            user_data = {
                "email": request.email,
                "name": request.name,
                "password": None,
                "picture": None,
                "roleIds": request.roleIds or [],
                "orgId": [org_id],  # Array format
                "isAdmin": False,
                "lastSeenAt": now,
                "createdAt": now,
                "firebaseUid": None,  # Will be set when they log in
                "updatedAt": now,
            }
            
            result = users_collection.insert_one(user_data)
            user_id = str(result.inserted_id)
        
        # Fetch updated user
        updated_user = users_collection.find_one({"_id": ObjectId(user_id)})
        
        return EmployeeResponse(
            id=user_id,
            name=updated_user.get("name", ""),
            email=updated_user.get("email", ""),
            picture=updated_user.get("picture"),
            roleIds=updated_user.get("roleIds", []),
            isAdmin=user_id in org.get("admins", []),
            createdAt=updated_user.get("createdAt").isoformat() if updated_user.get("createdAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating employee: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create employee: {str(e)}")

@router.put("/{employee_id}", response_model=EmployeeResponse)
async def update_employee(
    org_id: str,
    employee_id: str,
    request: UpdateEmployeeRequest,
    current_user: dict = Depends(get_current_user)
):
    """Update employee (assign roles, update name) - only admins can update"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can update employees")
        
        # Check if employee belongs to this organization
        employee = users_collection.find_one({"_id": ObjectId(employee_id)})
        if not employee:
            raise HTTPException(status_code=404, detail="Employee not found")
        
        user_org_ids = employee.get("orgId", [])
        if isinstance(user_org_ids, str):
            user_org_ids = [user_org_ids]
        
        if org_id not in user_org_ids:
            raise HTTPException(status_code=403, detail="Employee does not belong to this organization")
        
        now = datetime.utcnow()
        update_data = {"updatedAt": now}
        
        if request.name:
            update_data["name"] = request.name
        
        if request.roleIds is not None:
            # Verify roles belong to this organization
            valid_roles = list(roles_collection.find({
                "_id": {"$in": [ObjectId(rid) for rid in request.roleIds if ObjectId.is_valid(rid)]},
                "orgId": org_id
            }))
            valid_role_ids = [str(role["_id"]) for role in valid_roles]
            update_data["roleIds"] = valid_role_ids
        
        users_collection.update_one(
            {"_id": ObjectId(employee_id)},
            {"$set": update_data}
        )
        
        updated_employee = users_collection.find_one({"_id": ObjectId(employee_id)})
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        
        return EmployeeResponse(
            id=employee_id,
            name=updated_employee.get("name", ""),
            email=updated_employee.get("email", ""),
            picture=updated_employee.get("picture"),
            roleIds=updated_employee.get("roleIds", []),
            isAdmin=employee_id in (org or {}).get("admins", []),
            createdAt=updated_employee.get("createdAt").isoformat() if updated_employee.get("createdAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating employee: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to update employee: {str(e)}")

@router.delete("/{employee_id}")
async def remove_employee(
    org_id: str,
    employee_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Remove employee from organization - only admins can remove"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can remove employees")
        
        # Prevent removing yourself
        if str(user_doc["_id"]) == employee_id:
            raise HTTPException(status_code=400, detail="Cannot remove yourself from organization")
        
        employee = users_collection.find_one({"_id": ObjectId(employee_id)})
        if not employee:
            raise HTTPException(status_code=404, detail="Employee not found")
        
        user_org_ids = employee.get("orgId", [])
        if isinstance(user_org_ids, str):
            user_org_ids = [user_org_ids]
        
        if org_id not in user_org_ids:
            raise HTTPException(status_code=404, detail="Employee does not belong to this organization")
        
        # Remove org from user's orgId list
        user_org_ids.remove(org_id)
        
        now = datetime.utcnow()
        if len(user_org_ids) == 0:
            # If no orgs left, set to None
            users_collection.update_one(
                {"_id": ObjectId(employee_id)},
                {"$set": {"orgId": None, "updatedAt": now}}
            )
        else:
            users_collection.update_one(
                {"_id": ObjectId(employee_id)},
                {"$set": {"orgId": user_org_ids, "updatedAt": now}}
            )
        
        # Remove from org admins if they are admin
        organizations_collection.update_one(
            {"_id": ObjectId(org_id)},
            {"$pull": {"admins": employee_id}}
        )
        
        return {"message": "Employee removed from organization successfully"}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error removing employee: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to remove employee: {str(e)}")

