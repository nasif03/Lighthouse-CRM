"""Support AI helper service."""
from __future__ import annotations

from typing import Dict, List, Optional
import httpx

from config.settings import (
    SUPPORT_AI_API_KEY,
    SUPPORT_AI_MODEL,
    SUPPORT_AI_BASE_URL,
    SUPPORT_AI_SYSTEM_PROMPT,
)

# Default system prompt keeps responses focused on CRM support topics.
DEFAULT_SYSTEM_PROMPT = (
    "You are Lighthouse CRM's virtual support specialist. "
    "Provide concise, actionable answers about using the CRM, Jira/JS"
    "M integration, ticket workflows, MCP tools, and related setup. "
    "If you lack information or something requires human help, explain how to "
    "reach the Lighthouse CRM support team. Never invent facts about account "
    "status or credentials."
)


class SupportAIError(Exception):
    """Domain-specific error for support assistant issues."""


async def generate_support_response(
    user_message: str,
    history: Optional[List[Dict[str, str]]] = None,
    metadata: Optional[Dict[str, str]] = None,
) -> str:
    """Call the configured LLM provider and return a support response."""
    if not SUPPORT_AI_API_KEY:
        raise SupportAIError("Support assistant is not configured. Please contact an administrator.")

    api_base = SUPPORT_AI_BASE_URL.rstrip("/") if SUPPORT_AI_BASE_URL else "https://api.openai.com/v1"
    model = SUPPORT_AI_MODEL or "gpt-4o-mini"
    system_prompt = SUPPORT_AI_SYSTEM_PROMPT or DEFAULT_SYSTEM_PROMPT

    messages: List[Dict[str, str]] = [{"role": "system", "content": system_prompt}]

    # Include limited history (last 6 turns) to keep token usage manageable.
    trimmed_history = (history or [])[-6:]
    for turn in trimmed_history:
        role = turn.get("role")
        content = (turn.get("content") or "").strip()
        if role in {"user", "assistant"} and content:
            messages.append({"role": role, "content": content})

    metadata_text = ""
    if metadata:
        user_name = metadata.get("userName")
        org_name = metadata.get("orgName")
        metadata_text = f" (User: {user_name}, Org: {org_name})"

    messages.append({"role": "user", "content": f"{user_message.strip()}{metadata_text}"})

    payload = {
        "model": model,
        "messages": messages,
        "temperature": 0.2,
        "max_tokens": 600,
    }
    headers = {
        "Authorization": f"Bearer {SUPPORT_AI_API_KEY}",
        "Content-Type": "application/json",
    }

    try:
        async with httpx.AsyncClient(timeout=40.0) as client:
            response = await client.post(f"{api_base}/chat/completions", json=payload, headers=headers)
            response.raise_for_status()
            data = response.json()
            choices = data.get("choices") or []
            if not choices:
                raise SupportAIError("Support assistant returned an empty response.")
            message = choices[0].get("message", {})
            content = (message.get("content") or "").strip()
            if not content:
                raise SupportAIError("Support assistant could not generate a reply. Please try again.")
            return content
    except httpx.HTTPStatusError as exc:
        error_detail = exc.response.text if exc.response is not None else str(exc)
        raise SupportAIError(f"Support assistant error: {error_detail}") from exc
    except httpx.HTTPError as exc:
        raise SupportAIError("Unable to reach the support assistant service. Please try again.") from exc


