"""Lead-related Pydantic models"""
from pydantic import BaseModel
from typing import Optional

class CreateLeadRequest(BaseModel):
    name: str
    email: str
    source: str
    status: str
    phone: Optional[str] = None
    firstName: Optional[str] = None
    lastName: Optional[str] = None

class UpdateLeadStatusRequest(BaseModel):
    status: str

class LeadResponse(BaseModel):
    id: str
    name: str
    email: str
    source: str
    status: str
    phone: Optional[str] = None
    firstName: Optional[str] = None
    lastName: Optional[str] = None
    tags: list = []
    ownerId: str
    orgId: str
    createdAt: str
    updatedAt: str

