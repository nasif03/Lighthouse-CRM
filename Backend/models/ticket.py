"""Support ticket-related Pydantic models"""
from pydantic import BaseModel, EmailStr
from typing import Optional

class CreateTicketRequest(BaseModel):
    """Request model for creating a support ticket (public endpoint)"""
    orgId: str  # Organization ID - required to route ticket to correct org
    name: str
    email: EmailStr
    phone: Optional[str] = None
    subject: str
    description: str
    priority: Optional[str] = "medium"  # low, medium, high, urgent
    category: Optional[str] = None  # e.g., technical, billing, feature_request, etc.

class UpdateTicketRequest(BaseModel):
    """Request model for updating a support ticket"""
    status: Optional[str] = None  # open, in_progress, resolved, closed
    priority: Optional[str] = None  # low, medium, high, urgent
    assignedTo: Optional[str] = None  # User ID of assigned employee
    category: Optional[str] = None

class TicketResponse(BaseModel):
    """Response model for support ticket"""
    id: str
    ticketNumber: str  # Human-readable ticket number
    orgId: str
    name: str
    email: str
    phone: Optional[str] = None
    subject: str
    description: str
    priority: str
    category: Optional[str] = None
    status: str  # open, in_progress, resolved, closed
    assignedTo: Optional[str] = None  # User ID of assigned agent
    assignedToName: Optional[str] = None  # Name of assigned agent
    createdAt: str
    updatedAt: str

