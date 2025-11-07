"""Role management models"""
from pydantic import BaseModel
from typing import List, Optional

class RoleResponse(BaseModel):
    id: str
    name: str
    permissions: List[str]
    orgId: str
    createdAt: str
    updatedAt: str

class CreateRoleRequest(BaseModel):
    name: str
    permissions: List[str]

class UpdateRoleRequest(BaseModel):
    name: Optional[str] = None
    permissions: Optional[List[str]] = None

