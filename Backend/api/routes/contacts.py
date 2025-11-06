"""Contacts API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from models.contact import ContactResponse, CreateContactRequest, UpdateContactRequest
from api.dependencies import get_current_user
from config.database import contacts_collection

router = APIRouter(prefix="/api/contacts", tags=["contacts"])

@router.get("", response_model=list[ContactResponse])
async def get_contacts(
    skip: int = 0,
    limit: int = 100,
    current_user: dict = Depends(get_current_user)
):
    """Get contacts for the current user's organization with pagination"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        cursor = contacts_collection.find(
            {"orgId": org_id, "deleted": {"$ne": True}},
            {"firstName": 1, "lastName": 1, "email": 1, "phone": 1, "title": 1, "accountId": 1, "ownerId": 1, "orgId": 1, "tags": 1, "createdAt": 1, "updatedAt": 1}
        ).sort("createdAt", -1).skip(skip).limit(limit)
        contacts = list(cursor)
        
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

@router.post("", response_model=ContactResponse)
async def create_contact(request: CreateContactRequest, current_user: dict = Depends(get_current_user)):
    """Create a new contact"""
    try:
        user_doc = current_user.get("user_doc")
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

@router.put("/{contact_id}", response_model=ContactResponse)
async def update_contact(contact_id: str, request: UpdateContactRequest, current_user: dict = Depends(get_current_user)):
    """Update a contact"""
    try:
        user_doc = current_user.get("user_doc")
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

@router.delete("/{contact_id}")
async def delete_contact(contact_id: str, current_user: dict = Depends(get_current_user)):
    """Soft delete a contact"""
    try:
        user_doc = current_user.get("user_doc")
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

