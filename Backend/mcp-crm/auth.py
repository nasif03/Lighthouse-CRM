"""Authentication for MCP - reuses FastAPI auth logic"""
from typing import Optional, Dict
from services.auth import verify_firebase_token
from services.user_cache import get_cached_auth, cache_auth
from config.database import users_collection
# Import config using path workaround for hyphenated directory
import sys
from pathlib import Path
backend_dir = Path(__file__).parent.parent
sys.path.insert(0, str(backend_dir))
import importlib.util
spec = importlib.util.spec_from_file_location("mcp_crm_config", backend_dir / "mcp-crm" / "config.py")
mcp_crm_config = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mcp_crm_config)
REQUIRE_AUTH = mcp_crm_config.REQUIRE_AUTH


async def get_user_from_token(token: Optional[str]) -> Optional[Dict]:
    """
    Get user context from Firebase token - reuses FastAPI authentication logic.
    
    Args:
        token: Firebase ID token (optional if auth is disabled)
    
    Returns:
        User context dict with email, user_doc, decoded_token, or None if auth fails
    """
    if not REQUIRE_AUTH:
        # Return a default user context for development
        return {
            "email": "dev@example.com",
            "user_doc": {"_id": "dev", "orgId": "dev-org", "email": "dev@example.com"},
            "decoded_token": {"email": "dev@example.com"}
        }
    
    if not token:
        return None
    
    try:
        # Check cache first (same as FastAPI)
        cached = get_cached_auth(token)
        if cached:
            decoded_token, user_doc = cached
            return {
                "email": decoded_token.get("email"),
                "user_doc": user_doc,
                "decoded_token": decoded_token
            }
        
        # Verify token
        decoded_token = await verify_firebase_token(token)
        email = decoded_token.get("email")
        
        if not email:
            return None
        
        # Get user from database
        user_doc = users_collection.find_one({"email": email})
        
        if not user_doc:
            return None
        
        # Cache the result
        cache_auth(token, decoded_token, user_doc)
        
        return {
            "email": email,
            "user_doc": user_doc,
            "decoded_token": decoded_token
        }
    except Exception as e:
        print(f"Error authenticating user for MCP: {str(e)}")
        return None


def extract_token_from_context(context: Dict) -> Optional[str]:
    """
    Extract authentication token from MCP request context.
    
    MCP requests may include auth in:
    - context.metadata.headers.authorization (Bearer token)
    - context.metadata.token (direct token)
    
    Args:
        context: MCP request context
    
    Returns:
        Token string or None
    """
    try:
        metadata = context.get("metadata", {})
        
        # Try headers first
        headers = metadata.get("headers", {})
        auth_header = headers.get("authorization") or headers.get("Authorization")
        if auth_header and auth_header.startswith("Bearer "):
            return auth_header[7:]  # Remove "Bearer " prefix
        
        # Try direct token
        token = metadata.get("token")
        if token:
            return token
        
        return None
    except Exception:
        return None

