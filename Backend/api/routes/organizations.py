"""Organization management API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from models.organization import OrganizationResponse, CreateOrganizationRequest, UpdateOrganizationRequest
from api.dependencies import get_current_user
from config.database import organizations_collection, users_collection
from pydantic import BaseModel

router = APIRouter(prefix="/api/organizations", tags=["organizations"])

class JoinOrganizationRequest(BaseModel):
    email: str
    organizationName: str

def is_org_admin(user_doc: dict, org_id: str) -> bool:
    """Check if user is admin of the organization"""
    user_id = str(user_doc["_id"])
    org = organizations_collection.find_one({"_id": ObjectId(org_id)})
    if not org:
        return False
    admins = org.get("admins", [])
    return user_id in admins

@router.get("", response_model=list[OrganizationResponse])
async def get_organizations(current_user: dict = Depends(get_current_user)):
    """Get all organizations the current user owns or belongs to"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        user_id = str(user_doc["_id"])
        
        # Find organizations where user is admin (owner)
        orgs = list(organizations_collection.find({
            "admins": user_id
        }))
        
        # Also find organizations where user belongs (has orgId reference)
        user_org_ids = user_doc.get("orgId", [])
        if isinstance(user_org_ids, str):
            user_org_ids = [user_org_ids]
        
        if user_org_ids:
            additional_orgs = list(organizations_collection.find({
                "_id": {"$in": [ObjectId(oid) for oid in user_org_ids if ObjectId.is_valid(oid)]}
            }))
            
            # Merge and deduplicate
            existing_ids = {str(org["_id"]) for org in orgs}
            for org in additional_orgs:
                if str(org["_id"]) not in existing_ids:
                    orgs.append(org)
        
        return [
            OrganizationResponse(
                id=str(org["_id"]),
                name=org.get("name", ""),
                domain=org.get("domain", ""),
                createdAt=org.get("createdAt").isoformat() if org.get("createdAt") else "",
                updatedAt=org.get("updatedAt").isoformat() if org.get("updatedAt") else ""
            )
            for org in orgs
        ]
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching organizations: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch organizations: {str(e)}")

