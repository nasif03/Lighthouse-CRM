"""Support AI helper service with MCP tool integration."""
from __future__ import annotations

from typing import Dict, List, Optional, Any
import httpx
import json

from config.settings import (
    SUPPORT_AI_API_KEY,
    SUPPORT_AI_MODEL,
    SUPPORT_AI_BASE_URL,
    SUPPORT_AI_SYSTEM_PROMPT,
)

# Import MCP tools
import sys
from pathlib import Path
backend_dir = Path(__file__).parent.parent
sys.path.insert(0, str(backend_dir))

import importlib.util
spec = importlib.util.spec_from_file_location("mcp_tools", backend_dir / "mcp-crm" / "tools" / "__init__.py")
mcp_tools = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mcp_tools)

DEFAULT_SYSTEM_PROMPT = (
    "You are Lighthouse CRM's virtual support specialist. "
    "You have access to tools to query and manage CRM data. "
    "Available tools include: get_leads, get_contacts, get_deals, get_accounts, get_tickets, get_dashboard_stats, get_analytics, "
    "create_lead, create_contact, create_deal, create_account, create_ticket, and more. "
    "When users ask about CRM data, ALWAYS use the appropriate tools to get real information. "
    "Never make up or guess data - always query the database using tools. "
    "If you need to call a tool, use the function calling format. "
    "Provide concise, actionable answers based on the actual data you retrieve."
)

class SupportAIError(Exception):
    """Domain-specific error for support assistant issues."""

def convert_mcp_tool_to_openai_function(mcp_tool) -> Dict[str, Any]:
    """Convert MCP Tool to OpenAI function format."""
    return {
        "type": "function",
        "function": {
            "name": mcp_tool.name,
            "description": mcp_tool.description,
            "parameters": mcp_tool.inputSchema
        }
    }

async def call_mcp_tool(tool_name: str, arguments: Dict[str, Any], user_context: Dict[str, str]) -> str:
    """Call an MCP tool handler directly."""
    handler = mcp_tools.TOOL_HANDLERS.get(tool_name)
    if not handler:
        available_tools = ", ".join(sorted(mcp_tools.TOOL_HANDLERS.keys())[:10])
        return f"Error: Tool '{tool_name}' not found. Available tools include: {available_tools} (and more)."
    
    # Inject user context into arguments
    arguments["context"] = user_context
    
    try:
        result = await handler(**arguments)
        # Format result nicely
        if isinstance(result, dict):
            return json.dumps(result, indent=2)
        elif isinstance(result, list):
            return json.dumps(result, indent=2)
        else:
            return str(result)
    except Exception as e:
        import traceback
        error_details = str(e)
        return f"Error executing {tool_name}: {error_details}"

