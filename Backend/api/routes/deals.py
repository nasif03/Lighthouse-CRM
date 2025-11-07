"""Deals API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
import time
from models.deal import DealResponse, CreateDealRequest, UpdateDealRequest
from api.dependencies import get_current_user
from config.database import deals_collection
from services.activity_log import log_deal_created, log_deal_stage_changed
from utils.performance import time_operation, time_database_query

router = APIRouter(prefix="/api/deals", tags=["deals"])

@router.get("", response_model=list[DealResponse])
async def get_deals(
    skip: int = 0,
    limit: int = 100,
    current_user: dict = Depends(get_current_user)
):
    """Get deals for the current user's organization with pagination"""
    endpoint_start = time.perf_counter()
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        with time_database_query("deals", "find"):
            cursor = deals_collection.find(
                {"orgId": org_id},
                {"name": 1, "accountId": 1, "contactId": 1, "amount": 1, "currency": 1, "stageId": 1, "stageName": 1, "probability": 1, "closeDate": 1, "status": 1, "ownerId": 1, "orgId": 1, "tags": 1, "createdAt": 1, "updatedAt": 1}
            ).sort("createdAt", -1).skip(skip).limit(limit)
            deals = list(cursor)
        
        with time_operation("Deals: Transform response", threshold_ms=50.0):
            result = [
                DealResponse(
                    id=str(deal["_id"]),
                    name=deal.get("name", ""),
                    accountId=str(deal["accountId"]) if deal.get("accountId") else None,
                    contactId=str(deal["contactId"]) if deal.get("contactId") else None,
                    amount=deal.get("amount"),
                    currency=deal.get("currency"),
                    stageId=deal.get("stageId"),
                    stageName=deal.get("stageName"),
                    probability=deal.get("probability"),
                    closeDate=deal.get("closeDate").isoformat() if deal.get("closeDate") else None,
                    status=deal.get("status", "open"),
                    ownerId=deal.get("ownerId", ""),
                    orgId=deal.get("orgId", ""),
                    tags=deal.get("tags", []),
                    createdAt=deal.get("createdAt").isoformat() if deal.get("createdAt") else "",
                    updatedAt=deal.get("updatedAt").isoformat() if deal.get("updatedAt") else ""
                )
                for deal in deals
            ]
        
        endpoint_elapsed = (time.perf_counter() - endpoint_start) * 1000
        print(f"✅ [GET /api/deals] Total: {endpoint_elapsed:.2f}ms, returned {len(result)} deals")
        return result
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching deals: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch deals: {str(e)}")

