"""Role management API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from models.role import RoleResponse, CreateRoleRequest, UpdateRoleRequest
from api.dependencies import get_current_user
from config.database import roles_collection, organizations_collection

router = APIRouter(prefix="/api/organizations/{org_id}/roles", tags=["roles"])

def is_org_admin(user_doc: dict, org_id: str) -> bool:
    """Check if user is admin of the organization"""
    user_id = str(user_doc["_id"])
    org = organizations_collection.find_one({"_id": ObjectId(org_id)})
    if not org:
        return False
    admins = org.get("admins", [])
    return user_id in admins

@router.get("", response_model=list[RoleResponse])
async def get_roles(
    org_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Get all roles in an organization"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user belongs to this organization
        user_org_ids = user_doc.get("orgId", [])
        if isinstance(user_org_ids, str):
            user_org_ids = [user_org_ids]
        
        if org_id not in user_org_ids and not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="User does not belong to this organization")
        
        roles = list(roles_collection.find({"orgId": org_id}))
        
        return [
            RoleResponse(
                id=str(role["_id"]),
                name=role.get("name", ""),
                permissions=role.get("permissions", []),
                orgId=role.get("orgId", ""),
                createdAt=role.get("createdAt").isoformat() if role.get("createdAt") else "",
                updatedAt=role.get("updatedAt").isoformat() if role.get("updatedAt") else ""
            )
            for role in roles
        ]
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching roles: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch roles: {str(e)}")

@router.post("", response_model=RoleResponse)
async def create_role(
    org_id: str,
    request: CreateRoleRequest,
    current_user: dict = Depends(get_current_user)
):
    """Create a new role in an organization - only admins can create roles"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can create roles")
        
        # Check if organization exists
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found")
        
        # Check if role name already exists in this org
        existing_role = roles_collection.find_one({
            "name": request.name,
            "orgId": org_id
        })
        if existing_role:
            raise HTTPException(status_code=400, detail="Role with this name already exists")
        
        now = datetime.utcnow()
        
        role_data = {
            "name": request.name,
            "permissions": request.permissions,
            "orgId": org_id,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = roles_collection.insert_one(role_data)
        role_id = str(result.inserted_id)
        
        return RoleResponse(
            id=role_id,
            name=request.name,
            permissions=request.permissions,
            orgId=org_id,
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating role: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create role: {str(e)}")

@router.put("/{role_id}", response_model=RoleResponse)
async def update_role(
    org_id: str,
    role_id: str,
    request: UpdateRoleRequest,
    current_user: dict = Depends(get_current_user)
):
    """Update a role - only admins can update roles"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can update roles")
        
        role = roles_collection.find_one({"_id": ObjectId(role_id), "orgId": org_id})
        if not role:
            raise HTTPException(status_code=404, detail="Role not found")
        
        now = datetime.utcnow()
        update_data = {"updatedAt": now}
        
        if request.name:
            # Check if name conflict
            existing_role = roles_collection.find_one({
                "name": request.name,
                "orgId": org_id,
                "_id": {"$ne": ObjectId(role_id)}
            })
            if existing_role:
                raise HTTPException(status_code=400, detail="Role with this name already exists")
            update_data["name"] = request.name
        
        if request.permissions is not None:
            update_data["permissions"] = request.permissions
        
        roles_collection.update_one(
            {"_id": ObjectId(role_id)},
            {"$set": update_data}
        )
        
        updated_role = roles_collection.find_one({"_id": ObjectId(role_id)})
        
        return RoleResponse(
            id=role_id,
            name=updated_role.get("name", ""),
            permissions=updated_role.get("permissions", []),
            orgId=updated_role.get("orgId", ""),
            createdAt=updated_role.get("createdAt").isoformat() if updated_role.get("createdAt") else "",
            updatedAt=updated_role.get("updatedAt").isoformat() if updated_role.get("updatedAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating role: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to update role: {str(e)}")

@router.delete("/{role_id}")
async def delete_role(
    org_id: str,
    role_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Delete a role - only admins can delete roles"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can delete roles")
        
        role = roles_collection.find_one({"_id": ObjectId(role_id), "orgId": org_id})
        if not role:
            raise HTTPException(status_code=404, detail="Role not found")
        
        roles_collection.delete_one({"_id": ObjectId(role_id)})
        
        return {"message": "Role deleted successfully"}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error deleting role: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to delete role: {str(e)}")

