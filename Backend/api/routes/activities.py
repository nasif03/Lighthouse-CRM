"""Activities API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from models.activity import ActivityResponse, ActivityListResponse
from api.dependencies import get_current_user
from config.database import activities_collection
from utils.performance import time_operation, time_database_query

router = APIRouter(prefix="/api/activities", tags=["activities"])

@router.get("", response_model=ActivityListResponse)
async def get_activities(
    entity_type: str = None,
    entity_id: str = None,
    skip: int = 0,
    limit: int = 50,
    current_user: dict = Depends(get_current_user)
):
    """Get activities for the current user's organization, optionally filtered by entity"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        # Build query
        query = {"orgId": org_id}
        if entity_type:
            query["entityType"] = entity_type
        if entity_id:
            query["entityId"] = entity_id
        
        with time_database_query("activities", "find"):
            cursor = activities_collection.find(query).sort("createdAt", -1).skip(skip).limit(limit)
            activities = list(cursor)
            total = activities_collection.count_documents(query)
        
        with time_operation("Activities: Transform response", threshold_ms=50.0):
            result = [
                ActivityResponse(
                    id=str(activity["_id"]),
                    entityType=activity.get("entityType", ""),
                    entityId=activity.get("entityId", ""),
                    userId=activity.get("userId", ""),
                    orgId=activity.get("orgId", ""),
                    summary=activity.get("summary", ""),
                    body=activity.get("body", ""),
                    tags=activity.get("tags", []),
                    createdAt=activity.get("createdAt").isoformat() if activity.get("createdAt") else ""
                )
                for activity in activities
            ]
        
        return ActivityListResponse(activities=result, total=total)
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching activities: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch activities: {str(e)}")

@router.get("/entity/{entity_type}/{entity_id}", response_model=ActivityListResponse)
async def get_entity_activities(
    entity_type: str,
    entity_id: str,
    skip: int = 0,
    limit: int = 50,
    current_user: dict = Depends(get_current_user)
):
    """Get activities for a specific entity (lead, deal, account, contact)"""
    return await get_activities(
        entity_type=entity_type,
        entity_id=entity_id,
        skip=skip,
        limit=limit,
        current_user=current_user
    )
