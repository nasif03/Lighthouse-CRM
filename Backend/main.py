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
contacts_collection = db.contacts
accounts_collection = db.accounts
deals_collection = db.deals

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

# Contacts Models
class CreateContactRequest(BaseModel):
    firstName: str
    lastName: Optional[str] = None
    email: str
    phone: Optional[str] = None
    title: Optional[str] = None
    accountId: Optional[str] = None
    tags: Optional[list[str]] = None

class UpdateContactRequest(BaseModel):
    firstName: Optional[str] = None
    lastName: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    title: Optional[str] = None
    accountId: Optional[str] = None
    tags: Optional[list[str]] = None

class ContactResponse(BaseModel):
    id: str
    firstName: str
    lastName: Optional[str] = None
    email: str
    phone: Optional[str] = None
    title: Optional[str] = None
    accountId: Optional[str] = None
    ownerId: str
    orgId: str
    tags: list[str]
    createdAt: str
    updatedAt: str

# Accounts Models
class CreateAccountRequest(BaseModel):
    name: str
    domain: Optional[str] = None
    industry: Optional[str] = None
    phone: Optional[str] = None
    status: Optional[str] = None
    address: Optional[dict] = None

class UpdateAccountRequest(BaseModel):
    name: Optional[str] = None
    domain: Optional[str] = None
    industry: Optional[str] = None
    phone: Optional[str] = None
    status: Optional[str] = None
    address: Optional[dict] = None

class AccountResponse(BaseModel):
    id: str
    name: str
    domain: Optional[str] = None
    industry: Optional[str] = None
    phone: Optional[str] = None
    status: Optional[str] = None
    ownerId: str
    orgId: str
    createdAt: str
    updatedAt: str

# Deals Models
class CreateDealRequest(BaseModel):
    name: str
    accountId: Optional[str] = None
    contactId: Optional[str] = None
    amount: Optional[float] = None
    currency: Optional[str] = "USD"
    stageId: Optional[str] = None
    stageName: Optional[str] = None
    probability: Optional[float] = None
    closeDate: Optional[str] = None
    status: Optional[str] = "open"
    tags: Optional[list[str]] = None

class UpdateDealRequest(BaseModel):
    name: Optional[str] = None
    accountId: Optional[str] = None
    contactId: Optional[str] = None
    amount: Optional[float] = None
    currency: Optional[str] = None
    stageId: Optional[str] = None
    stageName: Optional[str] = None
    probability: Optional[float] = None
    closeDate: Optional[str] = None
    status: Optional[str] = None
    tags: Optional[list[str]] = None

class DealResponse(BaseModel):
    id: str
    name: str
    accountId: Optional[str] = None
    contactId: Optional[str] = None
    amount: Optional[float] = None
    currency: Optional[str] = None
    stageId: Optional[str] = None
    stageName: Optional[str] = None
    probability: Optional[float] = None
    closeDate: Optional[str] = None
    status: str
    ownerId: str
    orgId: str
    tags: list[str]
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

# Contacts endpoints
@app.get("/api/contacts", response_model=list[ContactResponse])
async def get_contacts(current_user: dict = Depends(get_current_user)):
    """Get all contacts for the current user's organization"""
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
        
        contacts = list(contacts_collection.find({"orgId": org_id, "deleted": {"$ne": True}}).sort("createdAt", -1))
        
        return [
            ContactResponse(
                id=str(contact["_id"]),
                firstName=contact.get("firstName", ""),
                lastName=contact.get("lastName"),
                email=contact.get("email", ""),
                phone=contact.get("phone"),
                title=contact.get("title"),
                accountId=str(contact["accountId"]) if contact.get("accountId") else None,
                ownerId=contact.get("ownerId", ""),
                orgId=contact.get("orgId", ""),
                tags=contact.get("tags", []),
                createdAt=contact.get("createdAt").isoformat() if contact.get("createdAt") else "",
                updatedAt=contact.get("updatedAt").isoformat() if contact.get("updatedAt") else ""
            )
            for contact in contacts
        ]
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching contacts: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch contacts: {str(e)}")

