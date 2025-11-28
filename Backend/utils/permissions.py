"""Permission checking utilities"""
from bson import ObjectId
from config.database import organizations_collection, roles_collection
from config.settings import SUPER_ADMIN_EMAIL


def is_super_admin(user_doc: dict) -> bool:
    """
    Check if user is a super admin (CRM seller).
    
    Args:
        user_doc: User document from database
    
    Returns:
        True if user is super admin, False otherwise
    """
    email = user_doc.get("email", "").lower()
    return email == SUPER_ADMIN_EMAIL.lower()


def has_permission(user_doc: dict, org_id: str, required_permissions: list[str]) -> bool:
    """
    Check if user has any of the required permissions.
    
    Args:
        user_doc: User document from database
        org_id: Organization ID to check permissions for
        required_permissions: List of permission strings to check (e.g., ["read:leads", "write:leads"])
    
    Returns:
        True if user has at least one of the required permissions, False otherwise
    """
    # Super admin bypasses all permission checks
    if is_super_admin(user_doc):
        return True
    
    user_id = str(user_doc["_id"])
    
    # Check if user is admin of the organization - admins have all permissions
    org = organizations_collection.find_one({"_id": ObjectId(org_id)})
    if org and user_id in org.get("admins", []):
        return True
    
    # Check if user belongs to this organization
    user_org_ids = user_doc.get("orgId", [])
    if isinstance(user_org_ids, str):
        user_org_ids = [user_org_ids]
    
    if org_id not in user_org_ids:
        return False
    
    # Check if user has roles with required permissions
    role_ids = user_doc.get("roleIds", [])
    if not role_ids:
        return False
    
    # Check if any of the user's roles have the required permissions
    roles = list(roles_collection.find({
        "_id": {"$in": [ObjectId(rid) for rid in role_ids if ObjectId.is_valid(rid)]},
        "orgId": org_id
    }))
    
    for role in roles:
        permissions = role.get("permissions", [])
        if any(perm in permissions for perm in required_permissions):
            return True
    
    return False


def has_lead_permission(user_doc: dict, org_id: str) -> bool:
    """Check if user has lead-related permissions"""
    return has_permission(user_doc, org_id, ["read:leads", "write:leads", "admin:leads"])


def has_contact_permission(user_doc: dict, org_id: str) -> bool:
    """Check if user has contact-related permissions"""
    return has_permission(user_doc, org_id, ["read:contacts", "write:contacts", "admin:contacts"])


def has_deal_permission(user_doc: dict, org_id: str) -> bool:
    """Check if user has deal-related permissions"""
    return has_permission(user_doc, org_id, ["read:deals", "write:deals", "admin:deals"])


def has_account_permission(user_doc: dict, org_id: str) -> bool:
    """Check if user has account-related permissions"""
    return has_permission(user_doc, org_id, ["read:accounts", "write:accounts", "admin:accounts"])