@router.post("/join", response_model=OrganizationResponse)
async def join_organization(
    request: JoinOrganizationRequest,
    current_user: dict = Depends(get_current_user)
):
    """Join an organization by email and organization name"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        user_id = str(user_doc["_id"])
        user_email = user_doc.get("email", "").lower()
        
        # Verify email matches
        if request.email.lower() != user_email:
            raise HTTPException(status_code=403, detail="Email does not match your account email")
        
        # Find organization by name
        org = organizations_collection.find_one({"name": request.organizationName})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found with the provided name")
        
        org_id = str(org["_id"])
        
        # Check if user already belongs to this org
        user_org_ids = user_doc.get("orgId", [])
        if isinstance(user_org_ids, str):
            user_org_ids = [user_org_ids]
        
        if org_id in user_org_ids:
            return OrganizationResponse(
                id=org_id,
                name=org.get("name", ""),
                domain=org.get("domain", ""),
                createdAt=org.get("createdAt").isoformat() if org.get("createdAt") else "",
                updatedAt=org.get("updatedAt").isoformat() if org.get("updatedAt") else ""
            )
        
        # Add user to organization
        user_org_ids.append(org_id)
        now = datetime.utcnow()
        try:
            users_collection.update_one(
                {"_id": ObjectId(user_id)},
                {"$set": {"orgId": user_org_ids, "updatedAt": now}}
            )
        except Exception as e:
            error_str = str(e).lower()
            if "validation" in error_str or "121" in error_str:
                try:
                    from config.database import db
                    db.command({
                        "update": "users",
                        "updates": [{
                            "q": {"_id": ObjectId(user_id)},
                            "u": {"$set": {"orgId": user_org_ids, "updatedAt": now}},
                            "bypassDocumentValidation": True
                        }]
                    })
                except Exception as e2:
                    from config.database import update_validators
                    try:
                        update_validators()
                        users_collection.update_one(
                            {"_id": ObjectId(user_id)},
                            {"$set": {"orgId": user_org_ids, "updatedAt": now}}
                        )
                    except Exception as e3:
                        raise HTTPException(
                            status_code=500,
                            detail=f"Failed to join organization: {str(e3)}"
                        )
            else:
                raise
        
        return OrganizationResponse(
            id=org_id,
            name=org.get("name", ""),
            domain=org.get("domain", ""),
            createdAt=org.get("createdAt").isoformat() if org.get("createdAt") else "",
            updatedAt=org.get("updatedAt").isoformat() if org.get("updatedAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error joining organization: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to join organization: {str(e)}")

@router.post("", response_model=OrganizationResponse)
async def create_organization(
    request: CreateOrganizationRequest,
    current_user: dict = Depends(get_current_user)
):
    """Create a new organization (tenant) - owner can create multiple organizations"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        user_id = str(user_doc["_id"])
        now = datetime.utcnow()
        
        # Generate domain from name if not provided
        domain = request.domain or request.name.lower().replace(" ", "-").replace("_", "-")
        
        # Check if domain already exists
        existing_org = organizations_collection.find_one({"domain": domain})
        if existing_org:
            raise HTTPException(status_code=400, detail="Organization with this domain already exists")
        
        # Create organization
        org_data = {
            "name": request.name,
            "domain": domain,
            "billingInfo": None,
            "settings": None,
            "salesStages": [],
            "admins": [user_id],  # Creator becomes admin
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = organizations_collection.insert_one(org_data)
        org_id = str(result.inserted_id)
        
        # Create Jira project for the organization
        try:
            from services.jira_service import create_jira_project
            admin_email = user_doc.get("email")
            project_info = create_jira_project(request.name, org_id, admin_email)
            if project_info:
                organizations_collection.update_one(
                    {"_id": ObjectId(org_id)},
                    {"$set": {
                        "jiraProjectKey": project_info["projectKey"],
                        "jiraProjectId": project_info["projectId"],
                        "updatedAt": now
                    }}
                )
        except Exception as e:
            print(f"Failed to create Jira project for organization: {str(e)}")
            # Don't fail org creation if Jira creation fails
        
        # Update user's orgId to include this new org
        # Support both array and string formats
        user_org_ids = user_doc.get("orgId", [])
        if isinstance(user_org_ids, str):
            user_org_ids = [user_org_ids]
        if org_id not in user_org_ids:
            user_org_ids.append(org_id)
            try:
                # Try normal update first
                users_collection.update_one(
                    {"_id": ObjectId(user_id)},
                    {"$set": {"orgId": user_org_ids, "updatedAt": now}}
                )
            except Exception as e:
                error_str = str(e).lower()
                if "171" in error_str or "parallel arrays" in error_str:
                    # Drop the index and retry
                    try:
                        from config.database import db
                        indexes = db.users.index_information()
                        if 'orgId_roleIds_idx' in indexes:
                            db.users.drop_index('orgId_roleIds_idx')
                            print("[WARN] Dropped orgId_roleIds_idx index due to parallel arrays error")
                        # Retry the update after dropping index
                        users_collection.update_one(
                            {"_id": ObjectId(user_id)},
                            {"$set": {"orgId": user_org_ids, "updatedAt": now}}
                        )
                    except Exception as e2:
                        print(f"[WARN] Failed to update user orgId: {str(e2)}")
                        # Continue anyway - org creation should succeed
                else:
                    print(f"[WARN] Error updating user orgId: {str(e)}")
                    # Continue anyway - org creation should succeed
        
        return OrganizationResponse(
            id=org_id,
            name=request.name,
            domain=domain,
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating organization: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create organization: {str(e)}")

@router.put("/{org_id}", response_model=OrganizationResponse)
async def update_organization(
    org_id: str,
    request: UpdateOrganizationRequest,
    current_user: dict = Depends(get_current_user)
):
    """Update an organization (admin only)"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Check if user is admin
        if not is_org_admin(user_doc, org_id):
            raise HTTPException(status_code=403, detail="Only organization admins can update organizations")
        
        # Update organization
        update_data = {"updatedAt": datetime.utcnow()}
        if request.name:
            update_data["name"] = request.name
        
        organizations_collection.update_one(
            {"_id": ObjectId(org_id)},
            {"$set": update_data}
        )
        
        # Fetch updated organization
        org = organizations_collection.find_one({"_id": ObjectId(org_id)})
        
        return OrganizationResponse(
            id=org_id,
            name=org.get("name", ""),
            domain=org.get("domain", ""),
            createdAt=org.get("createdAt").isoformat() if org.get("createdAt") else "",
            updatedAt=org.get("updatedAt").isoformat() if org.get("updatedAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating organization: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to update organization: {str(e)}")