@router.post("", response_model=DealResponse)
async def create_deal(request: CreateDealRequest, current_user: dict = Depends(get_current_user)):
    """Create a new deal"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        owner_id = str(user_doc["_id"])
        org_id = user_doc.get("orgId")
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        now = datetime.utcnow()
        close_date = None
        if request.closeDate:
            try:
                close_date = datetime.fromisoformat(request.closeDate.replace('Z', '+00:00'))
            except:
                pass
        
        deal_data = {
            "name": request.name,
            "accountId": ObjectId(request.accountId) if request.accountId else None,
            "contactId": ObjectId(request.contactId) if request.contactId else None,
            "amount": request.amount,
            "currency": request.currency or "USD",
            "stageId": request.stageId or "",
            "stageName": request.stageName or "",
            "probability": request.probability,
            "closeDate": close_date,
            "status": request.status or "open",
            "ownerId": owner_id,
            "orgId": org_id,
            "tags": request.tags or [],
            "lastActivityAt": now,
            "metadata": None,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = deals_collection.insert_one(deal_data)
        deal_id = str(result.inserted_id)
        
        # Log activity
        log_deal_created(org_id, owner_id, deal_id, request.name)
        
        return DealResponse(
            id=deal_id,
            name=request.name,
            accountId=request.accountId,
            contactId=request.contactId,
            amount=request.amount,
            currency=request.currency or "USD",
            stageId=request.stageId,
            stageName=request.stageName,
            probability=request.probability,
            closeDate=request.closeDate,
            status=request.status or "open",
            ownerId=owner_id,
            orgId=org_id,
            tags=request.tags or [],
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating deal: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create deal: {str(e)}")

@router.put("/{deal_id}", response_model=DealResponse)
async def update_deal(deal_id: str, request: UpdateDealRequest, current_user: dict = Depends(get_current_user)):
    """Update a deal"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        deal = deals_collection.find_one({"_id": ObjectId(deal_id), "orgId": org_id})
        if not deal:
            raise HTTPException(status_code=404, detail="Deal not found")
        
        # Track stage change for activity log
        old_stage = deal.get("stageName", deal.get("stageId", "Unknown"))
        new_stage = None
        
        update_data = {"updatedAt": datetime.utcnow(), "lastActivityAt": datetime.utcnow()}
        if request.name is not None:
            update_data["name"] = request.name
        if request.accountId is not None:
            update_data["accountId"] = ObjectId(request.accountId) if request.accountId else None
        if request.contactId is not None:
            update_data["contactId"] = ObjectId(request.contactId) if request.contactId else None
        if request.amount is not None:
            update_data["amount"] = request.amount
        if request.currency is not None:
            update_data["currency"] = request.currency
        if request.stageId is not None:
            update_data["stageId"] = request.stageId
        if request.stageName is not None:
            update_data["stageName"] = request.stageName
            new_stage = request.stageName
        if request.probability is not None:
            update_data["probability"] = request.probability
        if request.closeDate is not None:
            try:
                update_data["closeDate"] = datetime.fromisoformat(request.closeDate.replace('Z', '+00:00'))
            except:
                pass
        if request.status is not None:
            update_data["status"] = request.status
        if request.tags is not None:
            update_data["tags"] = request.tags
        
        owner_id = str(user_doc["_id"])
        
        deals_collection.update_one({"_id": ObjectId(deal_id)}, {"$set": update_data})
        
        # Log stage change if it occurred
        if new_stage and new_stage != old_stage:
            log_deal_stage_changed(
                org_id=org_id,
                user_id=owner_id,
                deal_id=deal_id,
                deal_name=deal.get("name", "Untitled Deal"),
                old_stage=old_stage,
                new_stage=new_stage
            )
        
        updated_deal = deals_collection.find_one({"_id": ObjectId(deal_id)})
        return DealResponse(
            id=str(updated_deal["_id"]),
            name=updated_deal.get("name", ""),
            accountId=str(updated_deal["accountId"]) if updated_deal.get("accountId") else None,
            contactId=str(updated_deal["contactId"]) if updated_deal.get("contactId") else None,
            amount=updated_deal.get("amount"),
            currency=updated_deal.get("currency"),
            stageId=updated_deal.get("stageId"),
            stageName=updated_deal.get("stageName"),
            probability=updated_deal.get("probability"),
            closeDate=updated_deal.get("closeDate").isoformat() if updated_deal.get("closeDate") else None,
            status=updated_deal.get("status", "open"),
            ownerId=updated_deal.get("ownerId", ""),
            orgId=updated_deal.get("orgId", ""),
            tags=updated_deal.get("tags", []),
            createdAt=updated_deal.get("createdAt").isoformat() if updated_deal.get("createdAt") else "",
            updatedAt=updated_deal.get("updatedAt").isoformat() if updated_deal.get("updatedAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating deal: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to update deal: {str(e)}")

@router.delete("/{deal_id}")
async def delete_deal(deal_id: str, current_user: dict = Depends(get_current_user)):
    """Delete a deal"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        deal = deals_collection.find_one({"_id": ObjectId(deal_id), "orgId": org_id})
        if not deal:
            raise HTTPException(status_code=404, detail="Deal not found")
        
        deals_collection.delete_one({"_id": ObjectId(deal_id)})
        
        return {"message": "Deal deleted successfully"}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error deleting deal: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to delete deal: {str(e)}")

