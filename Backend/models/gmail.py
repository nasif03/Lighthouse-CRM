"""Gmail-related Pydantic models"""
from pydantic import BaseModel, EmailStr
from typing import Optional, List, Dict

class GmailAuthRequest(BaseModel):
    authorization_code: Optional[str] = None
    access_token: Optional[str] = None
    refresh_token: Optional[str] = None

class GmailAuthResponse(BaseModel):
    authenticated: bool
    authorization_url: Optional[str] = None
    message: str

class GmailMessage(BaseModel):
    id: str
    threadId: str
    subject: str
    from_: str
    date: str
    snippet: str
    body: str
    labels: List[str]
    
    class Config:
        from_attributes = True
        fields = {
            'from_': 'from'
        }

class GmailMessagesResponse(BaseModel):
    messages: List[GmailMessage]
    total: int

class SendEmailRequest(BaseModel):
    to: EmailStr
    subject: str
    body: str

class SendEmailResponse(BaseModel):
    id: str
    threadId: str
    success: bool

