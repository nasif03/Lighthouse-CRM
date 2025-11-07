"""Leads API routes"""
from fastapi import APIRouter, HTTPException, Depends
from datetime import datetime
import time
from bson import ObjectId
from models.lead import LeadResponse, CreateLeadRequest, UpdateLeadStatusRequest
from api.dependencies import get_current_user
from config.database import leads_collection, accounts_collection, contacts_collection, deals_collection
from services.activity_log import log_lead_created, log_lead_converted
from utils.performance import time_operation, time_database_query
from utils.query_filters import build_user_filter, get_user_ids

router = APIRouter(prefix="/api/leads", tags=["leads"])

@router.post("", response_model=LeadResponse)
async def create_lead(request: CreateLeadRequest, current_user: dict = Depends(get_current_user)):
    """Create a new lead"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Get user IDs using reusable utility
        user_ids = get_user_ids(user_doc)
        owner_id = user_ids["ownerId"]
        org_id = user_ids["orgId"]
        
        now = datetime.utcnow()
        
        # Parse name into firstName and lastName if not provided
        name_parts = request.name.strip().split(" ", 1)
        first_name = request.firstName or name_parts[0] if name_parts else ""
        last_name = request.lastName or (name_parts[1] if len(name_parts) > 1 else "")
        
        lead_data = {
            "accountId": None,
            "contactId": None,
            "name": request.name,
            "firstName": first_name,
            "lastName": last_name,
            "email": request.email,
            "phone": request.phone or "",
            "source": request.source,
            "ownerId": owner_id,
            "orgId": org_id,
            "status": request.status,
            "tags": [],
            "converted": False,
            "metadata": None,
            "createdAt": now,
            "updatedAt": now,
        }
        
        try:
            result = leads_collection.insert_one(lead_data)
            lead_id = str(result.inserted_id)
            
            if not result.inserted_id:
                raise HTTPException(status_code=500, detail="Failed to insert lead into database")
            
            inserted_lead = leads_collection.find_one({"_id": result.inserted_id})
            if not inserted_lead:
                raise HTTPException(status_code=500, detail="Lead was not found after insertion")
            
            # Log activity
            log_lead_created(org_id, owner_id, lead_id, request.name)
            
        except Exception as db_error:
            print(f"Database error creating lead: {str(db_error)}")
            raise HTTPException(status_code=500, detail=f"Database error: {str(db_error)}")
        
        return LeadResponse(
            id=lead_id,
            name=request.name,
            email=request.email,
            source=request.source,
            status=request.status,
            ownerId=owner_id,
            orgId=org_id,
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating lead: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to create lead: {str(e)}")

@router.get("", response_model=list[LeadResponse])
async def get_leads(
    skip: int = 0,
    limit: int = 100,
    current_user: dict = Depends(get_current_user)
):
    """Get leads for the current user (ownerId) within their organization (orgId) with pagination"""
    endpoint_start = time.perf_counter()
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Build filter with both orgId and ownerId for data isolation
        query_filter = build_user_filter(user_doc, include_owner=True)
        
        try:
            with time_database_query("leads", "find"):
                cursor = leads_collection.find(
                    query_filter,
                    {"name": 1, "email": 1, "source": 1, "status": 1, "phone": 1, 
                     "firstName": 1, "lastName": 1, "tags": 1, "ownerId": 1, 
                     "orgId": 1, "createdAt": 1, "updatedAt": 1}
                ).sort("createdAt", -1).skip(skip).limit(limit)
                leads = list(cursor)
        except Exception as db_error:
            print(f"Database error fetching leads: {str(db_error)}")
            raise HTTPException(status_code=500, detail=f"Database error: {str(db_error)}")
        
        with time_operation("Leads: Transform response", threshold_ms=50.0):
            result = [
                LeadResponse(
                    id=str(lead["_id"]),
                    name=lead.get("name", ""),
                    email=lead.get("email", ""),
                    source=lead.get("source", ""),
                    status=lead.get("status", "new"),
                    phone=lead.get("phone"),
                    firstName=lead.get("firstName"),
                    lastName=lead.get("lastName"),
                    tags=lead.get("tags", []),
                    ownerId=lead.get("ownerId", ""),
                    orgId=lead.get("orgId", ""),
                    createdAt=lead.get("createdAt").isoformat() if lead.get("createdAt") else "",
                    updatedAt=lead.get("updatedAt").isoformat() if lead.get("updatedAt") else ""
                )
                for lead in leads
            ]
        
        endpoint_elapsed = (time.perf_counter() - endpoint_start) * 1000
        print(f"✅ [GET /api/leads] Total: {endpoint_elapsed:.2f}ms, returned {len(result)} leads")
        return result
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching leads: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to fetch leads: {str(e)}")

@router.patch("/{lead_id}/status", response_model=LeadResponse)
async def update_lead_status(
    lead_id: str,
    request: UpdateLeadStatusRequest,
    current_user: dict = Depends(get_current_user)
):
    """Update lead status - only for the current user's leads"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Build filter to ensure user can only update their own leads
        query_filter = build_user_filter(user_doc, include_owner=True)
        query_filter["_id"] = ObjectId(lead_id)
        
        # Fetch the lead - only if it belongs to the current user
        lead = leads_collection.find_one(query_filter)
        if not lead:
            raise HTTPException(status_code=404, detail="Lead not found")
        
        # Validate status value
        valid_statuses = ["new", "contacted", "qualified", "converted", "lost"]
        if request.status not in valid_statuses:
            raise HTTPException(
                status_code=400, 
                detail=f"Invalid status. Must be one of: {', '.join(valid_statuses)}"
            )
        
        now = datetime.utcnow()
        
        # Update the lead status
        update_result = leads_collection.update_one(
            query_filter,
            {
                "$set": {
                    "status": request.status,
                    "updatedAt": now
                }
            }
        )
        
        if update_result.matched_count == 0:
            raise HTTPException(status_code=404, detail="Lead not found")
        
        # Fetch updated lead
        updated_lead = leads_collection.find_one(query_filter)
        if not updated_lead:
            raise HTTPException(status_code=500, detail="Failed to fetch updated lead")
        
        return LeadResponse(
            id=str(updated_lead["_id"]),
            name=updated_lead.get("name", ""),
            email=updated_lead.get("email", ""),
            source=updated_lead.get("source", ""),
            status=updated_lead.get("status", "new"),
            phone=updated_lead.get("phone"),
            firstName=updated_lead.get("firstName"),
            lastName=updated_lead.get("lastName"),
            tags=updated_lead.get("tags", []),
            ownerId=updated_lead.get("ownerId", ""),
            orgId=updated_lead.get("orgId", ""),
            createdAt=updated_lead.get("createdAt").isoformat() if updated_lead.get("createdAt") else "",
            updatedAt=updated_lead.get("updatedAt").isoformat() if updated_lead.get("updatedAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating lead status: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to update lead status: {str(e)}")

@router.post("/{lead_id}/convert", response_model=dict)
async def convert_lead_to_deal(
    lead_id: str,
    current_user: dict = Depends(get_current_user)
):
    """Convert a lead to Account, Contact, and Deal"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Get user IDs using reusable utility
        user_ids = get_user_ids(user_doc)
        owner_id = user_ids["ownerId"]
        org_id = user_ids["orgId"]
        
        # Build filter to ensure user can only convert their own leads
        query_filter = build_user_filter(user_doc, include_owner=True)
        query_filter["_id"] = ObjectId(lead_id)
        
        # Fetch the lead - only if it belongs to the current user
        lead = leads_collection.find_one(query_filter)
        if not lead:
            raise HTTPException(status_code=404, detail="Lead not found")
        
        if lead.get("converted"):
            raise HTTPException(status_code=400, detail="Lead has already been converted")
        
        now = datetime.utcnow()
        
        # Create Account from lead
        account_data = {
            "name": lead.get("name", "").split()[0] if lead.get("name") else "Unknown Company",
            "domain": lead.get("email", "").split("@")[1] if "@" in lead.get("email", "") else "",
            "industry": "",
            "phone": lead.get("phone", ""),
            "status": "active",
            "ownerId": owner_id,
            "orgId": org_id,
            "metadata": None,
            "address": None,
            "deleted": False,
            "createdAt": now,
            "updatedAt": now,
        }
        account_result = accounts_collection.insert_one(account_data)
        account_id = str(account_result.inserted_id)
        
        # Create Contact from lead
        contact_data = {
            "firstName": lead.get("firstName", ""),
            "lastName": lead.get("lastName", ""),
            "email": lead.get("email", ""),
            "phone": lead.get("phone", ""),
            "title": "",
            "accountId": ObjectId(account_id),
            "ownerId": owner_id,
            "orgId": org_id,
            "tags": lead.get("tags", []),
            "metadata": None,
            "deleted": False,
            "createdAt": now,
            "updatedAt": now,
        }
        contact_result = contacts_collection.insert_one(contact_data)
        contact_id = str(contact_result.inserted_id)
        
        # Create Deal from lead
        # Note: Don't include amount, probability, or closeDate if they're None to avoid validation errors
        deal_data = {
            "name": f"Deal: {lead.get('name', 'Untitled')}",
            "accountId": ObjectId(account_id),
            "contactId": ObjectId(contact_id),
            "currency": "USD",
            "stageId": "prospecting",
            "stageName": "Prospecting",
            "status": "open",
            "ownerId": owner_id,
            "orgId": org_id,
            "tags": lead.get("tags", []),
            "lastActivityAt": now,
            "createdAt": now,
            "updatedAt": now,
        }
        # Only add optional fields if they have values (to avoid MongoDB validation errors)
        # These fields will be added later when the deal is updated
        deal_result = deals_collection.insert_one(deal_data)
        deal_id = str(deal_result.inserted_id)
        
        # Update lead as converted
        leads_collection.update_one(
            {"_id": ObjectId(lead_id)},
            {
                "$set": {
                    "converted": True,
                    "convertedAt": now,
                    "convertedBy": owner_id,
                    "status": "converted",
                    "accountId": ObjectId(account_id),
                    "contactId": ObjectId(contact_id),
                    "updatedAt": now
                }
            }
        )
        
        # Log conversion activity
        log_lead_converted(
            org_id=org_id,
            user_id=owner_id,
            lead_id=lead_id,
            lead_name=lead.get("name", "Untitled Lead"),
            account_id=account_id,
            contact_id=contact_id,
            deal_id=deal_id
        )
        
        return {
            "message": "Lead converted successfully",
            "leadId": lead_id,
            "accountId": account_id,
            "contactId": contact_id,
            "dealId": deal_id
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error converting lead: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to convert lead: {str(e)}")

