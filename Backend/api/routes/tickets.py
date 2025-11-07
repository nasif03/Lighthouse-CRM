"""Support tickets API routes"""
from fastapi import APIRouter, HTTPException
from bson import ObjectId
from datetime import datetime
from models.ticket import CreateTicketRequest, TicketResponse
from config.database import tickets_collection, organizations_collection

router = APIRouter(prefix="/api/tickets", tags=["tickets"])

def generate_ticket_number(org_id: str) -> str:
    """Generate a unique ticket number in format: TKT-YYYYMMDD-XXXX"""
    from datetime import datetime
    date_prefix = datetime.utcnow().strftime("%Y%m%d")
    
    # Find the highest ticket number for today for this org
    today_prefix = f"TKT-{date_prefix}-"
    today_tickets = tickets_collection.find({
        "orgId": org_id,
        "ticketNumber": {"$regex": f"^{today_prefix}"}
    }).sort("ticketNumber", -1).limit(1)
    
    last_ticket = list(today_tickets)
    if last_ticket and last_ticket[0].get("ticketNumber"):
        # Extract the number part and increment
        last_num = last_ticket[0]["ticketNumber"].split("-")[-1]
        try:
            next_num = int(last_num) + 1
        except ValueError:
            next_num = 1
    else:
        next_num = 1
    
    return f"{today_prefix}{next_num:04d}"

@router.post("", response_model=TicketResponse)
async def create_ticket(request: CreateTicketRequest):
    """
    Create a new support ticket (PUBLIC ENDPOINT - No authentication required)
    Customers can submit tickets via this endpoint
    """
    try:
        # Validate that organization exists
        org = organizations_collection.find_one({"_id": ObjectId(request.orgId)})
        if not org:
            raise HTTPException(status_code=404, detail="Organization not found")
        
        now = datetime.utcnow()
        ticket_number = generate_ticket_number(request.orgId)
        
        ticket_data = {
            "ticketNumber": ticket_number,
            "orgId": request.orgId,
            "name": request.name,
            "email": request.email,
            "phone": request.phone or "",
            "subject": request.subject,
            "description": request.description,
            "priority": request.priority or "medium",
            "category": request.category or "",
            "status": "open",
            "assignedTo": None,
            "createdAt": now,
            "updatedAt": now,
        }
        
        result = tickets_collection.insert_one(ticket_data)
        ticket_id = str(result.inserted_id)
        
        return TicketResponse(
            id=ticket_id,
            ticketNumber=ticket_number,
            orgId=request.orgId,
            name=request.name,
            email=request.email,
            phone=request.phone,
            subject=request.subject,
            description=request.description,
            priority=request.priority or "medium",
            category=request.category,
            status="open",
            assignedTo=None,
            createdAt=now.isoformat(),
            updatedAt=now.isoformat()
        )
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating ticket: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create ticket: {str(e)}")

