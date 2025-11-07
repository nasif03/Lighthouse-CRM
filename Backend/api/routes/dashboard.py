"""Dashboard API routes"""
from fastapi import APIRouter, HTTPException, Depends
from datetime import datetime, timedelta
from bson import ObjectId
from api.dependencies import get_current_user
from config.database import (
    leads_collection, 
    contacts_collection, 
    deals_collection, 
    accounts_collection,
    activities_collection
)
from utils.query_filters import build_user_filter

router = APIRouter(prefix="/api/dashboard", tags=["dashboard"])

@router.get("/stats")
async def get_dashboard_stats(current_user: dict = Depends(get_current_user)):
    """Get dashboard statistics for the current user"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        # Build filter for user's data
        query_filter = build_user_filter(user_doc, include_owner=True)
        org_id = user_doc.get("orgId")
        
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        # Calculate date ranges
        now = datetime.utcnow()
        today_start = datetime(now.year, now.month, now.day)
        week_ago = now - timedelta(days=7)
        month_ago = now - timedelta(days=30)
        
        # Get counts
        total_leads = leads_collection.count_documents(query_filter)
        total_contacts = contacts_collection.count_documents(query_filter)
        total_deals = deals_collection.count_documents(query_filter)
        total_accounts = accounts_collection.count_documents({**query_filter, "deleted": {"$ne": True}})
        
        # Recent counts (last 7 days)
        recent_leads_filter = {
            **query_filter,
            "createdAt": {"$gte": week_ago}
        }
        recent_leads = leads_collection.count_documents(recent_leads_filter)
        
        recent_contacts_filter = {
            **query_filter,
            "createdAt": {"$gte": week_ago}
        }
        recent_contacts = contacts_collection.count_documents(recent_contacts_filter)
        
        recent_deals_filter = {
            **query_filter,
            "createdAt": {"$gte": week_ago}
        }
        recent_deals = deals_collection.count_documents(recent_deals_filter)
        
        # Leads by status
        leads_by_status = {}
        for status in ["new", "contacted", "qualified", "converted", "lost"]:
            status_filter = {**query_filter, "status": status}
            leads_by_status[status] = leads_collection.count_documents(status_filter)
        
        # Deals by stage
        deals_by_stage = {}
        stages = ["prospecting", "qualification", "proposal", "negotiation", "closed-won", "closed-lost"]
        for stage in stages:
            stage_filter = {**query_filter, "stageId": stage}
            deals_by_stage[stage] = deals_collection.count_documents(stage_filter)
        
        # Calculate deal value
        deals_with_amount = deals_collection.find(
            {**query_filter, "amount": {"$exists": True, "$ne": None}},
            {"amount": 1, "currency": 1, "stageId": 1}
        )
        
        total_deal_value = 0
        won_deal_value = 0
        for deal in deals_with_amount:
            amount = deal.get("amount", 0) or 0
            total_deal_value += amount
            if deal.get("stageId") == "closed-won":
                won_deal_value += amount
        
        # Recent activities count
        activities_filter = {
            "orgId": org_id,
            "createdAt": {"$gte": week_ago}
        }
        recent_activities = activities_collection.count_documents(activities_filter)
        
        # Conversion rate (leads converted / total leads)
        converted_leads = leads_by_status.get("converted", 0)
        conversion_rate = (converted_leads / total_leads * 100) if total_leads > 0 else 0
        
        return {
            "summary": {
                "totalLeads": total_leads,
                "totalContacts": total_contacts,
                "totalDeals": total_deals,
                "totalAccounts": total_accounts,
                "recentLeads": recent_leads,
                "recentContacts": recent_contacts,
                "recentDeals": recent_deals,
                "recentActivities": recent_activities,
                "totalDealValue": total_deal_value,
                "wonDealValue": won_deal_value,
                "conversionRate": round(conversion_rate, 2)
            },
            "leadsByStatus": leads_by_status,
            "dealsByStage": deals_by_stage
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching dashboard stats: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch dashboard stats: {str(e)}")

@router.get("/recent")
async def get_recent_items(current_user: dict = Depends(get_current_user)):
    """Get recent leads, deals, and contacts"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        query_filter = build_user_filter(user_doc, include_owner=True)
        
        # Get recent leads (last 5)
        recent_leads = list(leads_collection.find(
            query_filter,
            {"name": 1, "email": 1, "status": 1, "source": 1, "createdAt": 1}
        ).sort("createdAt", -1).limit(5))
        
        # Get recent deals (last 5)
        recent_deals = list(deals_collection.find(
            query_filter,
            {"name": 1, "amount": 1, "currency": 1, "stageId": 1, "stageName": 1, "createdAt": 1}
        ).sort("createdAt", -1).limit(5))
        
        # Get recent contacts (last 5)
        recent_contacts = list(contacts_collection.find(
            query_filter,
            {"firstName": 1, "lastName": 1, "email": 1, "title": 1, "createdAt": 1}
        ).sort("createdAt", -1).limit(5))
        
        return {
            "recentLeads": [
                {
                    "id": str(lead["_id"]),
                    "name": lead.get("name", ""),
                    "email": lead.get("email", ""),
                    "status": lead.get("status", ""),
                    "source": lead.get("source", ""),
                    "createdAt": lead.get("createdAt").isoformat() if lead.get("createdAt") else ""
                }
                for lead in recent_leads
            ],
            "recentDeals": [
                {
                    "id": str(deal["_id"]),
                    "name": deal.get("name", ""),
                    "amount": deal.get("amount"),
                    "currency": deal.get("currency", "USD"),
                    "stageId": deal.get("stageId", ""),
                    "stageName": deal.get("stageName", ""),
                    "createdAt": deal.get("createdAt").isoformat() if deal.get("createdAt") else ""
                }
                for deal in recent_deals
            ],
            "recentContacts": [
                {
                    "id": str(contact["_id"]),
                    "name": f"{contact.get('firstName', '')} {contact.get('lastName', '')}".strip(),
                    "email": contact.get("email", ""),
                    "title": contact.get("title", ""),
                    "createdAt": contact.get("createdAt").isoformat() if contact.get("createdAt") else ""
                }
                for contact in recent_contacts
            ]
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching recent items: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch recent items: {str(e)}")

