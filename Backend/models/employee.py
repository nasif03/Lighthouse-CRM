"""Employee/User management models"""
from pydantic import BaseModel, EmailStr
from typing import Optional, List

class EmployeeResponse(BaseModel):
    id: str
    name: str
    email: str
    picture: Optional[str] = None
    roleIds: List[str]
    isAdmin: bool
    createdAt: str

class CreateEmployeeRequest(BaseModel):
    email: EmailStr
    name: str
    roleIds: Optional[List[str]] = None

class UpdateEmployeeRequest(BaseModel):
    name: Optional[str] = None
    roleIds: Optional[List[str]] = None

