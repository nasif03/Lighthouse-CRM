"""Accounts API routes"""
from fastapi import APIRouter, HTTPException, Depends
from bson import ObjectId
from datetime import datetime
from models.account import AccountResponse, CreateAccountRequest, UpdateAccountRequest
from api.dependencies import get_current_user
from config.database import accounts_collection, contacts_collection, deals_collection

router = APIRouter(prefix="/api/accounts", tags=["accounts"])

@router.get("", response_model=list[AccountResponse])
async def get_accounts(
    skip: int = 0,
    limit: int = 100,
    current_user: dict = Depends(get_current_user)
):
    """Get accounts for the current user's organization with pagination"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        cursor = accounts_collection.find(
            {"orgId": org_id, "deleted": {"$ne": True}},
            {"name": 1, "domain": 1, "industry": 1, "phone": 1, "status": 1, "ownerId": 1, "orgId": 1, "createdAt": 1, "updatedAt": 1}
        ).sort("createdAt", -1).skip(skip).limit(limit)
        accounts = list(cursor)
        
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

@router.post("", response_model=AccountResponse)
async def create_account(request: CreateAccountRequest, current_user: dict = Depends(get_current_user)):
    """Create a new account"""
    try:
        user_doc = current_user.get("user_doc")
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

@router.put("/{account_id}", response_model=AccountResponse)
async def update_account(account_id: str, request: UpdateAccountRequest, current_user: dict = Depends(get_current_user)):
    """Update an account"""
    try:
        user_doc = current_user.get("user_doc")
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

@router.get("/{account_id}")
async def get_account(account_id: str, current_user: dict = Depends(get_current_user)):
    """Get a single account with linked contacts and deals"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found in database")
        
        org_id = user_doc.get("orgId")
        if not org_id:
            raise HTTPException(status_code=400, detail="User must belong to an organization")
        
        account = accounts_collection.find_one({"_id": ObjectId(account_id), "orgId": org_id})
        if not account:
            raise HTTPException(status_code=404, detail="Account not found")
        
        linked_contacts = list(contacts_collection.find({
            "accountId": ObjectId(account_id),
            "orgId": org_id,
            "deleted": {"$ne": True}
        }))
        
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

@router.delete("/{account_id}")
async def delete_account(account_id: str, current_user: dict = Depends(get_current_user)):
    """Soft delete an account"""
    try:
        user_doc = current_user.get("user_doc")
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

