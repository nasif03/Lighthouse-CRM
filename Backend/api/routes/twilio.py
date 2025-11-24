"""Twilio VoIP API routes"""
from fastapi import APIRouter, HTTPException, Depends
from models.twilio import MakeCallRequest, MakeCallResponse, CallStatusResponse
from api.dependencies import get_current_user
from services.twilio_service import make_call, get_call_status, is_allowed_number

router = APIRouter(prefix="/api/twilio", tags=["twilio"])

@router.post("/call", response_model=MakeCallResponse)
async def create_call(
    request: MakeCallRequest,
    current_user: dict = Depends(get_current_user)
):
    """Make a phone call using Twilio"""
    try:
        # Check if number is allowed
        if not is_allowed_number(request.to):
            raise HTTPException(
                status_code=403,
                detail=f"Calling to {request.to} is not allowed. Only +8801957128594 is allowed."
            )
        
        # Make the call
        result = make_call(request.to, request.message)
        
        return MakeCallResponse(
            success=result['success'],
            call_sid=result['call_sid'],
            status=result['status'],
            to=result['to'],
            from_=result['from'],
            message="Call initiated successfully"
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to make call: {str(e)}")

@router.get("/call/{call_sid}", response_model=CallStatusResponse)
async def get_call(
    call_sid: str,
    current_user: dict = Depends(get_current_user)
):
    """Get the status of a call"""
    try:
        status = get_call_status(call_sid)
        
        return CallStatusResponse(
            sid=status['sid'],
            status=status['status'],
            to=status['to'],
            from_=status['from'],
            duration=status.get('duration'),
            start_time=status.get('start_time'),
            end_time=status.get('end_time')
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to get call status: {str(e)}")

@router.get("/allowed-number")
async def get_allowed_number(current_user: dict = Depends(get_current_user)):
    """Get the allowed phone number for calls"""
    return {
        "allowed_number": "+8801957128594",
        "message": "Only calls to this number are allowed"
    }

