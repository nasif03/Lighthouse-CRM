"""User-related Pydantic models"""
from pydantic import BaseModel
from typing import Optional, Union, List

class UserResponse(BaseModel):
    id: str
    name: str
    email: str
    picture: Optional[str] = None
    orgId: Optional[Union[str, List[str]]] = None
    
    class Config:
        from_attributes = True

class TokenResponse(BaseModel):
    token: str
    user: UserResponse

class VerifyTokenRequest(BaseModel):
    id_token: str

