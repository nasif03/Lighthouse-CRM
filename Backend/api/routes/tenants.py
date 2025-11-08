"""Tenant/Organization switching API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from models.tenant import TenantResponse, TenantListResponse, SwitchTenantRequest
from api.dependencies import get_current_user
from config.database import users_collection, organizations_collection
from utils.performance import time_operation, time_database_query
from services.user_cache import clear_user_cache

router = APIRouter(prefix="/api/tenants", tags=["tenants"])

@router.get("", response_model=TenantListResponse)
async def get_tenants(current_user: dict = Depends(get_current_user)):
    """Get all organizations (tenants) that the current user belongs to"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        user_id = str(user_doc["_id"])
        org_ids = user_doc.get("orgId", [])
        
        # If orgId is a string (single org), convert to list
        if isinstance(org_ids, str):
            org_ids = [org_ids]
        
        # Normalize org_ids to strings
        org_ids_str = [str(oid) if oid else None for oid in org_ids]
        
        # Also find organizations where user is admin (they should be able to switch to these)
        admin_orgs = list(organizations_collection.find({"admins": user_id}))
        admin_org_ids = [str(org["_id"]) for org in admin_orgs]
        
        # Combine user's orgIds and admin orgIds
        all_org_ids = list(set(org_ids_str + admin_org_ids))
        
        if not all_org_ids:
            return TenantListResponse(tenants=[], activeTenantId=None)
        
        # Fetch organizations
        with time_database_query("organizations", "find"):
            orgs = list(organizations_collection.find(
                {"_id": {"$in": [ObjectId(oid) for oid in all_org_ids if ObjectId.is_valid(oid)]}}
            ))
        
        with time_operation("Tenants: Transform response", threshold_ms=10.0):
            tenants = [
                TenantResponse(
                    id=str(org["_id"]),
                    name=org.get("name", "Unknown Organization")
                )
                for org in orgs
            ]
        
        # Get active tenant - check user's activeOrgId first, otherwise use first org
        stored_active_org_id = user_doc.get("activeOrgId")
        if stored_active_org_id and stored_active_org_id in all_org_ids:
            active_tenant_id = stored_active_org_id
        else:
            active_tenant_id = str(orgs[0]["_id"]) if orgs else None
            # If no activeOrgId is set, set it to the first org
            if active_tenant_id and not stored_active_org_id:
                users_collection.update_one(
                    {"_id": ObjectId(user_id)},
                    {"$set": {"activeOrgId": active_tenant_id, "updatedAt": datetime.utcnow()}}
                )
        
        return TenantListResponse(tenants=tenants, activeTenantId=active_tenant_id)
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching tenants: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch tenants: {str(e)}")

@router.post("/switch", response_model=dict)
async def switch_tenant(
    request: SwitchTenantRequest,
    current_user: dict = Depends(get_current_user)
):
    """Switch active tenant context for the user (updates user's primary orgId)"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        user_id = user_doc["_id"]
        org_ids = user_doc.get("orgId", [])
        
        # If orgId is a string, convert to list
        if isinstance(org_ids, str):
            org_ids = [org_ids]
        
        tenant_id = request.tenant_id
        
        # Normalize org_ids and tenant_id to strings for comparison
        org_ids_str = [str(oid) if oid else None for oid in org_ids]
        tenant_id_str = str(tenant_id) if tenant_id else None
        
        # Verify user belongs to this organization
        if tenant_id_str not in org_ids_str:
            # Also check if user is admin of the organization (they should be able to switch to it)
            org = organizations_collection.find_one({"_id": ObjectId(tenant_id) if ObjectId.is_valid(tenant_id) else tenant_id})
            if not org:
                raise HTTPException(status_code=404, detail="Organization not found")
            
            user_id_str = str(user_id)
            if user_id_str not in org.get("admins", []):
                raise HTTPException(status_code=403, detail="User does not belong to this organization")
        
        # Store active tenant preference in user document
        # We'll use a custom field 'activeOrgId' to track the active tenant
        # This allows users to have multiple orgs but work within one context at a time
        now = datetime.utcnow()
        try:
            users_collection.update_one(
                {"_id": user_id},
                {"$set": {"activeOrgId": tenant_id, "updatedAt": now}}
            )
        except Exception as e:
            # Handle validation errors
            error_str = str(e).lower()
            if "validation" in error_str or "121" in error_str:
                # Try using database command to bypass validation
                try:
                    from config.database import db
                    db.command({
                        "update": "users",
                        "updates": [{
                            "q": {"_id": user_id},
                            "u": {"$set": {"activeOrgId": tenant_id, "updatedAt": now}},
                            "bypassDocumentValidation": True
                        }]
                    })
                except Exception as e2:
                    # Try to update validator and retry
                    from config.database import update_validators
                    update_validators()
                    users_collection.update_one(
                        {"_id": user_id},
                        {"$set": {"activeOrgId": tenant_id, "updatedAt": now}}
                    )
            else:
                raise
        
        # Clear user cache to force refresh with new activeOrgId
        # Get token from request to clear specific user's cache
        # Note: We need to get the token from the request, but it's not directly available
        # For now, clear all cache (or we could pass token in request)
        clear_user_cache()
        
        return {
            "message": "Tenant switched successfully",
            "tenantId": tenant_id
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error switching tenant: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to switch tenant: {str(e)}")

