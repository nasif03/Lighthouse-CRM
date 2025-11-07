"""Activity log models"""
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class ActivityResponse(BaseModel):
    id: str
    entityType: str
    entityId: str
    userId: str
    orgId: str
    summary: str
    body: str
    tags: List[str]
    createdAt: str

class ActivityListResponse(BaseModel):
    activities: List[ActivityResponse]
    total: int