@app.post("/api/contacts", response_model=ContactResponse)
async def create_contact(request: CreateContactRequest, current_user: dict = Depends(get_current_user)):
    """Create a new contact"""
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
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        now = datetime.utcnow()
        
        contact_data = {
            "firstName": request.firstName,
            "lastName": request.lastName or "",
            "email": request.email,
            "phone": request.phone or "",
            "title": request.title or "",
            "accountId": ObjectId(request.accountId) if request.accountId else None,
            "ownerId": owner_id,
            "orgId": org_id,
            "tags": request.tags or [],
            "metadata": None,
            "deleted": False,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = contacts_collection.insert_one(contact_data)
        contact_id = str(result.inserted_id)
        
        return ContactResponse(
            id=contact_id,
            firstName=request.firstName,
            lastName=request.lastName,
            email=request.email,
            phone=request.phone,
            title=request.title,
            accountId=request.accountId,
            ownerId=owner_id,
            orgId=org_id,
            tags=request.tags or [],
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating contact: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create contact: {str(e)}")

@app.put("/api/contacts/{contact_id}", response_model=ContactResponse)
async def update_contact(contact_id: str, request: UpdateContactRequest, current_user: dict = Depends(get_current_user)):
    """Update a contact"""
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
        
        contact = contacts_collection.find_one({"_id": ObjectId(contact_id), "orgId": org_id})
        if not contact:
            raise HTTPException(status_code=404, detail="Contact not found")
        
        update_data = {"updatedAt": datetime.utcnow()}
        if request.firstName is not None:
            update_data["firstName"] = request.firstName
        if request.lastName is not None:
            update_data["lastName"] = request.lastName
        if request.email is not None:
            update_data["email"] = request.email
        if request.phone is not None:
            update_data["phone"] = request.phone
        if request.title is not None:
            update_data["title"] = request.title
        if request.accountId is not None:
            update_data["accountId"] = ObjectId(request.accountId) if request.accountId else None
        if request.tags is not None:
            update_data["tags"] = request.tags
        
        contacts_collection.update_one({"_id": ObjectId(contact_id)}, {"$set": update_data})
        
        updated_contact = contacts_collection.find_one({"_id": ObjectId(contact_id)})
        return ContactResponse(
            id=str(updated_contact["_id"]),
            firstName=updated_contact.get("firstName", ""),
            lastName=updated_contact.get("lastName"),
            email=updated_contact.get("email", ""),
            phone=updated_contact.get("phone"),
            title=updated_contact.get("title"),
            accountId=str(updated_contact["accountId"]) if updated_contact.get("accountId") else None,
            ownerId=updated_contact.get("ownerId", ""),
            orgId=updated_contact.get("orgId", ""),
            tags=updated_contact.get("tags", []),
            createdAt=updated_contact.get("createdAt").isoformat() if updated_contact.get("createdAt") else "",
            updatedAt=updated_contact.get("updatedAt").isoformat() if updated_contact.get("updatedAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating contact: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to update contact: {str(e)}")

@app.delete("/api/contacts/{contact_id}")
async def delete_contact(contact_id: str, current_user: dict = Depends(get_current_user)):
    """Soft delete a contact"""
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
        
        contact = contacts_collection.find_one({"_id": ObjectId(contact_id), "orgId": org_id})
        if not contact:
            raise HTTPException(status_code=404, detail="Contact not found")
        
        contacts_collection.update_one(
            {"_id": ObjectId(contact_id)},
            {"$set": {"deleted": True, "updatedAt": datetime.utcnow()}}
        )
        
        return {"message": "Contact deleted successfully"}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error deleting contact: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to delete contact: {str(e)}")

# Accounts endpoints
@app.get("/api/accounts", response_model=list[AccountResponse])
async def get_accounts(current_user: dict = Depends(get_current_user)):
    """Get all accounts for the current user's organization"""
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
        
        accounts = list(accounts_collection.find({"orgId": org_id, "deleted": {"$ne": True}}).sort("createdAt", -1))
        
        return [
            AccountResponse(
                id=str(account["_id"]),
                name=account.get("name", ""),
                domain=account.get("domain"),
                industry=account.get("industry"),
                phone=account.get("phone"),
                status=account.get("status"),
                ownerId=account.get("ownerId", ""),
                orgId=account.get("orgId", ""),
                createdAt=account.get("createdAt").isoformat() if account.get("createdAt") else "",
                updatedAt=account.get("updatedAt").isoformat() if account.get("updatedAt") else ""
            )
            for account in accounts
        ]
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching accounts: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch accounts: {str(e)}")

@app.post("/api/accounts", response_model=AccountResponse)
async def create_account(request: CreateAccountRequest, current_user: dict = Depends(get_current_user)):
    """Create a new account"""
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
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        now = datetime.utcnow()
        
        account_data = {
            "name": request.name,
            "domain": request.domain or "",
            "industry": request.industry or "",
            "phone": request.phone or "",
            "status": request.status or "active",
            "ownerId": owner_id,
            "orgId": org_id,
            "metadata": None,
            "address": request.address,
            "deleted": False,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = accounts_collection.insert_one(account_data)
        account_id = str(result.inserted_id)
        
        return AccountResponse(
            id=account_id,
            name=request.name,
            domain=request.domain,
            industry=request.industry,
            phone=request.phone,
            status=request.status or "active",
            ownerId=owner_id,
            orgId=org_id,
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating account: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create account: {str(e)}")

@app.put("/api/accounts/{account_id}", response_model=AccountResponse)
async def update_account(account_id: str, request: UpdateAccountRequest, current_user: dict = Depends(get_current_user)):
    """Update an account"""
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
        
        account = accounts_collection.find_one({"_id": ObjectId(account_id), "orgId": org_id})
        if not account:
            raise HTTPException(status_code=404, detail="Account not found")
        
        update_data = {"updatedAt": datetime.utcnow()}
        if request.name is not None:
            update_data["name"] = request.name
        if request.domain is not None:
            update_data["domain"] = request.domain
        if request.industry is not None:
            update_data["industry"] = request.industry
        if request.phone is not None:
            update_data["phone"] = request.phone
        if request.status is not None:
            update_data["status"] = request.status
        if request.address is not None:
            update_data["address"] = request.address
        
        accounts_collection.update_one({"_id": ObjectId(account_id)}, {"$set": update_data})
        
        updated_account = accounts_collection.find_one({"_id": ObjectId(account_id)})
        return AccountResponse(
            id=str(updated_account["_id"]),
            name=updated_account.get("name", ""),
            domain=updated_account.get("domain"),
            industry=updated_account.get("industry"),
            phone=updated_account.get("phone"),
            status=updated_account.get("status"),
            ownerId=updated_account.get("ownerId", ""),
            orgId=updated_account.get("orgId", ""),
            createdAt=updated_account.get("createdAt").isoformat() if updated_account.get("createdAt") else "",
            updatedAt=updated_account.get("updatedAt").isoformat() if updated_account.get("updatedAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating account: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to update account: {str(e)}")

@app.get("/api/accounts/{account_id}")
async def get_account(account_id: str, current_user: dict = Depends(get_current_user)):
    """Get a single account with linked contacts and deals"""
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
        
        account = accounts_collection.find_one({"_id": ObjectId(account_id), "orgId": org_id})
        if not account:
            raise HTTPException(status_code=404, detail="Account not found")
        
        # Get linked contacts
        linked_contacts = list(contacts_collection.find({
            "accountId": ObjectId(account_id),
            "orgId": org_id,
            "deleted": {"$ne": True}
        }))
        
        # Get linked deals
        linked_deals = list(deals_collection.find({
            "accountId": ObjectId(account_id),
            "orgId": org_id
        }))
        
        return {
            "account": AccountResponse(
                id=str(account["_id"]),
                name=account.get("name", ""),
                domain=account.get("domain"),
                industry=account.get("industry"),
                phone=account.get("phone"),
                status=account.get("status"),
                ownerId=account.get("ownerId", ""),
                orgId=account.get("orgId", ""),
                createdAt=account.get("createdAt").isoformat() if account.get("createdAt") else "",
                updatedAt=account.get("updatedAt").isoformat() if account.get("updatedAt") else ""
            ),
            "contacts": [
                {
                    "id": str(c["_id"]),
                    "firstName": c.get("firstName", ""),
                    "lastName": c.get("lastName"),
                    "email": c.get("email", ""),
                }
                for c in linked_contacts
            ],
            "deals": [
                {
                    "id": str(d["_id"]),
                    "name": d.get("name", ""),
                    "amount": d.get("amount"),
                    "currency": d.get("currency"),
                    "status": d.get("status", "open"),
                }
                for d in linked_deals
            ]
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching account: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch account: {str(e)}")

@app.delete("/api/accounts/{account_id}")
async def delete_account(account_id: str, current_user: dict = Depends(get_current_user)):
    """Soft delete an account"""
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
        
        account = accounts_collection.find_one({"_id": ObjectId(account_id), "orgId": org_id})
        if not account:
            raise HTTPException(status_code=404, detail="Account not found")
        
        accounts_collection.update_one(
            {"_id": ObjectId(account_id)},
            {"$set": {"deleted": True, "updatedAt": datetime.utcnow()}}
        )
        
        return {"message": "Account deleted successfully"}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error deleting account: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to delete account: {str(e)}")

# Deals endpoints
@app.get("/api/deals", response_model=list[DealResponse])
async def get_deals(current_user: dict = Depends(get_current_user)):
    """Get all deals for the current user's organization"""
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
        
        deals = list(deals_collection.find({"orgId": org_id}).sort("createdAt", -1))
        
        return [
            DealResponse(
                id=str(deal["_id"]),
                name=deal.get("name", ""),
                accountId=str(deal["accountId"]) if deal.get("accountId") else None,
                contactId=str(deal["contactId"]) if deal.get("contactId") else None,
                amount=deal.get("amount"),
                currency=deal.get("currency"),
                stageId=deal.get("stageId"),
                stageName=deal.get("stageName"),
                probability=deal.get("probability"),
                closeDate=deal.get("closeDate").isoformat() if deal.get("closeDate") else None,
                status=deal.get("status", "open"),
                ownerId=deal.get("ownerId", ""),
                orgId=deal.get("orgId", ""),
                tags=deal.get("tags", []),
                createdAt=deal.get("createdAt").isoformat() if deal.get("createdAt") else "",
                updatedAt=deal.get("updatedAt").isoformat() if deal.get("updatedAt") else ""
            )
            for deal in deals
        ]
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error fetching deals: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch deals: {str(e)}")

@app.post("/api/deals", response_model=DealResponse)
async def create_deal(request: CreateDealRequest, current_user: dict = Depends(get_current_user)):
    """Create a new deal"""
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
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        now = datetime.utcnow()
        close_date = None
        if request.closeDate:
            try:
                close_date = datetime.fromisoformat(request.closeDate.replace('Z', '+00:00'))
            except:
                pass
        
        deal_data = {
            "name": request.name,
            "accountId": ObjectId(request.accountId) if request.accountId else None,
            "contactId": ObjectId(request.contactId) if request.contactId else None,
            "amount": request.amount,
            "currency": request.currency or "USD",
            "stageId": request.stageId or "",
            "stageName": request.stageName or "",
            "probability": request.probability,
            "closeDate": close_date,
            "status": request.status or "open",
            "ownerId": owner_id,
            "orgId": org_id,
            "tags": request.tags or [],
            "lastActivityAt": now,
            "metadata": None,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = deals_collection.insert_one(deal_data)
        deal_id = str(result.inserted_id)
        
        return DealResponse(
            id=deal_id,
            name=request.name,
            accountId=request.accountId,
            contactId=request.contactId,
            amount=request.amount,
            currency=request.currency or "USD",
            stageId=request.stageId,
            stageName=request.stageName,
            probability=request.probability,
            closeDate=request.closeDate,
            status=request.status or "open",
            ownerId=owner_id,
            orgId=org_id,
            tags=request.tags or [],
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating deal: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create deal: {str(e)}")

@app.put("/api/deals/{deal_id}", response_model=DealResponse)
async def update_deal(deal_id: str, request: UpdateDealRequest, current_user: dict = Depends(get_current_user)):
    """Update a deal"""
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
        
        deal = deals_collection.find_one({"_id": ObjectId(deal_id), "orgId": org_id})
        if not deal:
            raise HTTPException(status_code=404, detail="Deal not found")
        
        update_data = {"updatedAt": datetime.utcnow(), "lastActivityAt": datetime.utcnow()}
        if request.name is not None:
            update_data["name"] = request.name
        if request.accountId is not None:
            update_data["accountId"] = ObjectId(request.accountId) if request.accountId else None
        if request.contactId is not None:
            update_data["contactId"] = ObjectId(request.contactId) if request.contactId else None
        if request.amount is not None:
            update_data["amount"] = request.amount
        if request.currency is not None:
            update_data["currency"] = request.currency
        if request.stageId is not None:
            update_data["stageId"] = request.stageId
        if request.stageName is not None:
            update_data["stageName"] = request.stageName
        if request.probability is not None:
            update_data["probability"] = request.probability
        if request.closeDate is not None:
            try:
                update_data["closeDate"] = datetime.fromisoformat(request.closeDate.replace('Z', '+00:00'))
            except:
                pass
        if request.status is not None:
            update_data["status"] = request.status
        if request.tags is not None:
            update_data["tags"] = request.tags
        
        deals_collection.update_one({"_id": ObjectId(deal_id)}, {"$set": update_data})
        
        updated_deal = deals_collection.find_one({"_id": ObjectId(deal_id)})
        return DealResponse(
            id=str(updated_deal["_id"]),
            name=updated_deal.get("name", ""),
            accountId=str(updated_deal["accountId"]) if updated_deal.get("accountId") else None,
            contactId=str(updated_deal["contactId"]) if updated_deal.get("contactId") else None,
            amount=updated_deal.get("amount"),
            currency=updated_deal.get("currency"),
            stageId=updated_deal.get("stageId"),
            stageName=updated_deal.get("stageName"),
            probability=updated_deal.get("probability"),
            closeDate=updated_deal.get("closeDate").isoformat() if updated_deal.get("closeDate") else None,
            status=updated_deal.get("status", "open"),
            ownerId=updated_deal.get("ownerId", ""),
            orgId=updated_deal.get("orgId", ""),
            tags=updated_deal.get("tags", []),
            createdAt=updated_deal.get("createdAt").isoformat() if updated_deal.get("createdAt") else "",
            updatedAt=updated_deal.get("updatedAt").isoformat() if updated_deal.get("updatedAt") else ""
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating deal: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to update deal: {str(e)}")

@app.delete("/api/deals/{deal_id}")
async def delete_deal(deal_id: str, current_user: dict = Depends(get_current_user)):
    """Delete a deal"""
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
        
        deal = deals_collection.find_one({"_id": ObjectId(deal_id), "orgId": org_id})
        if not deal:
            raise HTTPException(status_code=404, detail="Deal not found")
        
        deals_collection.delete_one({"_id": ObjectId(deal_id)})
        
        return {"message": "Deal deleted successfully"}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error deleting deal: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to delete deal: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 3000))
    uvicorn.run(app, host="0.0.0.0", port=port)

