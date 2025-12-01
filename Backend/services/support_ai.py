"""Support AI service using Gemini API with MCP tool integration"""
import os
import json
import inspect
from typing import Optional, List, Dict, Any, Callable
from datetime import datetime
from bson import ObjectId
from google import genai

from config.database import support_chat_messages_collection
from config.settings import SUPER_ADMIN_EMAIL
from utils.permissions import has_permission, is_super_admin

# Initialize Gemini client
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "AIzaSyBEawA5f5Bj65MkKAkyHZ8EmYzCegb6FDw")
gemini_client = genai.Client(api_key=GEMINI_API_KEY)

# Import all MCP tools
from mcp_server.tools.leads import (
    create_lead,
    get_leads,
    update_lead_status,
    convert_lead_to_deal,
    delete_lead,
)

from mcp_server.tools.deals import (
    create_deal,
    get_deals,
    update_deal,
    delete_deal,
)

from mcp_server.tools.tickets import (
    create_ticket,
    get_tickets,
    get_ticket,
    update_ticket,
    get_assignable_employees,
)

from mcp_server.tools.calendar import (
    create_meeting,
    get_meetings,
)

from mcp_server.tools.admin import (
    get_organizations,
    create_organization,
    update_organization,
    get_employees,
    create_employee,
    update_employee,
    remove_employee,
    get_roles,
    create_role,
    update_role,
    delete_role,
)

# Map tool names to functions
MCP_TOOLS: Dict[str, Callable] = {
    # Leads
    "create_lead": create_lead,
    "get_leads": get_leads,
    "update_lead_status": update_lead_status,
    "convert_lead_to_deal": convert_lead_to_deal,
    "delete_lead": delete_lead,
    
    # Deals
    "create_deal": create_deal,
    "get_deals": get_deals,
    "update_deal": update_deal,
    "delete_deal": delete_deal,
    
    # Tickets
    "create_ticket": create_ticket,
    "get_tickets": get_tickets,
    "get_ticket": get_ticket,
    "update_ticket": update_ticket,
    "get_assignable_employees": get_assignable_employees,
    
    # Calendar
    "create_meeting": create_meeting,
    "get_meetings": get_meetings,
    
    # Admin
    "get_organizations": get_organizations,
    "create_organization": create_organization,
    "update_organization": update_organization,
    "get_employees": get_employees,
    "create_employee": create_employee,
    "update_employee": update_employee,
    "remove_employee": remove_employee,
    "get_roles": get_roles,
    "create_role": create_role,
    "update_role": update_role,
    "delete_role": delete_role,
}

# Permission mappings for tools
TOOL_PERMISSIONS: Dict[str, List[str]] = {
    "create_lead": ["write:leads"],
    "get_leads": ["read:leads"],
    "update_lead_status": ["write:leads"],
    "convert_lead_to_deal": ["write:leads", "write:deals"],
    "delete_lead": ["write:leads"],
    
    "create_deal": ["write:deals"],
    "get_deals": ["read:deals"],
    "update_deal": ["write:deals"],
    "delete_deal": ["write:deals"],
    
    "create_ticket": [],  # Public endpoint
    "get_tickets": ["read:tickets"],
    "get_ticket": ["read:tickets"],
    "update_ticket": ["write:tickets"],
    "get_assignable_employees": ["read:tickets"],
    
    "create_meeting": ["write:calendar"],
    "get_meetings": ["read:calendar"],
    
    "get_organizations": [],  # User can see their own orgs
    "create_organization": [],  # Anyone can create org
    "update_organization": ["admin:organizations"],
    "get_employees": ["read:employees"],
    "create_employee": ["admin:employees"],
    "update_employee": ["admin:employees"],
    "remove_employee": ["admin:employees"],
    "get_roles": ["read:roles"],
    "create_role": ["admin:roles"],
    "update_role": ["admin:roles"],
    "delete_role": ["admin:roles"],
}


