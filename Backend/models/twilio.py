"""Twilio-related Pydantic models"""
from pydantic import BaseModel
from typing import Optional

class MakeCallRequest(BaseModel):
    to: str
    message: Optional[str] = "Hello, this is a call from Lighthouse CRM."

class MakeCallResponse(BaseModel):
    success: bool
    call_sid: str
    status: str
    to: str
    from_: str
    message: Optional[str] = None
    
    class Config:
        from_attributes = True
        fields = {
            'from_': 'from'
        }

class CallStatusResponse(BaseModel):
    sid: str
    status: str
    to: str
    from_: str
    duration: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    
    class Config:
        from_attributes = True
        fields = {
            'from_': 'from'
        }

