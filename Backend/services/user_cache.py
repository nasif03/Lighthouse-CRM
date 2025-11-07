"""User caching service - Caches both token verification and user data"""
from typing import Dict, Optional, Tuple
from datetime import datetime, timedelta
from config.settings import USER_CACHE_TTL_MINUTES

# Cache structure: token -> (decoded_token, user_doc, expiry_time)
_auth_cache: Dict[str, Tuple[dict, dict, datetime]] = {}
CACHE_TTL = timedelta(minutes=USER_CACHE_TTL_MINUTES)

def get_cached_auth(token: str) -> Optional[Tuple[dict, dict]]:
    """Get cached authentication data (decoded_token, user_doc) if still valid"""
    if token in _auth_cache:
        decoded_token, user_doc, expiry = _auth_cache[token]
        if datetime.utcnow() < expiry:
            return (decoded_token, user_doc)
        else:
            del _auth_cache[token]
    return None

def cache_auth(token: str, decoded_token: dict, user_doc: dict):
    """Cache both token verification and user data together"""
    _auth_cache[token] = (decoded_token, user_doc, datetime.utcnow() + CACHE_TTL)

def get_cached_user(token: str) -> Optional[dict]:
    """Get cached user data if still valid (backward compatibility)"""
    cached = get_cached_auth(token)
    if cached:
        return cached[1]  # Return user_doc
    return None

def cache_user(token: str, user_doc: dict):
    """Cache user data (backward compatibility - requires decoded_token to be cached separately)"""
    # This is a fallback - prefer using cache_auth instead
    if token in _auth_cache:
        decoded_token, _, expiry = _auth_cache[token]
        _auth_cache[token] = (decoded_token, user_doc, expiry)

def clear_user_cache(token: str = None):
    """Clear user cache for a specific token or all tokens"""
    if token:
        _auth_cache.pop(token, None)
    else:
        _auth_cache.clear()