def get_tool_schema(tool_name: str, tool_func: Callable) -> Dict[str, Any]:
    """Generate Gemini function calling schema from a tool function"""
    sig = inspect.signature(tool_func)
    doc = inspect.getdoc(tool_func) or ""
    
    properties = {}
    required = []
    
    for param_name, param in sig.parameters.items():
        if param_name == "auth_token":
            continue  # Skip auth_token, we'll inject it
        
        param_type = param.annotation
        if param_type == inspect.Parameter.empty:
            param_type = str
        
        # Convert Python types to JSON schema types
        if param_type == str or param_type == Optional[str]:
            json_type = "string"
        elif param_type == int or param_type == Optional[int]:
            json_type = "integer"
        elif param_type == float or param_type == Optional[float]:
            json_type = "number"
        elif param_type == bool or param_type == Optional[bool]:
            json_type = "boolean"
        elif param_type == list or param_type == List[str] or param_type == Optional[List[str]]:
            json_type = "array"
            properties[param_name] = {
                "type": "array",
                "items": {"type": "string"},
                "description": f"{param_name} parameter"
            }
            if param.default == inspect.Parameter.empty:
                required.append(param_name)
            continue
        else:
            json_type = "string"
        
        prop = {
            "type": json_type,
            "description": f"{param_name} parameter"
        }
        
        if param.default != inspect.Parameter.empty:
            prop["default"] = param.default
        else:
            required.append(param_name)
        
        properties[param_name] = prop
    
    return {
        "name": tool_name,
        "description": doc.split("\n\n")[0] if doc else f"{tool_name} function",
        "parameters": {
            "type": "object",
            "properties": properties,
            "required": required
        }
    }


def check_tool_permission(tool_name: str, user_doc: dict, org_id: str) -> bool:
    """Check if user has permission to use a tool"""
    required_perms = TOOL_PERMISSIONS.get(tool_name, [])
    
    # Super admin bypasses all checks
    if is_super_admin(user_doc):
        return True
    
    # Public endpoints (empty permission list)
    if not required_perms:
        return True
    
    # Check permissions
    return has_permission(user_doc, org_id, required_perms)


async def call_mcp_tool(tool_name: str, args: Dict[str, Any], auth_token: str) -> Any:
    """Call an MCP tool with arguments"""
    if tool_name not in MCP_TOOLS:
        raise ValueError(f"Unknown tool: {tool_name}")
    
    tool_func = MCP_TOOLS[tool_name]
    
    # Inject auth_token into args
    args_with_auth = {**args, "auth_token": auth_token}
    
    # Call the tool
    if inspect.iscoroutinefunction(tool_func):
        return await tool_func(**args_with_auth)
    else:
        return tool_func(**args_with_auth)