async def generate_support_response(
    user_message: str,
    history: Optional[List[Dict[str, str]]] = None,
    metadata: Optional[Dict[str, str]] = None,
) -> str:
    """Call LLM with function calling support for MCP tools."""
    if not SUPPORT_AI_API_KEY:
        raise SupportAIError("Support assistant is not configured. Please contact an administrator.")

    api_base = SUPPORT_AI_BASE_URL.rstrip("/") if SUPPORT_AI_BASE_URL else "https://api.openai.com/v1"
    model = SUPPORT_AI_MODEL or "gpt-4o-mini"
    system_prompt = SUPPORT_AI_SYSTEM_PROMPT or DEFAULT_SYSTEM_PROMPT

    # Get MCP tools and convert to OpenAI format
    mcp_tool_list = mcp_tools.get_all_tools()
    functions = [convert_mcp_tool_to_openai_function(tool) for tool in mcp_tool_list]
    
    # Build list of available tool names for the prompt
    available_tool_names = [tool.name for tool in mcp_tool_list]
    tool_list_text = ", ".join(available_tool_names[:20])  # Limit to first 20 to avoid token bloat
    
    # Enhance system prompt with available tools
    enhanced_system_prompt = f"{system_prompt}\n\nAvailable tools: {tool_list_text}"

    messages: List[Dict[str, Any]] = [{"role": "system", "content": enhanced_system_prompt}]
    
    # Include limited history
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

    # User context for MCP tools
    user_context = {
        "userEmail": metadata.get("userEmail", "") if metadata else "",
        "orgId": metadata.get("orgId", "") if metadata else "",
    }

    # Function calling loop (max 3 iterations to avoid infinite loops)
    max_iterations = 3
    for iteration in range(max_iterations):
        payload = {
            "model": model,
            "messages": messages,
            "temperature": 0.2,
            "max_tokens": 1000,
        }
        
        # Add functions if available
        if functions:
            payload["tools"] = functions
            payload["tool_choice"] = "auto"

        headers = {
            "Authorization": f"Bearer {SUPPORT_AI_API_KEY}",
            "Content-Type": "application/json",
        }

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.post(f"{api_base}/chat/completions", json=payload, headers=headers)
                response.raise_for_status()
                data = response.json()
                choices = data.get("choices") or []
                if not choices:
                    raise SupportAIError("Support assistant returned an empty response.")
                
                message = choices[0].get("message", {})
                content = (message.get("content") or "").strip()
                
                # Check for OpenAI-style function calls first
                tool_calls = message.get("tool_calls")
                
                # Fallback: Parse JSON tool calls from content (for Ollama compatibility)
                if not tool_calls and content:
                    # Try to extract JSON tool calls from the text - look for patterns like {"name": "...", "parameters": {...}}
                    import re
                    # More flexible pattern to catch JSON objects with name and parameters
                    json_pattern = r'\{\s*"name"\s*:\s*"([^"]+)"\s*,\s*"parameters"\s*:\s*(\{[^}]*\}|{})\s*\}'
                    json_matches = re.findall(json_pattern, content)
                    if json_matches:
                        tool_calls = []
                        for idx, (tool_name, params_str) in enumerate(json_matches):
                            if tool_name and tool_name in mcp_tools.TOOL_HANDLERS:
                                try:
                                    tool_params = json.loads(params_str) if params_str.strip() != "{}" else {}
                                    tool_calls.append({
                                        "id": f"call_{iteration}_{idx}",
                                        "function": {
                                            "name": tool_name,
                                            "arguments": json.dumps(tool_params)
                                        }
                                    })
                                except json.JSONDecodeError:
                                    # Try to parse as string if it's not valid JSON
                                    tool_params = {}
                                    tool_calls.append({
                                        "id": f"call_{iteration}_{idx}",
                                        "function": {
                                            "name": tool_name,
                                            "arguments": json.dumps(tool_params)
                                        }
                                    })
                    else:
                        # Try to find JSON objects anywhere in the content
                        try:
                            # Look for standalone JSON objects
                            json_obj_pattern = r'\{\s*"name"\s*:\s*"[^"]+"\s*[^}]*\}'
                            matches = re.findall(json_obj_pattern, content)
                            for idx, match in enumerate(matches):
                                try:
                                    tool_call_data = json.loads(match)
                                    tool_name = tool_call_data.get("name")
                                    if tool_name and tool_name in mcp_tools.TOOL_HANDLERS:
                                        tool_params = tool_call_data.get("parameters", {})
                                        tool_calls.append({
                                            "id": f"call_{iteration}_{idx}",
                                            "function": {
                                                "name": tool_name,
                                                "arguments": json.dumps(tool_params)
                                            }
                                        })
                                except (json.JSONDecodeError, AttributeError):
                                    continue
                        except Exception:
                            pass
                
                if tool_calls:
                    # Add assistant message with tool calls
                    messages.append(message)
                    
                    # Execute all tool calls
                    for tool_call in tool_calls:
                        tool_name = tool_call.get("function", {}).get("name")
                        tool_args_str = tool_call.get("function", {}).get("arguments", "{}")
                        
                        # Parse arguments
                        try:
                            tool_args = json.loads(tool_args_str) if isinstance(tool_args_str, str) else tool_args_str
                        except json.JSONDecodeError:
                            tool_args = {}
                        
                        tool_call_id = tool_call.get("id", f"call_{iteration}_{tool_name}")
                        
                        # Call the MCP tool
                        tool_result = await call_mcp_tool(tool_name, tool_args, user_context)
                        
                        # Add tool result to messages
                        messages.append({
                            "role": "tool",
                            "tool_call_id": tool_call_id,
                            "content": tool_result
                        })
                    
                    # Continue loop to get final response
                    continue
                else:
                    # No function calls - return the text response
                    if not content:
                        raise SupportAIError("Support assistant could not generate a reply. Please try again.")
                    return content
                    
        except httpx.HTTPStatusError as exc:
            error_detail = exc.response.text if exc.response is not None else str(exc)
            raise SupportAIError(f"Support assistant error: {error_detail}") from exc
        except httpx.HTTPError as exc:
            raise SupportAIError("Unable to reach the support assistant service. Please try again.") from exc
    
    # If we exhausted iterations, return a message
    return "I've processed your request using the available tools. If you need more information, please ask a specific question."
