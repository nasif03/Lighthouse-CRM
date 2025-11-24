"""MCP tools for Twilio integration"""
from typing import Any, Dict, Optional
# Import utils using path workaround
import sys
from pathlib import Path
backend_dir = Path(__file__).parent.parent.parent
sys.path.insert(0, str(backend_dir))
import importlib.util
spec = importlib.util.spec_from_file_location("mcp_crm_utils", backend_dir / "mcp-crm" / "utils.py")
mcp_crm_utils = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mcp_crm_utils)
get_user_context = mcp_crm_utils.get_user_context
validate_user_context = mcp_crm_utils.validate_user_context
format_error_response = mcp_crm_utils.format_error_response
format_success_response = mcp_crm_utils.format_success_response
from services.twilio_service import make_call, get_call_status, is_allowed_number


async def make_phone_call(
    to: str,
    message: str = "Hello, this is a call from Lighthouse CRM.",
    context: Optional[Dict] = None
) -> str:
    """
    Make a phone call using Twilio VoIP.
    
    Args:
        to: Phone number to call (must be allowed number: +8801957128594)
        message: Message to say during the call (default: greeting message)
        context: MCP request context (for authentication)
    
    Returns:
        Success message with call SID
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        
        # Check if number is allowed
        if not is_allowed_number(to):
            return f"Calling to {to} is not allowed. Only +8801957128594 is allowed."
        
        # Make the call
        result = make_call(to, message)
        
        return format_success_response(
            f"Call initiated successfully to {result['to']}",
            {
                "callSid": result["call_sid"],
                "status": result["status"],
                "to": result["to"],
                "from": result["from"]
            }
        )
    except Exception as e:
        return format_error_response(e)


async def get_call_status_info(
    call_sid: str,
    context: Optional[Dict] = None
) -> str:
    """
    Get the status of a Twilio call.
    
    Args:
        call_sid: Twilio call SID
        context: MCP request context (for authentication)
    
    Returns:
        Call status information
    """
    try:
        user_context = await get_user_context(context or {})
        user_context = validate_user_context(user_context)
        
        # Get call status
        status = get_call_status(call_sid)
        
        return format_success_response(
            f"Call status retrieved",
            {
                "sid": status["sid"],
                "status": status["status"],
                "to": status["to"],
                "from": status["from"],
                "duration": status.get("duration"),
                "startTime": status.get("start_time"),
                "endTime": status.get("end_time")
            }
        )
    except Exception as e:
        return format_error_response(e)