async def process_support_chat(
    user_message: str,
    conversation_id: Optional[str],
    user_id: str,
    org_id: str,
    user_doc: dict,
    auth_token: str,
) -> tuple[str, str]:
    """
    Process a support chat message using Gemini with MCP tool integration.
    
    Returns:
        tuple: (reply_text, conversation_id)
    """
    # Load conversation history
    history = []
    if conversation_id:
        messages = list(
            support_chat_messages_collection.find(
                {"conversationId": conversation_id}
            ).sort("createdAt", 1)
        )
        for msg in messages:
            history.append({
                "role": msg["role"],
                "parts": [{"text": msg["content"]}]
            })
    
    # Add user message to history
    history.append({
        "role": "user",
        "parts": [{"text": user_message}]
    })
    
    # Generate tool schemas for available tools (filter by permissions)
    available_tools = []
    tool_schemas = []
    for tool_name in MCP_TOOLS.keys():
        if check_tool_permission(tool_name, user_doc, org_id):
            tool_func = MCP_TOOLS[tool_name]
            schema = get_tool_schema(tool_name, tool_func)
            available_tools.append(tool_name)
            tool_schemas.append(schema)
    
    # System prompt
    system_prompt = """You are Support AI, an intelligent assistant for Lighthouse CRM. 
You can help users with:
- CRM operations (leads, deals, contacts, accounts)
- Jira/JSM ticket management
- Calendar and meeting scheduling
- Organization and employee management
- General questions about the system

When users ask you to perform actions, use the available tools. Always explain what you're doing in a friendly, helpful manner.
If a user doesn't have permission for a requested action, politely explain that you cannot perform that action due to permissions.

Be concise but thorough. Format responses clearly with proper structure."""
    
    try:
        # Build conversation context
        conversation_text = ""
        
        # Add conversation history
        for msg in history:
            role = msg.get("role", "user")
            parts = msg.get("parts", [])
            text = ""
            
            for part in parts:
                if isinstance(part, dict) and "text" in part:
                    text += part["text"]
                elif isinstance(part, str):
                    text += part
            
            if role == "user":
                conversation_text += f"User: {text}\n"
            elif role == "assistant" or role == "model":
                conversation_text += f"Assistant: {text}\n"
        
        # Build tools description
        tools_description = "\n\nYou have access to these MCP tools:\n"
        for tool_name in available_tools[:10]:  # Limit to first 10 to avoid token limits
            tool_func = MCP_TOOLS[tool_name]
            doc = inspect.getdoc(tool_func) or ""
            first_line = doc.split("\n")[0] if doc else tool_name
            tools_description += f"- {tool_name}: {first_line}\n"
        
        # Build full prompt
        full_prompt = f"""{system_prompt}

{tools_description}

{conversation_text}User: {user_message}

Assistant:"""
        
        # Generate response with Gemini (format matching user's example)
        try:
            response = gemini_client.models.generate_content(
                model="gemini-2.0-flash",
                contents=full_prompt
            )
            
            # Extract response text
            if hasattr(response, "text") and response.text:
                final_reply = response.text
            elif response and hasattr(response, "candidates"):
                # Try to extract from candidates structure
                if response.candidates and len(response.candidates) > 0:
                    candidate = response.candidates[0]
                    if hasattr(candidate, "content") and hasattr(candidate.content, "parts"):
                        text_parts = []
                        for part in candidate.content.parts:
                            if hasattr(part, "text"):
                                text_parts.append(part.text)
                        final_reply = " ".join(text_parts) if text_parts else "I apologize, but I couldn't generate a response."
                    else:
                        final_reply = str(candidate) if candidate else "I apologize, but I couldn't generate a response."
                else:
                    final_reply = "I apologize, but I couldn't generate a response."
            else:
                final_reply = str(response) if response else "I apologize, but I couldn't generate a response."
        except Exception as e:
            print(f"Gemini API error: {str(e)}")
            final_reply = f"I encountered an error while processing your request: {str(e)}. Please try again."
        
        # TODO: Implement full function calling with Gemini's native support
        # For now, this provides conversational AI assistance
        
        # Create or update conversation
        if not conversation_id:
            conversation_id = str(ObjectId())
        
        # Save messages to database
        timestamp = datetime.utcnow()
        
        # Save user message
        support_chat_messages_collection.insert_one({
            "conversationId": conversation_id,
            "userId": user_id,
            "orgId": org_id,
            "role": "user",
            "content": user_message,
            "createdAt": timestamp
        })
        
        # Save assistant reply
        support_chat_messages_collection.insert_one({
            "conversationId": conversation_id,
            "userId": user_id,
            "orgId": org_id,
            "role": "assistant",
            "content": final_reply,
            "createdAt": timestamp
        })
        
        return final_reply, conversation_id
        
    except Exception as e:
        error_msg = f"I encountered an error: {str(e)}. Please try again or contact support."
        print(f"Support AI error: {str(e)}")
        return error_msg, conversation_id or str(ObjectId())


def get_conversation_history(conversation_id: str, user_id: str, org_id: str) -> List[Dict[str, Any]]:
    """Get conversation history from database"""
    messages = list(
        support_chat_messages_collection.find({
            "conversationId": conversation_id,
            "userId": user_id,
            "orgId": org_id
        }).sort("createdAt", 1)
    )
    
    result = []
    for msg in messages:
        result.append({
            "id": str(msg["_id"]),
            "role": msg["role"],
            "content": msg["content"],
            "createdAt": msg["createdAt"].isoformat() if isinstance(msg["createdAt"], datetime) else str(msg["createdAt"])
        })
    
    return result


def get_or_create_conversation_id(user_id: str, org_id: str) -> Optional[str]:
    """Get the most recent conversation ID for a user, or None if no conversation exists"""
    latest = support_chat_messages_collection.find_one(
        {"userId": user_id, "orgId": org_id},
        sort=[("createdAt", -1)]
    )
    
    return latest["conversationId"] if latest else None

