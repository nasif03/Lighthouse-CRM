"""Account-related Pydantic models"""
from pydantic import BaseModel
from typing import Optional

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

