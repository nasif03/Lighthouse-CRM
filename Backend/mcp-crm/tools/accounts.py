"""MCP tools for account management"""
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
format_error_response = mcp_crm_utils.format_error_response
format_success_response = mcp_crm_utils.format_success_response
parse_filters = mcp_crm_utils.parse_filters
from config.database import accounts_collection
from services.activity_log import log_account_created
from utils.query_filters import build_user_filter


async def create_account(
    name: str,
    domain: Optional[str] = None,
    industry: Optional[str] = None,
    phone: Optional[str] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Create a new account in the CRM.
    
    Args:
        name: Account/company name
        domain: Website domain (optional)
        industry: Industry type (optional)
        phone: Phone number (optional)
        context: MCP request context (for authentication)
    
    Returns:
        Success message with account ID
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_ids = get_user_ids_from_context(user_context)
        
        owner_id = user_ids["ownerId"]
        org_id = user_ids["orgId"]
        
        now = datetime.utcnow()
        
        account_data = {
            "name": name,
            "domain": domain or "",
            "industry": industry or "",
            "phone": phone or "",
            "status": "active",
            "ownerId": owner_id,
            "orgId": org_id,
            "metadata": None,
            "address": None,
            "deleted": False,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = accounts_collection.insert_one(account_data)
        account_id = str(result.inserted_id)
        
        # Log activity
        log_account_created(org_id, owner_id, account_id, name)
        
        return format_success_response(
            f"Account created successfully: {name}",
            {
                "accountId": account_id,
                "name": name,
                "domain": domain,
                "industry": industry
            }
        )
    except Exception as e:
        return format_error_response(e)


async def get_accounts(
    filters: Optional[str] = None,
    limit: int = 50,
    context: Optional[Dict] = None
) -> str:
    """
    Get accounts from the CRM.
    
    Args:
        filters: Optional filter string (e.g., "industry:Technology")
        limit: Maximum number of accounts to return (default: 50)
        context: MCP request context (for authentication)
    
    Returns:
        Formatted list of accounts
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_doc = user_context.get("user_doc")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            return "User must belong to an organization."
        
        # Build filter
        query_filter = {"orgId": org_id, "deleted": {"$ne": True}}
        
        # Add additional filters
        if filters:
            additional = parse_filters(filters)
            query_filter.update(additional)
        
        # Query accounts
        cursor = accounts_collection.find(
            query_filter,
            {"name": 1, "domain": 1, "industry": 1, "phone": 1,
             "status": 1, "createdAt": 1}
        ).sort("createdAt", -1).limit(limit)
        
        accounts = list(cursor)
        
        if not accounts:
            return "No accounts found matching the criteria."
        
        # Format results
        results = []
        for account in accounts:
            results.append({
                "id": str(account["_id"]),
                "name": account.get("name", ""),
                "domain": account.get("domain"),
                "industry": account.get("industry"),
                "phone": account.get("phone"),
                "status": account.get("status", "active"),
                "createdAt": account.get("createdAt").isoformat() if account.get("createdAt") else ""
            })
        
        return format_success_response(
            f"Found {len(results)} account(s)",
            {"accounts": results}
        )
    except Exception as e:
        return format_error_response(e)


async def update_account(
    account_id: str,
    name: Optional[str] = None,
    domain: Optional[str] = None,
    industry: Optional[str] = None,
    phone: Optional[str] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Update an account.
    
    Args:
        account_id: ID of the account to update
        name: New account name (optional)
        domain: New domain (optional)
        industry: New industry (optional)
        phone: New phone (optional)
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
        
        # Check if account exists
        account = accounts_collection.find_one({"_id": ObjectId(account_id), "orgId": org_id})
        if not account:
            return "Account not found or you don't have permission to update it."
        
        # Build update
        update_data = {"updatedAt": datetime.utcnow()}
        
        if name:
            update_data["name"] = name
        if domain is not None:
            update_data["domain"] = domain
        if industry is not None:
            update_data["industry"] = industry
        if phone is not None:
            update_data["phone"] = phone
        
        # Update account
        accounts_collection.update_one(
            {"_id": ObjectId(account_id)},
            {"$set": update_data}
        )
        
        return format_success_response(
            f"Account updated successfully",
            {"accountId": account_id, "updates": update_data}
        )
    except Exception as e:
        return format_error_response(e)

