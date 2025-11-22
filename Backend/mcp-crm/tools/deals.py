"""MCP tools for deal management"""
from typing import Any, Dict, Optional
from datetime import datetime
from bson import ObjectId
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
get_user_ids_from_context = mcp_crm_utils.get_user_ids_from_context
build_mongo_filter = mcp_crm_utils.build_mongo_filter
format_error_response = mcp_crm_utils.format_error_response
format_success_response = mcp_crm_utils.format_success_response
parse_filters = mcp_crm_utils.parse_filters
from config.database import deals_collection
from services.activity_log import log_deal_created, log_deal_stage_changed
from utils.query_filters import build_user_filter


async def create_deal(
    name: str,
    account_id: Optional[str] = None,
    contact_id: Optional[str] = None,
    amount: Optional[float] = None,
    stage: str = "prospecting",
    currency: str = "USD",
    probability: Optional[int] = None,
    close_date: Optional[str] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Create a new deal in the CRM.
    
    Args:
        name: Deal name
        account_id: Associated account ID (optional)
        contact_id: Associated contact ID (optional)
        amount: Deal amount (optional)
        stage: Deal stage (prospecting, qualification, proposal, negotiation, closed-won, closed-lost)
        currency: Currency code (default: USD)
        probability: Win probability percentage (0-100, optional)
        close_date: Expected close date in ISO format (optional)
        context: MCP request context (for authentication)
    
    Returns:
        Success message with deal ID
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_ids = get_user_ids_from_context(user_context)
        
        owner_id = user_ids["ownerId"]
        org_id = user_ids["orgId"]
        
        now = datetime.utcnow()
        
        # Parse close date
        close_date_obj = None
        if close_date:
            try:
                close_date_obj = datetime.fromisoformat(close_date.replace('Z', '+00:00'))
            except:
                pass
        
        # Stage name mapping
        stage_names = {
            "prospecting": "Prospecting",
            "qualification": "Qualification",
            "proposal": "Proposal",
            "negotiation": "Negotiation",
            "closed-won": "Closed Won",
            "closed-lost": "Closed Lost"
        }
        
        deal_data = {
            "name": name,
            "accountId": ObjectId(account_id) if account_id else None,
            "contactId": ObjectId(contact_id) if contact_id else None,
            "amount": amount,
            "currency": currency,
            "stageId": stage,
            "stageName": stage_names.get(stage, stage.title()),
            "probability": probability,
            "closeDate": close_date_obj,
            "status": "open",
            "ownerId": owner_id,
            "orgId": org_id,
            "tags": [],
            "lastActivityAt": now,
            "metadata": None,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = deals_collection.insert_one(deal_data)
        deal_id = str(result.inserted_id)
        
        # Log activity
        log_deal_created(org_id, owner_id, deal_id, name)
        
        return format_success_response(
            f"Deal created successfully: {name}",
            {
                "dealId": deal_id,
                "name": name,
                "amount": amount,
                "stage": stage,
                "currency": currency
            }
        )
    except Exception as e:
        return format_error_response(e)


async def get_deals(
    filters: Optional[str] = None,
    limit: int = 50,
    context: Optional[Dict] = None
) -> str:
    """
    Get deals from the CRM.
    
    Args:
        filters: Optional filter string (e.g., "stageId:prospecting,status:open")
        limit: Maximum number of deals to return (default: 50)
        context: MCP request context (for authentication)
    
    Returns:
        Formatted list of deals
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            return "User must belong to an organization."
        
        # Build filter
        query_filter = {"orgId": org_id}
        
        # Add additional filters
        if filters:
            additional = parse_filters(filters)
            query_filter.update(additional)
        
        # Query deals
        cursor = deals_collection.find(
            query_filter,
            {"name": 1, "accountId": 1, "contactId": 1, "amount": 1, "currency": 1,
             "stageId": 1, "stageName": 1, "probability": 1, "closeDate": 1,
             "status": 1, "createdAt": 1}
        ).sort("createdAt", -1).limit(limit)
        
        deals = list(cursor)
        
        if not deals:
            return "No deals found matching the criteria."
        
        # Format results
        results = []
        for deal in deals:
            results.append({
                "id": str(deal["_id"]),
                "name": deal.get("name", ""),
                "amount": deal.get("amount"),
                "currency": deal.get("currency", "USD"),
                "stageId": deal.get("stageId", ""),
                "stageName": deal.get("stageName", ""),
                "probability": deal.get("probability"),
                "status": deal.get("status", "open"),
                "closeDate": deal.get("closeDate").isoformat() if deal.get("closeDate") else None,
                "createdAt": deal.get("createdAt").isoformat() if deal.get("createdAt") else ""
            })
        
        return format_success_response(
            f"Found {len(results)} deal(s)",
            {"deals": results}
        )
    except Exception as e:
        return format_error_response(e)


async def update_deal(
    deal_id: str,
    name: Optional[str] = None,
    amount: Optional[float] = None,
    stage: Optional[str] = None,
    probability: Optional[int] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Update a deal.
    
    Args:
        deal_id: ID of the deal to update
        name: New deal name (optional)
        amount: New deal amount (optional)
        stage: New deal stage (optional)
        probability: New win probability (optional)
        context: MCP request context (for authentication)
    
    Returns:
        Success message
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            return "User must belong to an organization."
        
        # Check if deal exists and belongs to org
        deal = deals_collection.find_one({"_id": ObjectId(deal_id), "orgId": org_id})
        if not deal:
            return "Deal not found or you don't have permission to update it."
        
        # Build update
        update_data = {"updatedAt": datetime.utcnow()}
        
        if name:
            update_data["name"] = name
        if amount is not None:
            update_data["amount"] = amount
        if stage:
            stage_names = {
                "prospecting": "Prospecting",
                "qualification": "Qualification",
                "proposal": "Proposal",
                "negotiation": "Negotiation",
                "closed-won": "Closed Won",
                "closed-lost": "Closed Lost"
            }
            update_data["stageId"] = stage
            update_data["stageName"] = stage_names.get(stage, stage.title())
            
            # Log stage change
            if deal.get("stageId") != stage:
                log_deal_stage_changed(
                    org_id, str(user_doc["_id"]), deal_id,
                    deal.get("stageId", ""), stage, deal.get("name", "")
                )
        if probability is not None:
            update_data["probability"] = probability
        
        # Update deal
        deals_collection.update_one(
            {"_id": ObjectId(deal_id)},
            {"$set": update_data}
        )
        
        return format_success_response(
            f"Deal updated successfully",
            {"dealId": deal_id, "updates": update_data}
        )
    except Exception as e:
        return format_error_response(e)


async def update_deal_stage(
    deal_id: str,
    stage: str,
    context: Optional[Dict] = None
) -> str:
    """
    Update the stage of a deal (moves it through the sales pipeline).
    
    Args:
        deal_id: ID of the deal to update
        stage: New stage (prospecting, qualification, proposal, negotiation, closed-won, closed-lost)
        context: MCP request context (for authentication)
    
    Returns:
        Success message
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            return "User must belong to an organization."
        
        # Check if deal exists
        deal = deals_collection.find_one({"_id": ObjectId(deal_id), "orgId": org_id})
        if not deal:
            return "Deal not found or you don't have permission to update it."
        
        # Stage name mapping
        stage_names = {
            "prospecting": "Prospecting",
            "qualification": "Qualification",
            "proposal": "Proposal",
            "negotiation": "Negotiation",
            "closed-won": "Closed Won",
            "closed-lost": "Closed Lost"
        }
        
        old_stage = deal.get("stageId", "")
        
        # Update stage
        deals_collection.update_one(
            {"_id": ObjectId(deal_id)},
            {
                "$set": {
                    "stageId": stage,
                    "stageName": stage_names.get(stage, stage.title()),
                    "updatedAt": datetime.utcnow()
                }
            }
        )
        
        # Log stage change
        if old_stage != stage:
            log_deal_stage_changed(
                org_id, str(user_doc["_id"]), deal_id,
                old_stage, stage, deal.get("name", "")
            )
        
        return format_success_response(
            f"Deal stage updated from '{old_stage}' to '{stage}'",
            {"dealId": deal_id, "oldStage": old_stage, "newStage": stage}
        )
    except Exception as e:
        return format_error_response(e)

