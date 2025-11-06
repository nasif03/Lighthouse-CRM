"""Deal-related Pydantic models"""
from pydantic import BaseModel
from typing import Optional

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

