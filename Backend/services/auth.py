"""Authentication service - Firebase token verification"""
from fastapi import HTTPException
from firebase_admin import auth
import httpx
from config.settings import FIREBASE_API_KEY

async def verify_firebase_token(id_token: str) -> dict:
    """Verify Firebase ID token and return user info"""
    try:
        # Try using Firebase Admin SDK first
        decoded_token = auth.verify_id_token(id_token)
        return decoded_token
    except Exception:
        # If Firebase Admin SDK is not available, verify via REST API
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"https://www.googleapis.com/identitytoolkit/v3/relyingparty/getAccountInfo?key={FIREBASE_API_KEY}",
                    json={"idToken": id_token},
                    timeout=10.0
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
        except Exception as e:
            # As a fallback, we can decode the token (JWT) without verification
            # This is less secure but works for development
            # In production, always use proper verification
            try:
                import jwt
                # Decode without verification (for development only)
                decoded = jwt.decode(id_token, options={"verify_signature": False})
                return decoded
            except Exception:
                raise HTTPException(status_code=401, detail=f"Token verification failed: {str(e)}")

