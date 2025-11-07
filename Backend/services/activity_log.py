"""Activity logging service - Auto-logs key actions"""
from datetime import datetime
from bson import ObjectId
from config.database import activities_collection

def create_activity(
    org_id: str,
    user_id: str,
    entity_type: str,
    entity_id: str,
    summary: str,
    body: str = "",
    tags: list = None
):
    """Create an activity log entry"""
    activity_data = {
        "orgId": org_id,
        "userId": user_id,
        "entityType": entity_type,
        "entityId": entity_id,
        "summary": summary,
        "body": body,
        "tags": tags or [],
        "attachments": [],
        "recordingUrl": "",
        "externalId": "",
        "createdAt": datetime.utcnow()
    }
    
    result = activities_collection.insert_one(activity_data)
    return str(result.inserted_id)

def log_lead_created(org_id: str, user_id: str, lead_id: str, lead_name: str):
    """Log lead creation activity"""
    return create_activity(
        org_id=org_id,
        user_id=user_id,
        entity_type="lead",
        entity_id=lead_id,
        summary=f"Lead '{lead_name}' was created",
        body=f"New lead '{lead_name}' was added to the system"
    )

def log_lead_converted(
    org_id: str,
    user_id: str,
    lead_id: str,
    lead_name: str,
    account_id: str,
    contact_id: str,
    deal_id: str
):
    """Log lead conversion activity"""
    return create_activity(
        org_id=org_id,
        user_id=user_id,
        entity_type="lead",
        entity_id=lead_id,
        summary=f"Lead '{lead_name}' was converted to Deal",
        body=f"Lead '{lead_name}' was converted. Created Account ({account_id}), Contact ({contact_id}), and Deal ({deal_id})",
        tags=["conversion", "lead", "deal"]
    )

def log_deal_stage_changed(
    org_id: str,
    user_id: str,
    deal_id: str,
    deal_name: str,
    old_stage: str,
    new_stage: str
):
    """Log deal stage change activity"""
    return create_activity(
        org_id=org_id,
        user_id=user_id,
        entity_type="deal",
        entity_id=deal_id,
        summary=f"Deal '{deal_name}' stage changed from '{old_stage}' to '{new_stage}'",
        body=f"Deal stage updated: {old_stage} → {new_stage}",
        tags=["deal", "stage_change"]
    )

def log_deal_created(org_id: str, user_id: str, deal_id: str, deal_name: str):
    """Log deal creation activity"""
    return create_activity(
        org_id=org_id,
        user_id=user_id,
        entity_type="deal",
        entity_id=deal_id,
        summary=f"Deal '{deal_name}' was created",
        body=f"New deal '{deal_name}' was added to the pipeline"
    )

def log_account_created(org_id: str, user_id: str, account_id: str, account_name: str):
    """Log account creation activity"""
    return create_activity(
        org_id=org_id,
        user_id=user_id,
        entity_type="account",
        entity_id=account_id,
        summary=f"Account '{account_name}' was created",
        body=f"New account '{account_name}' was added"
    )

def log_contact_created(org_id: str, user_id: str, contact_id: str, contact_name: str):
    """Log contact creation activity"""
    return create_activity(
        org_id=org_id,
        user_id=user_id,
        entity_type="contact",
        entity_id=contact_id,
        summary=f"Contact '{contact_name}' was created",
        body=f"New contact '{contact_name}' was added"
    )
