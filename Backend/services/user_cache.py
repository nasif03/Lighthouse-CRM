"""User caching service"""
from typing import Dict, Optional
from datetime import datetime, timedelta
from config.settings import USER_CACHE_TTL_MINUTES

_user_cache: Dict[str, tuple] = {}  # token -> (user_doc, expiry_time)
CACHE_TTL = timedelta(minutes=USER_CACHE_TTL_MINUTES)

def get_cached_user(token: str) -> Optional[dict]:
    """Get cached user data if still valid"""
    if token in _user_cache:
        user_doc, expiry = _user_cache[token]
        if datetime.utcnow() < expiry:
            return user_doc
        else:
            del _user_cache[token]
    return None

def cache_user(token: str, user_doc: dict):
    """Cache user data"""
    _user_cache[token] = (user_doc, datetime.utcnow() + CACHE_TTL)

def clear_user_cache(token: str = None):
    """Clear user cache for a specific token or all tokens"""
    if token:
        _user_cache.pop(token, None)
    else:
        _user_cache.clear()

