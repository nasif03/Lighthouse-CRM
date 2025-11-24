"""In-app support chat API routes."""
from __future__ import annotations

from typing import List, Literal, Optional
from uuid import uuid4
from datetime import datetime
from bson import ObjectId

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from api.dependencies import get_current_user
from services.support_ai import generate_support_response, SupportAIError
from config.database import support_chat_messages_collection


class ChatTurn(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(..., min_length=1, max_length=2000)


class SupportChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=2000)
    conversationId: Optional[str] = None
    history: Optional[List[ChatTurn]] = None


class SupportChatResponse(BaseModel):
    reply: str
    conversationId: str

class SupportChatMessage(BaseModel):
    id: str
    role: Literal["user", "assistant"]
    content: str
    createdAt: str

class SupportChatHistoryResponse(BaseModel):
    conversationId: str
    messages: List[SupportChatMessage]


router = APIRouter(prefix="/api/support-chat", tags=["support-chat"])


@router.post("", response_model=SupportChatResponse)
async def support_chat(
    payload: SupportChatRequest,
    current_user: dict = Depends(get_current_user),
) -> SupportChatResponse:
    """Handle support chat requests by proxying to the Support AI service."""
    user_doc = current_user.get("user_doc") or {}
    user_id = str(user_doc.get("_id", ""))
    org_id = str(user_doc.get("activeOrgId") or user_doc.get("orgId") or "")
    
    # Generate conversation ID: one conversation per user per org
    conversation_id = payload.conversationId or f"{user_id}_{org_id}"
    
    metadata = {
        "userEmail": current_user.get("email", ""),
        "userName": user_doc.get("name", "Unknown User"),
        "orgId": org_id,
        "orgName": user_doc.get("activeOrgName") or user_doc.get("orgName") or "",
    }

    # Load conversation history from database
    history_messages = list(
        support_chat_messages_collection.find(
            {"conversationId": conversation_id, "orgId": org_id}
        ).sort("createdAt", 1).limit(20)  # Last 20 messages for context
    )
    
    # Convert to format expected by support_ai
    history = [
        {"role": msg["role"], "content": msg["content"]}
        for msg in history_messages
    ]

    try:
        reply = await generate_support_response(
            user_message=payload.message,
            history=history,
            metadata=metadata,
        )
        
        # Save user message to database
        user_message_doc = {
            "conversationId": conversation_id,
            "orgId": org_id,
            "userId": user_id,
            "role": "user",
            "content": payload.message,
            "createdAt": datetime.utcnow(),
        }
        support_chat_messages_collection.insert_one(user_message_doc)
        
        # Save assistant reply to database
        assistant_message_doc = {
            "conversationId": conversation_id,
            "orgId": org_id,
            "userId": user_id,
            "role": "assistant",
            "content": reply,
            "createdAt": datetime.utcnow(),
        }
        support_chat_messages_collection.insert_one(assistant_message_doc)
        
        return SupportChatResponse(reply=reply, conversationId=conversation_id)
    except SupportAIError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Support assistant is currently unavailable.") from exc


@router.get("/history", response_model=SupportChatHistoryResponse)
async def get_support_chat_history(
    conversationId: Optional[str] = None,
    current_user: dict = Depends(get_current_user),
) -> SupportChatHistoryResponse:
    """Get conversation history for Support AI chat."""
    user_doc = current_user.get("user_doc") or {}
    user_id = str(user_doc.get("_id", ""))
    org_id = str(user_doc.get("activeOrgId") or user_doc.get("orgId") or "")
    
    # Use provided conversationId or generate default (one per user per org)
    conversation_id = conversationId or f"{user_id}_{org_id}"
    
    # Load messages from database
    messages = list(
        support_chat_messages_collection.find(
            {"conversationId": conversation_id, "orgId": org_id}
        ).sort("createdAt", 1)
    )
    
    return SupportChatHistoryResponse(
        conversationId=conversation_id,
        messages=[
            SupportChatMessage(
                id=str(msg["_id"]),
                role=msg["role"],
                content=msg["content"],
                createdAt=msg["createdAt"].isoformat() if isinstance(msg["createdAt"], datetime) else str(msg["createdAt"]),
            )
            for msg in messages
        ]
    )


