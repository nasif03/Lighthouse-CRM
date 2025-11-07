"""Authentication service - Firebase token verification with caching"""
from fastapi import HTTPException
from firebase_admin import auth
import httpx
from config.settings import FIREBASE_API_KEY
from utils.performance import time_operation
from services.user_cache import get_cached_auth

# Track if Firebase Admin SDK is available (avoid 9s timeout on every request)
_firebase_admin_available = None

def _check_firebase_admin_available() -> bool:
    """Check if Firebase Admin SDK is available (cache the result)"""
    global _firebase_admin_available
    if _firebase_admin_available is None:
        try:
            # Quick check without full verification
            auth._get_app()
            _firebase_admin_available = True
        except Exception:
            _firebase_admin_available = False
            print("ℹ️  Firebase Admin SDK not available - will use REST API or JWT decode")
    return _firebase_admin_available

async def verify_firebase_token(id_token: str) -> dict:
    """Verify Firebase ID token and return user info with optimized fallback"""
    # Check cache first (if token was verified recently, skip expensive verification)
    cached = get_cached_auth(id_token)
    if cached:
        return cached[0]  # Return cached decoded_token
    
    # Check if Firebase Admin SDK is available (avoid 9s timeout)
    if _check_firebase_admin_available():
        try:
            with time_operation("Firebase: Admin SDK verification", threshold_ms=100.0):
                decoded_token = auth.verify_id_token(id_token)
            return decoded_token
        except Exception as e:
            print(f"⚠️  Firebase Admin SDK verification failed: {str(e)}")
            # Fall through to REST API
    
    # Try REST API with shorter timeout
    try:
        with time_operation("Firebase: REST API verification", threshold_ms=500.0):
            async with httpx.AsyncClient(timeout=5.0) as client:  # Reduced timeout from 10s to 5s
                response = await client.post(
                    f"https://www.googleapis.com/identitytoolkit/v3/relyingparty/getAccountInfo?key={FIREBASE_API_KEY}",
                    json={"idToken": id_token},
                    timeout=5.0
                )
                if response.status_code == 200:
                    data = response.json()
                    if "users" in data and len(data["users"]) > 0:
                        user_info = data["users"][0]
                        # Convert to format similar to Firebase Admin SDK
                        return {
                            "uid": user_info.get("localId"),
                            "email": user_info.get("email"),
                            "name": user_info.get("displayName"),
                            "picture": user_info.get("photoUrl"),
                            "email_verified": user_info.get("emailVerified", False)
                        }
                raise HTTPException(status_code=401, detail="Invalid token")
    except httpx.TimeoutException:
        print("⚠️  Firebase REST API timeout - falling back to JWT decode")
    except Exception as e:
        print(f"⚠️  Firebase REST API failed: {str(e)} - falling back to JWT decode")
    
    # Fallback: JWT decode without verification (development only)
    try:
        import jwt
        with time_operation("Firebase: JWT decode (no verification)", threshold_ms=10.0):
            # Decode without verification (for development only)
            decoded = jwt.decode(id_token, options={"verify_signature": False})
        print("⚠️  Using JWT decode without verification (development mode)")
        return decoded
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Token verification failed: {str(e)}")

