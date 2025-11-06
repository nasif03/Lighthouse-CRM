"""FastAPI dependencies"""
from fastapi import HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from services.auth import verify_firebase_token
from services.user_cache import get_cached_user, cache_user
from config.database import users_collection

# Security scheme
security = HTTPBearer()

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> dict:
    """Get current authenticated user from token"""
    token = credentials.credentials
    try:
        decoded_token = await verify_firebase_token(token)
        email = decoded_token.get("email")
        
        if not email:
            raise HTTPException(status_code=401, detail="Invalid authentication token")
        
        # Check cache first
        cached_user = get_cached_user(token)
        if cached_user:
            return {"email": email, "user_doc": cached_user, "decoded_token": decoded_token}
        
        # Query database
        user_doc = users_collection.find_one({"email": email})
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        # Cache user data
        cache_user(token, user_doc)
        
        return {"email": email, "user_doc": user_doc, "decoded_token": decoded_token}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=401, detail="Invalid authentication token")

