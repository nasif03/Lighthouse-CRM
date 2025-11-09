"""Gmail API routes"""
from fastapi import APIRouter, HTTPException, Depends
from typing import Optional
from models.gmail import (
    GmailAuthRequest,
    GmailAuthResponse,
    GmailMessagesResponse,
    GmailMessage,
    SendEmailRequest,
    SendEmailResponse
)
from api.dependencies import get_current_user
from services.gmail import (
    is_authenticated,
    get_authorization_url,
    exchange_code_for_token,
    get_messages,
    send_message,
    get_gmail_service,
    create_credentials_from_firebase_token
)
from models.gmail import GmailAuthRequest

router = APIRouter(prefix="/api/gmail", tags=["gmail"])

@router.get("/auth/status", response_model=GmailAuthResponse)
async def get_auth_status(current_user: dict = Depends(get_current_user)):
    """Check Gmail authentication status"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        user_email = user_doc.get("email")
        if not user_email:
            raise HTTPException(status_code=400, detail="User email not found")
        
        # Quick check: see if token file exists first
        from services.gmail import get_token_path
        token_path = get_token_path(user_email)
        
        authenticated = False
        if token_path.exists():
            # Token file exists, do full verification
            try:
                authenticated = is_authenticated(user_email)
            except Exception as e:
                print(f"Error verifying Gmail authentication for {user_email}: {e}")
                authenticated = False
        
        if authenticated:
            return GmailAuthResponse(
                authenticated=True,
                message="Gmail is authenticated"
            )
        else:
            # Generate authorization URL only if not authenticated
            try:
                redirect_uri = "http://localhost:5173"  # Frontend base URL
                auth_data = get_authorization_url(user_email, redirect_uri)
                
                return GmailAuthResponse(
                    authenticated=False,
                    authorization_url=auth_data['authorization_url'],
                    message="Gmail authentication required"
                )
            except Exception as e:
                print(f"Error generating authorization URL: {e}")
                return GmailAuthResponse(
                    authenticated=False,
                    message=f"Failed to generate authorization URL: {str(e)}"
                )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to check auth status: {str(e)}")

@router.post("/auth/callback", response_model=GmailAuthResponse)
async def handle_auth_callback(
    request: GmailAuthRequest,
    current_user: dict = Depends(get_current_user)
):
    """Handle Gmail OAuth callback - supports both authorization code and Firebase token"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        user_email = user_doc.get("email")
        if not user_email:
            raise HTTPException(status_code=400, detail="User email not found")
        
        # If access token is provided (from Firebase OAuth), use it directly
        if request.access_token:
            try:
                create_credentials_from_firebase_token(
                    user_email,
                    request.access_token,
                    request.refresh_token
                )
                return GmailAuthResponse(
                    authenticated=True,
                    message="Gmail authenticated successfully via Firebase"
                )
            except Exception as e:
                print(f"Error creating credentials from Firebase token: {e}")
                # Fall through to authorization code flow
        
        # Otherwise, use authorization code flow
        if request.authorization_code:
            redirect_uri = "http://localhost:5173"  # Frontend base URL
            exchange_code_for_token(user_email, request.authorization_code, redirect_uri)
            
            return GmailAuthResponse(
                authenticated=True,
                message="Gmail authenticated successfully"
            )
        
        raise HTTPException(status_code=400, detail="Either authorization_code or access_token is required")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to authenticate: {str(e)}")

@router.get("/messages", response_model=GmailMessagesResponse)
async def get_gmail_messages(
    max_results: int = 10,
    query: str = '',
    current_user: dict = Depends(get_current_user)
):
    """Get Gmail messages for the current user"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        user_email = user_doc.get("email")
        if not user_email:
            raise HTTPException(status_code=400, detail="User email not found")
        
        if not is_authenticated(user_email):
            raise HTTPException(status_code=401, detail="Gmail not authenticated")
        
        messages = get_messages(user_email, max_results=max_results, query=query)
        
        # Convert to response model
        gmail_messages = [
            GmailMessage(
                id=msg['id'],
                threadId=msg.get('threadId', ''),
                subject=msg.get('subject', ''),
                from_=msg.get('from', ''),
                date=msg.get('date', ''),
                snippet=msg.get('snippet', ''),
                body=msg.get('body', ''),
                labels=msg.get('labels', [])
            )
            for msg in messages
        ]
        
        return GmailMessagesResponse(
            messages=gmail_messages,
            total=len(gmail_messages)
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch messages: {str(e)}")

@router.post("/send", response_model=SendEmailResponse)
async def send_gmail(
    request: SendEmailRequest,
    current_user: dict = Depends(get_current_user)
):
    """Send an email via Gmail"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        user_email = user_doc.get("email")
        if not user_email:
            raise HTTPException(status_code=400, detail="User email not found")
        
        if not is_authenticated(user_email):
            raise HTTPException(status_code=401, detail="Gmail not authenticated")
        
        result = send_message(
            user_email,
            request.to,
            request.subject,
            request.body
        )
        
        return SendEmailResponse(
            id=result['id'],
            threadId=result.get('threadId', ''),
            success=result.get('success', True)
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to send email: {str(e)}")

