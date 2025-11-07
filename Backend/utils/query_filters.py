"""Reusable query filter utilities for data isolation"""
from typing import Dict, Any, Optional


def build_user_filter(user_doc: dict, include_owner: bool = True, active_org_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Build a MongoDB query filter for data isolation.
    
    Ensures users can only access data from:
    1. Their current organization (orgId) - tenant isolation
    2. Their own data (ownerId) - user isolation (if include_owner=True)
    
    Args:
        user_doc: The user document from the database
        include_owner: If True, filter by ownerId. If False, only filter by orgId.
                     Set to False for admin/manager views that need to see all org data.
        active_org_id: Optional active organization ID. If provided, uses this.
                      Otherwise, uses the first orgId from the user's orgId array/string.
    
    Returns:
        A MongoDB query filter dictionary
    
    Example:
        filter = build_user_filter(user_doc, include_owner=True)
        leads = collection.find(filter)
    """
    if not user_doc:
        raise ValueError("user_doc is required")
    
    org_id_value = user_doc.get("orgId")
    if not org_id_value:
        raise ValueError("User must belong to an organization")
    
    # Check for activeOrgId in user document first (set when switching tenants)
    stored_active_org_id = user_doc.get("activeOrgId")
    
    # Use active_org_id parameter if provided, otherwise use stored activeOrgId, otherwise use first org
    target_org_id = active_org_id or stored_active_org_id
    
    if target_org_id:
        # Verify user belongs to this org
        if isinstance(org_id_value, list):
            if target_org_id not in org_id_value:
                raise ValueError("User does not belong to the specified organization")
        elif isinstance(org_id_value, str):
            if target_org_id != org_id_value:
                raise ValueError("User does not belong to the specified organization")
        org_id = target_org_id
    else:
        # Use first orgId from array, or the string value
        if isinstance(org_id_value, list):
            if not org_id_value:
                raise ValueError("User must belong to an organization")
            org_id = org_id_value[0]
        else:
            org_id = org_id_value
    
    filter_dict: Dict[str, Any] = {"orgId": org_id}
    
    if include_owner:
        owner_id = str(user_doc["_id"])
        filter_dict["ownerId"] = owner_id
    
    return filter_dict


def build_user_filter_with_conditions(
    user_doc: dict, 
    additional_conditions: Optional[Dict[str, Any]] = None,
    include_owner: bool = True,
    active_org_id: Optional[str] = None
) -> Dict[str, Any]:
    """
    Build a MongoDB query filter with additional conditions.
    
    Combines user isolation filters with custom conditions.
    
    Args:
        user_doc: The user document from the database
        additional_conditions: Additional MongoDB query conditions to merge
        include_owner: If True, filter by ownerId. If False, only filter by orgId.
        active_org_id: Optional active organization ID. If provided, uses this.
    
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
    base_filter = build_user_filter(user_doc, include_owner=include_owner, active_org_id=active_org_id)
    
    if additional_conditions:
        base_filter.update(additional_conditions)
    
    return base_filter


def get_user_ids(user_doc: dict, active_org_id: Optional[str] = None) -> Dict[str, str]:
    """
    Extract user IDs from user document.
    
    Args:
        user_doc: The user document from the database
        active_org_id: Optional active organization ID. If provided, uses this.
                      Otherwise, uses the first orgId from the user's orgId array/string.
    
    Returns:
        Dictionary with 'ownerId' and 'orgId' keys
    
    Example:
        ids = get_user_ids(user_doc)
        lead_data = {**lead_data, **ids}
    """
    if not user_doc:
        raise ValueError("user_doc is required")
    
    owner_id = str(user_doc["_id"])
    org_id_value = user_doc.get("orgId")
    
    if not org_id_value:
        raise ValueError("User must belong to an organization")
    
    # Check for activeOrgId in user document first (set when switching tenants)
    stored_active_org_id = user_doc.get("activeOrgId")
    
    # Use active_org_id parameter if provided, otherwise use stored activeOrgId, otherwise use first org
    target_org_id = active_org_id or stored_active_org_id
    
    if target_org_id:
        # Verify user belongs to this org
        if isinstance(org_id_value, list):
            if target_org_id not in org_id_value:
                raise ValueError("User does not belong to the specified organization")
        elif isinstance(org_id_value, str):
            if target_org_id != org_id_value:
                raise ValueError("User does not belong to the specified organization")
        org_id = target_org_id
    else:
        # Use first orgId from array, or the string value
        if isinstance(org_id_value, list):
            if not org_id_value:
                raise ValueError("User must belong to an organization")
            org_id = org_id_value[0]
        else:
            org_id = org_id_value
    
    return {
        "ownerId": owner_id,
        "orgId": org_id
    }

