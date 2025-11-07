"""Organization/tenant management models"""
from pydantic import BaseModel
from typing import Optional, List

class OrganizationResponse(BaseModel):
    id: str
    name: str
    domain: str
    createdAt: str
    updatedAt: str

class CreateOrganizationRequest(BaseModel):
    name: str
    domain: Optional[str] = None

class UpdateOrganizationRequest(BaseModel):
    name: Optional[str] = None

