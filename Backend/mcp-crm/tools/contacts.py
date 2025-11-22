"""MCP tools for contact management"""
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
from config.database import contacts_collection
from services.activity_log import log_contact_created
from utils.query_filters import build_user_filter


async def create_contact(
    first_name: str,
    last_name: str,
    email: str,
    account_id: Optional[str] = None,
    phone: Optional[str] = None,
    title: Optional[str] = None,
    tags: Optional[list] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Create a new contact in the CRM.
    
    Args:
        first_name: First name
        last_name: Last name
        email: Email address
        account_id: Associated account ID (optional)
        phone: Phone number (optional)
        title: Job title (optional)
        tags: List of tags (optional)
        context: MCP request context (for authentication)
    
    Returns:
        Success message with contact ID
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        user_ids = get_user_ids_from_context(user_context)
        
        owner_id = user_ids["ownerId"]
        org_id = user_ids["orgId"]
        
        now = datetime.utcnow()
        
        contact_data = {
            "firstName": first_name,
            "lastName": last_name or "",
            "email": email,
            "phone": phone or "",
            "title": title or "",
            "accountId": ObjectId(account_id) if account_id else None,
            "ownerId": owner_id,
            "orgId": org_id,
            "tags": tags or [],
            "metadata": None,
            "deleted": False,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = contacts_collection.insert_one(contact_data)
        contact_id = str(result.inserted_id)
        
        # Log activity
        log_contact_created(org_id, owner_id, contact_id, f"{first_name} {last_name}")
        
        return format_success_response(
            f"Contact created successfully: {first_name} {last_name} ({email})",
            {
                "contactId": contact_id,
                "name": f"{first_name} {last_name}",
                "email": email
            }
        )
    except Exception as e:
        return format_error_response(e)


async def get_contacts(
    filters: Optional[str] = None,
    limit: int = 50,
    context: Optional[Dict] = None
) -> str:
    """
    Get contacts from the CRM.
    
    Args:
        filters: Optional filter string (e.g., "title:Manager")
        limit: Maximum number of contacts to return (default: 50)
        context: MCP request context (for authentication)
    
    Returns:
        Formatted list of contacts
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
        
        # Query contacts
        cursor = contacts_collection.find(
            query_filter,
            {"firstName": 1, "lastName": 1, "email": 1, "phone": 1,
             "title": 1, "accountId": 1, "tags": 1, "createdAt": 1}
        ).sort("createdAt", -1).limit(limit)
        
        contacts = list(cursor)
        
        if not contacts:
            return "No contacts found matching the criteria."
        
        # Format results
        results = []
        for contact in contacts:
            results.append({
                "id": str(contact["_id"]),
                "name": f"{contact.get('firstName', '')} {contact.get('lastName', '')}".strip(),
                "email": contact.get("email", ""),
                "phone": contact.get("phone"),
                "title": contact.get("title"),
                "accountId": str(contact["accountId"]) if contact.get("accountId") else None,
                "tags": contact.get("tags", []),
                "createdAt": contact.get("createdAt").isoformat() if contact.get("createdAt") else ""
            })
        
        return format_success_response(
            f"Found {len(results)} contact(s)",
            {"contacts": results}
        )
    except Exception as e:
        return format_error_response(e)


async def update_contact(
    contact_id: str,
    first_name: Optional[str] = None,
    last_name: Optional[str] = None,
    email: Optional[str] = None,
    phone: Optional[str] = None,
    title: Optional[str] = None,
    context: Optional[Dict] = None
) -> str:
    """
    Update a contact.
    
    Args:
        contact_id: ID of the contact to update
        first_name: New first name (optional)
        last_name: New last name (optional)
        email: New email (optional)
        phone: New phone (optional)
        title: New title (optional)
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
        
        # Check if contact exists
        contact = contacts_collection.find_one({"_id": ObjectId(contact_id), "orgId": org_id})
        if not contact:
            return "Contact not found or you don't have permission to update it."
        
        # Build update
        update_data = {"updatedAt": datetime.utcnow()}
        
        if first_name:
            update_data["firstName"] = first_name
        if last_name is not None:
            update_data["lastName"] = last_name
        if email:
            update_data["email"] = email
        if phone is not None:
            update_data["phone"] = phone
        if title is not None:
            update_data["title"] = title
        
        # Update contact
        contacts_collection.update_one(
            {"_id": ObjectId(contact_id)},
            {"$set": update_data}
        )
        
        return format_success_response(
            f"Contact updated successfully",
            {"contactId": contact_id, "updates": update_data}
        )
    except Exception as e:
        return format_error_response(e)

