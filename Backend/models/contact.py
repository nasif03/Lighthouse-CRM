"""Contact-related Pydantic models"""
from pydantic import BaseModel
from typing import Optional

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

