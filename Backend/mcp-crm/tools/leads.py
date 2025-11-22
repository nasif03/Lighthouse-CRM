"""MCP tools for lead management"""
import sys
from pathlib import Path
from typing import Any, Dict, Optional
from datetime import datetime
from bson import ObjectId

# Add parent directories to path and import utils
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
from config.database import leads_collection, accounts_collection, contacts_collection, deals_collection
from services.activity_log import log_lead_created, log_lead_converted
from utils.query_filters import build_user_filter


async def create_lead(
    name: str,
    email: str,
    source: str = "manual",
    status: str = "new",
    phone: Optional[str] = None,
    firstName: Optional[str] = None,
    lastName: Optional[str] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Create a new lead in the CRM.
    
    Args:
        name: Full name of the lead
        email: Email address
        source: Lead source (e.g., "website", "linkedin", "referral")
        status: Lead status (new, contacted, qualified, converted, lost)
        phone: Phone number (optional)
        firstName: First name (optional, will be parsed from name if not provided)
        lastName: Last name (optional, will be parsed from name if not provided)
        context: MCP request context (for authentication)
    
    Returns:
        Success message with lead ID
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_ids = get_user_ids_from_context(user_context)
        user_doc = user_context.get("user_doc")
        
        owner_id = user_ids["ownerId"]
        org_id = user_ids["orgId"]
        
        now = datetime.utcnow()
        
        # Parse name into firstName and lastName if not provided
        name_parts = name.strip().split(" ", 1)
        first_name = firstName or name_parts[0] if name_parts else ""
        last_name = lastName or (name_parts[1] if len(name_parts) > 1 else "")
        
        lead_data = {
            "accountId": None,
            "contactId": None,
            "name": name,
            "firstName": first_name,
            "lastName": last_name,
            "email": email,
            "phone": phone or "",
            "source": source,
            "ownerId": owner_id,
            "orgId": org_id,
            "status": status,
            "tags": [],
            "converted": False,
            "metadata": None,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = leads_collection.insert_one(lead_data)
        lead_id = str(result.inserted_id)
        
        # Log activity
        log_lead_created(org_id, owner_id, lead_id, name)
        
        return format_success_response(
            f"Lead created successfully: {name} ({email})",
            {"leadId": lead_id, "name": name, "email": email, "status": status}
        )
    except Exception as e:
        return format_error_response(e)


async def get_leads(
    filters: Optional[str] = None,
    limit: int = 50,
    context: Optional[Dict] = None
) -> str:
    """
    Get leads from the CRM.
    
    Args:
        filters: Optional filter string (e.g., "status:new,source:website")
        limit: Maximum number of leads to return (default: 50)
        context: MCP request context (for authentication)
    
    Returns:
        Formatted list of leads
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        # Build filter
        query_filter = build_user_filter(user_doc, include_owner=True)
        
        # Add additional filters
        if filters:
            additional = parse_filters(filters)
            query_filter.update(additional)
        
        # Query leads
        cursor = leads_collection.find(
            query_filter,
            {"name": 1, "email": 1, "source": 1, "status": 1, "phone": 1,
             "firstName": 1, "lastName": 1, "tags": 1, "createdAt": 1}
        ).sort("createdAt", -1).limit(limit)
        
        leads = list(cursor)
        
        if not leads:
            return "No leads found matching the criteria."
        
        # Format results
        results = []
        for lead in leads:
            results.append({
                "id": str(lead["_id"]),
                "name": lead.get("name", ""),
                "email": lead.get("email", ""),
                "source": lead.get("source", ""),
                "status": lead.get("status", "new"),
                "phone": lead.get("phone"),
                "createdAt": lead.get("createdAt").isoformat() if lead.get("createdAt") else ""
            })
        
        return format_success_response(
            f"Found {len(results)} lead(s)",
            {"leads": results}
        )
    except Exception as e:
        return format_error_response(e)


async def update_lead_status(
    lead_id: str,
    status: str,
    context: Optional[Dict] = None
) -> str:
    """
    Update the status of a lead.
    
    Args:
        lead_id: ID of the lead to update
        status: New status (new, contacted, qualified, converted, lost)
        context: MCP request context (for authentication)
    
    Returns:
        Success message
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        # Validate status
        valid_statuses = ["new", "contacted", "qualified", "converted", "lost"]
        if status not in valid_statuses:
            return f"Invalid status. Must be one of: {', '.join(valid_statuses)}"
        
        # Build filter to ensure user can only update their own leads
        query_filter = build_user_filter(user_doc, include_owner=True)
        query_filter["_id"] = ObjectId(lead_id)
        
        # Check if lead exists
        lead = leads_collection.find_one(query_filter)
        if not lead:
            return "Lead not found or you don't have permission to update it."
        
        # Update status
        now = datetime.utcnow()
        leads_collection.update_one(
            query_filter,
            {"$set": {"status": status, "updatedAt": now}}
        )
        
        return format_success_response(
            f"Lead status updated to '{status}'",
            {"leadId": lead_id, "name": lead.get("name"), "newStatus": status}
        )
    except Exception as e:
        return format_error_response(e)


async def convert_lead(
    lead_id: str,
    context: Optional[Dict] = None
) -> str:
    """
    Convert a lead to Account, Contact, and Deal.
    
    Args:
        lead_id: ID of the lead to convert
        context: MCP request context (for authentication)
    
    Returns:
        Success message with created IDs
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_ids = get_user_ids_from_context(user_context)
        user_doc = user_context.get("user_doc")
        
        owner_id = user_ids["ownerId"]
        org_id = user_ids["orgId"]
        
        # Build filter to ensure user can only convert their own leads
        query_filter = build_user_filter(user_doc, include_owner=True)
        query_filter["_id"] = ObjectId(lead_id)
        
        # Fetch the lead
        lead = leads_collection.find_one(query_filter)
        if not lead:
            return "Lead not found or you don't have permission to convert it."
        
        if lead.get("converted"):
            return "Lead has already been converted."
        
        now = datetime.utcnow()
        
        # Create Account
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
        
        # Create Contact
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
        
        # Create Deal
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
        
        # Log conversion
        log_lead_converted(
            org_id=org_id,
            user_id=owner_id,
            lead_id=lead_id,
            lead_name=lead.get("name", "Untitled Lead"),
            account_id=account_id,
            contact_id=contact_id,
            deal_id=deal_id
        )
        
        return format_success_response(
            f"Lead '{lead.get('name')}' converted successfully",
            {
                "leadId": lead_id,
                "accountId": account_id,
                "contactId": contact_id,
                "dealId": deal_id
            }
        )
    except Exception as e:
        return format_error_response(e)

