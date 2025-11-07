"""Tenant/Organization switching API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from models.tenant import TenantResponse, TenantListResponse, SwitchTenantRequest
from api.dependencies import get_current_user
from config.database import users_collection, organizations_collection
from utils.performance import time_operation, time_database_query

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
        
        if not org_ids:
            return TenantListResponse(tenants=[], activeTenantId=None)
        
        # Fetch organizations
        with time_database_query("organizations", "find"):
            orgs = list(organizations_collection.find(
                {"_id": {"$in": [ObjectId(oid) if ObjectId.is_valid(oid) else oid for oid in org_ids]}}
            ))
        
        with time_operation("Tenants: Transform response", threshold_ms=10.0):
            tenants = [
                TenantResponse(
                    id=str(org["_id"]),
                    name=org.get("name", "Unknown Organization")
                )
                for org in orgs
            ]
        
        # Get active tenant (first org or user's primary org)
        active_tenant_id = str(orgs[0]["_id"]) if orgs else None
        
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
        
        # Verify user belongs to this organization
        if tenant_id not in org_ids:
            raise HTTPException(status_code=403, detail="User does not belong to this organization")
        
        # Update user's primary orgId (set as string for backward compatibility)
        # Note: In a real multi-tenant system, you might want to track this differently
        users_collection.update_one(
            {"_id": user_id},
            {"$set": {"orgId": tenant_id, "updatedAt": datetime.utcnow()}}
        )
        
        return {
            "message": "Tenant switched successfully",
            "tenantId": tenant_id
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error switching tenant: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to switch tenant: {str(e)}")

