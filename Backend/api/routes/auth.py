"""Authentication routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from models.user import UserResponse, TokenResponse, VerifyTokenRequest
from services.auth import verify_firebase_token
from api.dependencies import get_current_user
from config.database import users_collection, organizations_collection

router = APIRouter(prefix="/api/auth", tags=["auth"])

@router.post("/verify-token", response_model=TokenResponse)
async def verify_token(request: VerifyTokenRequest):
    """Verify Firebase ID token and return user info"""
    try:
        # Verify the token
        decoded_token = await verify_firebase_token(request.id_token)
        
        # Extract user info
        uid = decoded_token.get("uid")
        email = decoded_token.get("email")
        name = decoded_token.get("name", email.split("@")[0] if email else "User")
        picture = decoded_token.get("picture")
        now = datetime.utcnow()
        org_id = None
        existing_org = None
        
        # Resolve organization by email domain; create if missing (optimized - single query)
        if email and "@" in email:
            domain = email.split("@", 1)[1].lower()
            existing_org = organizations_collection.find_one({"domain": domain})
            if not existing_org:
                org_doc = {
                    "name": domain,
                    "billingInfo": None,
                    "domain": domain,
                    "settings": None,
                    "salesStages": [],
                    "admins": [],
                    "createdAt": now,
                    "updatedAt": now,
                }
                org_insert = organizations_collection.insert_one(org_doc)
                org_id = str(org_insert.inserted_id)
                # Store org data to avoid another query
                existing_org = {"_id": org_insert.inserted_id, "admins": []}
            else:
                org_id = str(existing_org["_id"])
        
        # Check if user exists in database
        user_doc = users_collection.find_one({"email": email})
    
        if not user_doc:
            # Determine if user should be admin (first user in org)
            is_admin = False
            if org_id and existing_org and (not existing_org.get("admins") or len(existing_org.get("admins", [])) == 0):
                is_admin = True
            
            # Create new user according to database_struct.json
            user_data = {
                "email": email,
                "name": name,
                "password": None,
                "picture": picture,
                "roleIds": [],
                "orgId": org_id,
                "isAdmin": is_admin,
                "lastSeenAt": now,
                "createdAt": now,
                "firebaseUid": uid,
                "updatedAt": now,
            }
            insert_result = users_collection.insert_one(user_data)
            user_id = str(insert_result.inserted_id)
            
            # Update org admins if this is the first admin (single update operation)
            if is_admin and org_id:
                organizations_collection.update_one(
                    {"_id": ObjectId(org_id)}, 
                    {"$set": {"updatedAt": now}, "$push": {"admins": user_id}}
                )
        else:
            # Update user info if needed (optimized - single update)
            user_id = str(user_doc["_id"])
            update_data = {"updatedAt": now, "lastSeenAt": now}
            
            # Only update fields that actually changed
            if user_doc.get("name") != name:
                update_data["name"] = name
            if user_doc.get("picture") != picture:
                update_data["picture"] = picture
            if not user_doc.get("orgId") and org_id:
                update_data["orgId"] = org_id
            if user_doc.get("firebaseUid") != uid:
                update_data["firebaseUid"] = uid
            
            # Always update lastSeenAt and updatedAt (even if no other changes)
            users_collection.update_one({"_id": ObjectId(user_id)}, {"$set": update_data})
        
        # Return token and user info
        # Get user doc to include orgId in response
        final_user_doc = users_collection.find_one({"_id": ObjectId(user_id)})
        
        return TokenResponse(
            token=request.id_token,
            user=UserResponse(
                id=user_id,
                name=name,
                email=email,
                picture=picture,
                orgId=final_user_doc.get("orgId") if final_user_doc else None
            )
        )
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Token verification failed: {str(e)}")

@router.get("/me", response_model=UserResponse)
async def get_current_user_info(current_user: dict = Depends(get_current_user)):
    """Get current authenticated user info"""
    user_doc = current_user.get("user_doc")
    
    if not user_doc:
        raise HTTPException(status_code=404, detail="User not found")
    
    return UserResponse(
        id=str(user_doc["_id"]),
        name=user_doc.get("name", "User"),
        email=user_doc.get("email"),
        picture=user_doc.get("picture"),
        orgId=user_doc.get("orgId")
    )

@router.post("/logout")
async def logout():
    """Logout endpoint (client-side token removal)"""
    return {"message": "Logged out successfully"}

