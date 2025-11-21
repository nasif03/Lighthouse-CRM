"""Helper utilities for MCP tools"""
from typing import Dict, Any, Optional, List
# Import auth using path workaround for hyphenated directory
import sys
from pathlib import Path
backend_dir = Path(__file__).parent.parent
sys.path.insert(0, str(backend_dir))
import importlib.util
spec = importlib.util.spec_from_file_location("mcp_crm_auth", backend_dir / "mcp-crm" / "auth.py")
mcp_crm_auth = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mcp_crm_auth)
get_user_from_token = mcp_crm_auth.get_user_from_token
extract_token_from_context = mcp_crm_auth.extract_token_from_context
from utils.query_filters import get_user_ids, build_user_filter


async def get_user_context(context: Dict) -> Optional[Dict]:
    """
    Extract and validate user context from MCP request context.
    
    Args:
        context: MCP request context
    
    Returns:
        User context dict or None if authentication fails
    """
    token = extract_token_from_context(context)
    
    # If no token and auth is not required, fetch a real user from database
    if not token:
        # Check if auth is required
        import importlib.util
        spec = importlib.util.spec_from_file_location("mcp_crm_config", backend_dir / "mcp-crm" / "config.py")
        mcp_crm_config = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mcp_crm_config)
        
        if not mcp_crm_config.REQUIRE_AUTH:
            # Fetch first real user from database for development
            from config.database import users_collection, organizations_collection
            from bson import ObjectId
            
            user_doc = None
            
            # First priority: Use configured email if provided
            if mcp_crm_config.MCP_DEV_USER_EMAIL:
                user_doc = users_collection.find_one({"email": mcp_crm_config.MCP_DEV_USER_EMAIL})
                if user_doc:
                    # Convert ObjectId to string for _id
                    user_doc["_id"] = str(user_doc["_id"])
                    # Validate and fix orgId
                    org_id_value = user_doc.get("orgId")
                    if isinstance(org_id_value, list) and len(org_id_value) > 0:
                        user_doc["orgId"] = org_id_value[0]  # Use first orgId
                    elif not org_id_value or (isinstance(org_id_value, str) and not org_id_value):
                        # User has no valid orgId, continue to fallback
                        user_doc = None
                    # If orgId is a valid string, keep it as is
            
            # Strategy: Find an organization first, then find a user in that org
            # This ensures we get a user with a valid orgId
            if not user_doc:
                org = organizations_collection.find_one({})
                
                if org:
                    org_id = str(org["_id"])
                    # Find a user that belongs to this organization
                    user_doc = users_collection.find_one({
                        "$or": [
                            {"orgId": org_id},  # String match
                            {"orgId": {"$in": [org_id]}}  # Array contains
                        ]
                    })
                    
                    if user_doc:
                        # Convert ObjectId to string for _id
                        user_doc["_id"] = str(user_doc["_id"])
                        # Ensure orgId is set correctly (use the org we found)
                        org_id_value = user_doc.get("orgId")
                        if isinstance(org_id_value, list):
                            if org_id not in org_id_value:
                                user_doc["orgId"] = [org_id]
                            else:
                                # Use the first orgId from array
                                user_doc["orgId"] = org_id_value[0] if len(org_id_value) == 1 else org_id_value
                        elif org_id_value != org_id:
                            user_doc["orgId"] = org_id
            
            # Fallback: try to find any user with valid orgId (non-empty)
            if not user_doc:
                # Try to find user with non-empty orgId
                all_users = list(users_collection.find({
                    "orgId": {"$exists": True, "$ne": None, "$ne": "", "$ne": []}
                }).limit(10))
                
                for candidate_user in all_users:
                    org_id_value = candidate_user.get("orgId")
                    # Check if orgId is valid
                    if isinstance(org_id_value, list) and len(org_id_value) > 0:
                        user_doc = candidate_user
                        candidate_user["orgId"] = org_id_value[0]  # Use first orgId
                        break
                    elif isinstance(org_id_value, str) and org_id_value:
                        user_doc = candidate_user
                        break
            
            if user_doc:
                # Convert ObjectId to string for _id if not already done
                if isinstance(user_doc["_id"], ObjectId):
                    user_doc["_id"] = str(user_doc["_id"])
                
                # Final validation: ensure orgId is valid
                org_id_value = user_doc.get("orgId")
                if not org_id_value or (isinstance(org_id_value, list) and len(org_id_value) == 0):
                    # Still invalid, use fallback
                    user_doc = None
            
            if user_doc:
                return {
                    "email": user_doc.get("email", "dev@example.com"),
                    "user_doc": user_doc,
                    "decoded_token": {"email": user_doc.get("email", "dev@example.com")}
                }
            else:
                # Fallback if no valid users found
                return {
                    "email": "dev@example.com",
                    "user_doc": {"_id": "dev", "orgId": "dev-org", "email": "dev@example.com"},
                    "decoded_token": {"email": "dev@example.com"}
                }
        return None
    
    # Get user from token (async)
    user_context = await get_user_from_token(token)
    return user_context


