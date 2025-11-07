"""Tenant/Organization models"""
from pydantic import BaseModel
from typing import List, Optional

class TenantResponse(BaseModel):
    id: str
    name: str

class TenantListResponse(BaseModel):
    tenants: List[TenantResponse]
    activeTenantId: Optional[str] = None

class SwitchTenantRequest(BaseModel):
    tenant_id: str
