"""MCP tools for dashboard and analytics"""
from typing import Any, Dict, Optional
from datetime import datetime, timedelta
# Import utils using path workaround
import sys
from pathlib import Path
backend_dir = Path(__file__).parent.parent.parent
sys.path.insert(0, str(backend_dir))
import importlib.util
spec = importlib.util.spec_from_file_location("mcp_crm_utils", backend_dir / "mcp-crm" / "utils.py")
mcp_crm_utils = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mcp_crm_utils)
get_user_context = mcp_crm_utils.get_user_context
validate_user_context = mcp_crm_utils.validate_user_context
format_error_response = mcp_crm_utils.format_error_response
format_success_response = mcp_crm_utils.format_success_response
from config.database import (
    leads_collection, contacts_collection, deals_collection,
    accounts_collection, activities_collection
)
from utils.query_filters import build_user_filter


async def get_dashboard_stats(
    context: Optional[Dict] = None
) -> str:
    """
    Get dashboard statistics for the current user.
    
    Args:
        context: MCP request context (for authentication)
    
    Returns:
        Formatted dashboard statistics
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        query_filter = build_user_filter(user_doc, include_owner=True)
        org_id = user_doc.get("orgId")
        
        if not org_id:
            return "User must belong to an organization."
        
        # Calculate date ranges
        now = datetime.utcnow()
        week_ago = now - timedelta(days=7)
        month_ago = now - timedelta(days=30)
        
        # Get counts
        total_leads = leads_collection.count_documents(query_filter)
        total_contacts = contacts_collection.count_documents(query_filter)
        total_deals = deals_collection.count_documents(query_filter)
        total_accounts = accounts_collection.count_documents({**query_filter, "deleted": {"$ne": True}})
        
        # Recent counts (last 7 days)
        recent_leads_filter = {**query_filter, "createdAt": {"$gte": week_ago}}
        recent_leads = leads_collection.count_documents(recent_leads_filter)
        
        recent_contacts_filter = {**query_filter, "createdAt": {"$gte": week_ago}}
        recent_contacts = contacts_collection.count_documents(recent_contacts_filter)
        
        recent_deals_filter = {**query_filter, "createdAt": {"$gte": week_ago}}
        recent_deals = deals_collection.count_documents(recent_deals_filter)
        
        # Leads by status
        leads_by_status = {}
        for status in ["new", "contacted", "qualified", "converted"]:
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
        
        # Conversion rate
        converted_leads = leads_by_status.get("converted", 0)
        conversion_rate = (converted_leads / total_leads * 100) if total_leads > 0 else 0
        
        stats = {
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
        
        return format_success_response(
            "Dashboard statistics retrieved successfully",
            stats
        )
    except Exception as e:
        return format_error_response(e)


async def get_analytics(
    timeframe: str = "30days",
    context: Optional[Dict] = None
) -> str:
    """
    Get analytics data for a specific timeframe.
    
    Args:
        timeframe: Timeframe for analytics (7days, 30days, 90days, all)
        context: MCP request context (for authentication)
    
    Returns:
        Formatted analytics data
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        query_filter = build_user_filter(user_doc, include_owner=True)
        
        # Calculate date range
        now = datetime.utcnow()
        if timeframe == "7days":
            start_date = now - timedelta(days=7)
        elif timeframe == "30days":
            start_date = now - timedelta(days=30)
        elif timeframe == "90days":
            start_date = now - timedelta(days=90)
        else:
            start_date = None
        
        date_filter = {"createdAt": {"$gte": start_date}} if start_date else {}
        
        # Get leads created in timeframe
        leads_filter = {**query_filter, **date_filter}
        leads_count = leads_collection.count_documents(leads_filter)
        
        # Get deals created in timeframe
        deals_filter = {**query_filter, **date_filter}
        deals_count = deals_collection.count_documents(deals_filter)
        
        # Get deal value in timeframe
        deals_with_amount = deals_collection.find(
            {**deals_filter, "amount": {"$exists": True, "$ne": None}},
            {"amount": 1, "stageId": 1}
        )
        
        total_value = sum(deal.get("amount", 0) or 0 for deal in deals_with_amount)
        won_value = sum(
            deal.get("amount", 0) or 0
            for deal in deals_with_amount
            if deal.get("stageId") == "closed-won"
        )
        
        analytics = {
            "timeframe": timeframe,
            "leadsCreated": leads_count,
            "dealsCreated": deals_count,
            "totalDealValue": total_value,
            "wonDealValue": won_value,
            "winRate": round((won_value / total_value * 100) if total_value > 0 else 0, 2)
        }
        
        return format_success_response(
            f"Analytics data for {timeframe}",
            analytics
        )
    except Exception as e:
        return format_error_response(e)

