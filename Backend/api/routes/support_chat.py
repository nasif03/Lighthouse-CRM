"""Support Chat API routes with MCP tool integration"""
from fastapi import APIRouter, HTTPException, Depends, Request
from pydantic import BaseModel
from typing import Optional, List
from bson import ObjectId

from api.dependencies import get_current_user
from services.support_ai import (
    process_support_chat,
    get_conversation_history,
    get_or_create_conversation_id,
)

router = APIRouter(prefix="/api/support-chat", tags=["support-chat"])


class SupportChatRequest(BaseModel):
    message: str
    conversationId: Optional[str] = None


class SupportChatResponse(BaseModel):
    reply: str
    conversationId: str


class ChatMessage(BaseModel):
    id: str
    role: str
    content: str
    createdAt: str


class SupportChatHistoryResponse(BaseModel):
    conversationId: str
    messages: List[ChatMessage]


@router.post("", response_model=SupportChatResponse)
async def send_message(
    request: SupportChatRequest,
    request_obj: Request,
    current_user: dict = Depends(get_current_user)
):
    """
    Send a message to Support AI and get a response.
    The AI can use MCP tools to perform actions based on user permissions.
    """
    try:
        user_doc = current_user["user_doc"]
        user_id = str(user_doc["_id"])
        email = current_user["email"]
        
        # Get active organization
        org_ids = user_doc.get("orgId", [])
        if isinstance(org_ids, str):
            org_ids = [org_ids]
        
        active_org_id = user_doc.get("activeOrgId")
        if not active_org_id and org_ids:
            active_org_id = org_ids[0]
        
        if not active_org_id:
            raise HTTPException(
                status_code=400,
                detail="No active organization found. Please set an active organization."
            )
        
        # Extract the Firebase token from the Authorization header
        auth_token = None
        if request_obj:
            auth_header = request_obj.headers.get("Authorization", "")
            if auth_header.startswith("Bearer "):
                auth_token = auth_header[7:]  # Remove "Bearer " prefix
        
        # Process the chat message
        reply, conversation_id = await process_support_chat(
            user_message=request.message,
            conversation_id=request.conversationId,
            user_id=user_id,
            org_id=active_org_id,
            user_doc=user_doc,
            auth_token=auth_token,
        )
        
        return SupportChatResponse(
            reply=reply,
            conversationId=conversation_id
        )
        
    except HTTPException:
        raise
    except Exception as e:
        print(f"Support chat error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to process message: {str(e)}"
        )


@router.get("/history", response_model=SupportChatHistoryResponse)
async def get_history(
    conversationId: Optional[str] = None,
    current_user: dict = Depends(get_current_user)
):
    """Get conversation history for the current user"""
    try:
        user_doc = current_user["user_doc"]
        user_id = str(user_doc["_id"])
        
        # Get active organization
        org_ids = user_doc.get("orgId", [])
        if isinstance(org_ids, str):
            org_ids = [org_ids]
        
        active_org_id = user_doc.get("activeOrgId")
        if not active_org_id and org_ids:
            active_org_id = org_ids[0]
        
        if not active_org_id:
            raise HTTPException(
                status_code=400,
                detail="No active organization found."
            )
        
        # Get conversation ID
        if not conversationId:
            conversationId = get_or_create_conversation_id(user_id, active_org_id)
        
        if not conversationId:
            # No conversation history
            return SupportChatHistoryResponse(
                conversationId="",
                messages=[]
            )
        
        # Get messages
        messages = get_conversation_history(conversationId, user_id, active_org_id)
        
        return SupportChatHistoryResponse(
            conversationId=conversationId,
            messages=[ChatMessage(**msg) for msg in messages]
        )
        
    except HTTPException:
        raise
    except Exception as e:
        print(f"Get history error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to get history: {str(e)}"
        )

