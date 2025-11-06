"""Leads API routes"""
from fastapi import APIRouter, HTTPException, Depends
from datetime import datetime
from models.lead import LeadResponse, CreateLeadRequest
from api.dependencies import get_current_user
from config.database import leads_collection

router = APIRouter(prefix="/api/leads", tags=["leads"])

@router.post("", response_model=LeadResponse)
async def create_lead(request: CreateLeadRequest, current_user: dict = Depends(get_current_user)):
    """Create a new lead"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        owner_id = str(user_doc["_id"])
        org_id = user_doc.get("orgId")
        
        if not org_id:
            raise HTTPException(
                status_code=400, 
                detail="User must belong to an organization. Please sign out and sign in again to create an organization."
            )
        
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
    """Get leads for the current user's organization with pagination"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        org_id = user_doc.get("orgId")
        
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        try:
            cursor = leads_collection.find(
                {"orgId": org_id},
                {"name": 1, "email": 1, "source": 1, "status": 1, "ownerId": 1, "orgId": 1, "createdAt": 1, "updatedAt": 1}
            ).sort("createdAt", -1).skip(skip).limit(limit)
            leads = list(cursor)
        except Exception as db_error:
            print(f"Database error fetching leads: {str(db_error)}")
            raise HTTPException(status_code=500, detail=f"Database error: {str(db_error)}")
        
        return [
            LeadResponse(
                id=str(lead["_id"]),
                name=lead.get("name", ""),
                email=lead.get("email", ""),
                source=lead.get("source", ""),
                status=lead.get("status", "new"),
                ownerId=lead.get("ownerId", ""),
                orgId=lead.get("orgId", ""),
                createdAt=lead.get("createdAt").isoformat() if lead.get("createdAt") else "",
                updatedAt=lead.get("updatedAt").isoformat() if lead.get("updatedAt") else ""
            )
            for lead in leads
        ]
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching leads: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to fetch leads: {str(e)}")

