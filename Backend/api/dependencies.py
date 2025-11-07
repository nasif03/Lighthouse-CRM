"""FastAPI dependencies"""
from fastapi import HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from services.auth import verify_firebase_token
from services.user_cache import get_cached_auth, cache_auth
from config.database import users_collection
from utils.performance import time_operation

# Security scheme
security = HTTPBearer()

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> dict:
    """Get current authenticated user from token - optimized with full auth caching"""
    token = credentials.credentials
    try:
        # Check cache FIRST (includes both token verification and user data)
        with time_operation("Auth: Check full auth cache", threshold_ms=1.0):
            cached = get_cached_auth(token)
        
        if cached:
            decoded_token, user_doc = cached
            return {"email": decoded_token.get("email"), "user_doc": user_doc, "decoded_token": decoded_token}
        
        # Cache miss - verify token and fetch user
        with time_operation("Auth: Firebase token verification", threshold_ms=200.0):
            decoded_token = await verify_firebase_token(token)
        
        email = decoded_token.get("email")
        
        if not email:
            raise HTTPException(status_code=401, detail="Invalid authentication token")
        
        # Query database
        with time_operation("Auth: DB query for user", threshold_ms=50.0):
            user_doc = users_collection.find_one({"email": email})
        
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        # Cache both token verification AND user data together
        cache_auth(token, decoded_token, user_doc)
        
        return {"email": email, "user_doc": user_doc, "decoded_token": decoded_token}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=401, detail="Invalid authentication token")

