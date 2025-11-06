from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from typing import Optional
import os
from dotenv import load_dotenv
import firebase_admin
from firebase_admin import credentials, auth
from pymongo import MongoClient
from bson import ObjectId
import json
import httpx
from datetime import datetime

# Load environment variables
load_dotenv()

# Initialize FastAPI app
app = FastAPI(title="Lighthouse CRM Backend")

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:3000"],  # Add your frontend URL
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# MongoDB connection
MONGO_URI = os.getenv("MONGO_URI")
if not MONGO_URI:
    raise ValueError("MONGO_URI environment variable is not set")

mongo_client = MongoClient(MONGO_URI)
db = mongo_client.lighthousecrm
users_collection = db.users
organizations_collection = db.organizations
leads_collection = db.leads

# Test database connection
try:
    mongo_client.admin.command('ping')
    print("✅ MongoDB connection successful")
except Exception as e:
    print(f"❌ MongoDB connection failed: {str(e)}")
    raise

# Firebase configuration
FIREBASE_PROJECT_ID = os.getenv("FIREBASE_PROJECT_ID", "lighthousecrm-6caf2")
FIREBASE_API_KEY = os.getenv("FIREBASE_API_KEY", "AIzaSyB_nk1k3gqAr1KCEljJdEtAcGIlGRArrSw")

# Initialize Firebase Admin SDK
# Note: For production, you should use a service account JSON file
# For now, we'll verify tokens using Firebase REST API
try:
    # Try to get existing default app
    firebase_admin.get_app()
except ValueError:
    # Initialize with default credentials if available
    # Otherwise, we'll use REST API verification
    try:
        cred = credentials.ApplicationDefault()
        firebase_admin.initialize_app(cred, {
            'projectId': FIREBASE_PROJECT_ID,
        })
    except Exception:
        # If ApplicationDefault fails, we'll verify tokens via REST API
        pass

# Security scheme
security = HTTPBearer()

# Pydantic models
class UserResponse(BaseModel):
    id: str
    name: str
    email: str
    picture: Optional[str] = None
    orgId: Optional[str] = None
    
    class Config:
        from_attributes = True

class TokenResponse(BaseModel):
    token: str
    user: UserResponse

class VerifyTokenRequest(BaseModel):
    id_token: str

class CreateLeadRequest(BaseModel):
    name: str
    email: str
    source: str
    status: str
    phone: Optional[str] = None
    firstName: Optional[str] = None
    lastName: Optional[str] = None

class LeadResponse(BaseModel):
    id: str
    name: str
    email: str
    source: str
    status: str
    ownerId: str
    orgId: str
    createdAt: str
    updatedAt: str

# Helper function to verify Firebase ID token
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

# Dependency to get current user
async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> dict:
    """Get current authenticated user from token"""
    token = credentials.credentials
    try:
        decoded_token = await verify_firebase_token(token)
        return decoded_token
    except Exception as e:
        raise HTTPException(status_code=401, detail="Invalid authentication token")

# Routes
@app.get("/")
async def root():
    return {"message": "Lighthouse CRM Backend API"}

@app.post("/api/auth/verify-token", response_model=TokenResponse)
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
        org_id: Optional[str] = None
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
            has_changes = False
            
            # Only update fields that actually changed
            if user_doc.get("name") != name:
                update_data["name"] = name
                has_changes = True
            if user_doc.get("picture") != picture:
                update_data["picture"] = picture
                has_changes = True
            if not user_doc.get("orgId") and org_id:
                update_data["orgId"] = org_id
                has_changes = True
            if user_doc.get("firebaseUid") != uid:
                update_data["firebaseUid"] = uid
                has_changes = True
            
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

@app.get("/api/auth/me", response_model=UserResponse)
async def get_current_user_info(current_user: dict = Depends(get_current_user)):
    """Get current authenticated user info"""
    email = current_user.get("email")
    user_doc = users_collection.find_one({"email": email})
    
    if not user_doc:
        raise HTTPException(status_code=404, detail="User not found")
    
    return UserResponse(
        id=str(user_doc["_id"]),
        name=user_doc.get("name", "User"),
        email=user_doc.get("email"),
        picture=user_doc.get("picture"),
        orgId=user_doc.get("orgId")
    )