def format_error_response(error: Exception) -> str:
    """
    Format an exception as a user-friendly error message.
    
    Args:
        error: Exception object
    
    Returns:
        Formatted error message string
    """
    error_msg = str(error)
    if "not found" in error_msg.lower():
        return f"Resource not found: {error_msg}"
    elif "permission" in error_msg.lower() or "unauthorized" in error_msg.lower():
        return f"Permission denied: {error_msg}"
    elif "validation" in error_msg.lower() or "invalid" in error_msg.lower():
        return f"Invalid input: {error_msg}"
    else:
        return f"Error: {error_msg}"


def format_success_response(message: str, data: Optional[Dict] = None) -> str:
    """
    Format a success response message.
    
    Args:
        message: Success message
        data: Optional data to include
    
    Returns:
        Formatted success message
    """
    if data:
        return f"{message}\n\nDetails: {format_dict(data)}"
    return message


def format_dict(data: Dict, indent: int = 0) -> str:
    """
    Format a dictionary as a readable string.
    
    Args:
        data: Dictionary to format
        indent: Indentation level
    
    Returns:
        Formatted string
    """
    lines = []
    prefix = "  " * indent
    
    for key, value in data.items():
        if isinstance(value, dict):
            lines.append(f"{prefix}{key}:")
            lines.append(format_dict(value, indent + 1))
        elif isinstance(value, list):
            lines.append(f"{prefix}{key}: [{len(value)} items]")
            if value and isinstance(value[0], dict):
                for i, item in enumerate(value[:3]):  # Show first 3 items
                    lines.append(f"{prefix}  [{i}]: {format_dict(item, indent + 2)}")
                if len(value) > 3:
                    lines.append(f"{prefix}  ... and {len(value) - 3} more")
        else:
            lines.append(f"{prefix}{key}: {value}")
    
    return "\n".join(lines)


def validate_user_context(user_context: Optional[Dict]) -> Dict:
    """
    Validate user context and raise error if invalid.
    
    Args:
        user_context: User context from get_user_context()
    
    Returns:
        Validated user context
    
    Raises:
        ValueError: If user context is invalid
    """
    if not user_context:
        raise ValueError("Authentication required. Please provide a valid Firebase token.")
    
    user_doc = user_context.get("user_doc")
    if not user_doc:
        raise ValueError("User not found in database.")
    
    org_id = user_doc.get("orgId")
    if not org_id:
        raise ValueError("User must belong to an organization.")
    
    return user_context


def get_user_ids_from_context(user_context: Dict) -> Dict[str, str]:
    """
    Extract user IDs from user context.
    
    Args:
        user_context: Validated user context
    
    Returns:
        Dictionary with ownerId and orgId
    """
    user_doc = user_context.get("user_doc")
    return get_user_ids(user_doc)


def build_mongo_filter(user_context: Dict, include_owner: bool = True, **additional_filters) -> Dict[str, Any]:
    """
    Build MongoDB filter from user context.
    
    Args:
        user_context: Validated user context
        include_owner: Whether to filter by ownerId
        **additional_filters: Additional filter conditions
    
    Returns:
        MongoDB query filter
    """
    user_doc = user_context.get("user_doc")
    base_filter = build_user_filter(user_doc, include_owner=include_owner)
    
    if additional_filters:
        base_filter.update(additional_filters)
    
    return base_filter


def parse_filters(filters_str: Optional[str]) -> Dict[str, Any]:
    """
    Parse filter string into MongoDB query.
    
    Supports simple key:value format.
    Example: "status:new,source:website"
    
    Args:
        filters_str: Filter string
    
    Returns:
        Dictionary of filter conditions
    """
    if not filters_str:
        return {}
    
    result = {}
    for pair in filters_str.split(","):
        if ":" in pair:
            key, value = pair.split(":", 1)
            key = key.strip()
            value = value.strip()
            
            # Try to parse as number
            try:
                if "." in value:
                    value = float(value)
                else:
                    value = int(value)
            except ValueError:
                pass  # Keep as string
            
            result[key] = value
    
    return result

