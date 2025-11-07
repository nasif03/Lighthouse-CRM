"""Reusable query filter utilities for data isolation"""
from typing import Dict, Any, Optional


def build_user_filter(user_doc: dict, include_owner: bool = True) -> Dict[str, Any]:
    """
    Build a MongoDB query filter for data isolation.
    
    Ensures users can only access data from:
    1. Their current organization (orgId) - tenant isolation
    2. Their own data (ownerId) - user isolation (if include_owner=True)
    
    Args:
        user_doc: The user document from the database
        include_owner: If True, filter by ownerId. If False, only filter by orgId.
                     Set to False for admin/manager views that need to see all org data.
    
    Returns:
        A MongoDB query filter dictionary
    
    Example:
        filter = build_user_filter(user_doc, include_owner=True)
        leads = collection.find(filter)
    """
    if not user_doc:
        raise ValueError("user_doc is required")
    
    org_id = user_doc.get("orgId")
    if not org_id:
        raise ValueError("User must belong to an organization")
    
    filter_dict: Dict[str, Any] = {"orgId": org_id}
    
    if include_owner:
        owner_id = str(user_doc["_id"])
        filter_dict["ownerId"] = owner_id
    
    return filter_dict


def build_user_filter_with_conditions(
    user_doc: dict, 
    additional_conditions: Optional[Dict[str, Any]] = None,
    include_owner: bool = True
) -> Dict[str, Any]:
    """
    Build a MongoDB query filter with additional conditions.
    
    Combines user isolation filters with custom conditions.
    
    Args:
        user_doc: The user document from the database
        additional_conditions: Additional MongoDB query conditions to merge
        include_owner: If True, filter by ownerId. If False, only filter by orgId.
    
    Returns:
        A MongoDB query filter dictionary with user isolation + custom conditions
    
    Example:
        filter = build_user_filter_with_conditions(
            user_doc, 
            {"status": "active", "deleted": False},
            include_owner=True
        )
        accounts = collection.find(filter)
    """
    base_filter = build_user_filter(user_doc, include_owner=include_owner)
    
    if additional_conditions:
        base_filter.update(additional_conditions)
    
    return base_filter


def get_user_ids(user_doc: dict) -> Dict[str, str]:
    """
    Extract user IDs from user document.
    
    Returns:
        Dictionary with 'ownerId' and 'orgId' keys
    
    Example:
        ids = get_user_ids(user_doc)
        lead_data = {**lead_data, **ids}
    """
    if not user_doc:
        raise ValueError("user_doc is required")
    
    owner_id = str(user_doc["_id"])
    org_id = user_doc.get("orgId")
    
    if not org_id:
        raise ValueError("User must belong to an organization")
    
    return {
        "ownerId": owner_id,
        "orgId": org_id
    }

