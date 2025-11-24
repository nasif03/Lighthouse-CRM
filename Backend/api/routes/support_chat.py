"""In-app support chat API routes."""
from __future__ import annotations

from typing import List, Literal, Optional
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from api.dependencies import get_current_user
from services.support_ai import generate_support_response, SupportAIError


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


router = APIRouter(prefix="/api/support-chat", tags=["support-chat"])


@router.post("", response_model=SupportChatResponse)
async def support_chat(
    payload: SupportChatRequest,
    current_user: dict = Depends(get_current_user),
) -> SupportChatResponse:
    """Handle support chat requests by proxying to the Support AI service."""
    user_doc = current_user.get("user_doc") or {}
    org_id = str(user_doc.get("activeOrgId") or user_doc.get("orgId") or "")
    metadata = {
        "userEmail": current_user.get("email", ""),
        "userName": user_doc.get("name", "Unknown User"),
        "orgId": org_id,
        "orgName": user_doc.get("activeOrgName") or user_doc.get("orgName") or "",
    }

    try:
        reply = await generate_support_response(
            user_message=payload.message,
            history=[turn.model_dump() for turn in (payload.history or [])],
            metadata=metadata,
        )
        conversation_id = payload.conversationId or str(uuid4())
        return SupportChatResponse(reply=reply, conversationId=conversation_id)
    except SupportAIError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Support assistant is currently unavailable.") from exc