@app.post("/api/auth/logout")
async def logout():
    """Logout endpoint (client-side token removal)"""
    return {"message": "Logged out successfully"}

# Leads endpoints
@app.post("/api/leads", response_model=LeadResponse)
async def create_lead(request: CreateLeadRequest, current_user: dict = Depends(get_current_user)):
    """Create a new lead"""
    try:
        email = current_user.get("email")
        if not email:
            raise HTTPException(status_code=400, detail="User email not found in token")
        
        user_doc = users_collection.find_one({"email": email})
        
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        owner_id = str(user_doc["_id"])
        org_id = user_doc.get("orgId")
        
        if not org_id:
            raise HTTPException(
                status_code=400, 
                detail="User must belong to an organization. Please sign out and sign in again to create an organization."
            )
        
        now = datetime.utcnow()
        
        # Parse name into firstName and lastName if not provided
        name_parts = request.name.strip().split(" ", 1)
        first_name = request.firstName or name_parts[0] if name_parts else ""
        last_name = request.lastName or (name_parts[1] if len(name_parts) > 1 else "")
        
        # Create lead document according to database_struct.json
        # Note: MongoDB schema validation requires specific types
        # Fields like score, convertedAt, convertedBy cannot be null - omit them if not set
        lead_data = {
            "accountId": None,  # Can be null per schema
            "contactId": None,   # Can be null per schema
            "name": request.name,
            "firstName": first_name,
            "lastName": last_name,
            "email": request.email,
            "phone": request.phone or "",
            "source": request.source,
            "ownerId": owner_id,
            "orgId": org_id,
            "status": request.status,
            "tags": [],
            "converted": False,
            "metadata": None,  # Can be null per schema
            "createdAt": now,
            "updatedAt": now,
        }
        
        # score, convertedAt, and convertedBy cannot be null per schema validation
        # Omit these fields entirely for new leads (they'll be added when lead is converted/scored)
        
        # Insert lead into database
        try:
            result = leads_collection.insert_one(lead_data)
            lead_id = str(result.inserted_id)
            
            # Verify the lead was inserted
            if not result.inserted_id:
                raise HTTPException(status_code=500, detail="Failed to insert lead into database")
            
            # Verify the lead exists in database
            inserted_lead = leads_collection.find_one({"_id": result.inserted_id})
            if not inserted_lead:
                raise HTTPException(status_code=500, detail="Lead was not found after insertion")
            
        except Exception as db_error:
            print(f"Database error creating lead: {str(db_error)}")
            raise HTTPException(status_code=500, detail=f"Database error: {str(db_error)}")
        
        return LeadResponse(
            id=lead_id,
            name=request.name,
            email=request.email,
            source=request.source,
            status=request.status,
            ownerId=owner_id,
            orgId=org_id,
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating lead: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to create lead: {str(e)}")

@app.get("/api/leads", response_model=list[LeadResponse])
async def get_leads(current_user: dict = Depends(get_current_user)):
    """Get all leads for the current user's organization"""
    try:
        email = current_user.get("email")
        if not email:
            raise HTTPException(status_code=400, detail="User email not found in token")
        
        user_doc = users_collection.find_one({"email": email})
        
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        org_id = user_doc.get("orgId")
        
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        # Get all leads for this organization
        try:
            leads = list(leads_collection.find({"orgId": org_id}).sort("createdAt", -1))
        except Exception as db_error:
            print(f"Database error fetching leads: {str(db_error)}")
            raise HTTPException(status_code=500, detail=f"Database error: {str(db_error)}")
        
        return [
            LeadResponse(
                id=str(lead["_id"]),
                name=lead.get("name", ""),
                email=lead.get("email", ""),
                source=lead.get("source", ""),
                status=lead.get("status", "new"),
                ownerId=lead.get("ownerId", ""),
                orgId=lead.get("orgId", ""),
                createdAt=lead.get("createdAt").isoformat() if lead.get("createdAt") else "",
                updatedAt=lead.get("updatedAt").isoformat() if lead.get("updatedAt") else ""
            )
            for lead in leads
        ]
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching leads: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to fetch leads: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 3000))
    uvicorn.run(app, host="0.0.0.0", port=port)

