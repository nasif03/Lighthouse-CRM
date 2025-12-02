"""Support AI service using Gemini API with MCP tool integration"""
import os
import json
import re
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
    get_tenants,
    switch_tenant,
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
    "get_tenants": get_tenants,
    "switch_tenant": switch_tenant,
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
    
    "get_tenants": [],  # User can see their own tenants
    "switch_tenant": [],  # User can switch their own tenant
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
    
    # Use the first paragraph of the docstring (up to first double newline)
    description = doc.split("\n\n")[0] if doc else f"{tool_name} function"
    # If description is very short, try to get more context from the docstring
    if len(description) < 50 and doc:
        # Get first two sentences or first paragraph, whichever is longer
        sentences = doc.split('. ')
        if len(sentences) > 1:
            extended_desc = '. '.join(sentences[:2])
            if len(extended_desc) > len(description):
                description = extended_desc + ('.' if not extended_desc.endswith('.') else '')
    
    return {
        "name": tool_name,
        "description": description,
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
    Implements full function calling: Gemini can call tools, we execute them, and feed results back.
    
    Returns:
        tuple: (reply_text, conversation_id)
    """
    # Load conversation history
    conversation_history = []
    if conversation_id:
        messages = list(
            support_chat_messages_collection.find(
                {"conversationId": conversation_id}
            ).sort("createdAt", 1)
        )
        for msg in messages:
            # Skip JSON tool calls in old conversation history (they're internal only)
            content = msg.get("content", "")
            if content:
                stripped = content.strip()
                # If this message is a JSON tool call, skip it
                if (stripped.startswith('{"tool_call"') or 
                    (stripped.startswith('{') and '"name"' in stripped and '"args"' in stripped and len(stripped) < 500)):
                    continue  # Skip this message - it's an internal tool call, not user-facing
            
            # Convert to Gemini message format
            role = "user" if msg["role"] == "user" else "model"
            conversation_history.append({
                "role": role,
                "parts": [{"text": msg["content"]}]
            })
    
    # Generate tool schemas for available tools (filter by permissions)
    # Priority tools that should always be included (common/important operations)
    priority_tools = ["get_tenants", "switch_tenant", "get_leads", "get_tickets", "get_deals"]
    
    available_tools = []
    function_declarations = []
    
    # First, add priority tools
    for tool_name in priority_tools:
        if tool_name in MCP_TOOLS and check_tool_permission(tool_name, user_doc, org_id):
            tool_func = MCP_TOOLS[tool_name]
            schema = get_tool_schema(tool_name, tool_func)
            available_tools.append(tool_name)
            function_declarations.append(schema)
    
    # Then add remaining tools (excluding already added priority tools)
    for tool_name in MCP_TOOLS.keys():
        if tool_name not in priority_tools and check_tool_permission(tool_name, user_doc, org_id):
            tool_func = MCP_TOOLS[tool_name]
            schema = get_tool_schema(tool_name, tool_func)
            available_tools.append(tool_name)
            function_declarations.append(schema)
    
    # System instruction
    system_instruction = """You are Support AI, an intelligent assistant for Lighthouse CRM. 
You can help users with:
- CRM operations (leads, deals, contacts, accounts)
- Jira/JSM ticket management
- Calendar and meeting scheduling
- Organization and employee management
- Organization/tenant context: Use get_tenants when users ask about their current organization, which organization they're in, or want to see their organizations. Use switch_tenant when users want to change their active organization.
- General questions about the system

When users ask you to perform actions, use the available function calling tools. Always explain what you're doing in a friendly, helpful manner.
If a user doesn't have permission for a requested action, politely explain that you cannot perform that action due to permissions.

For organization-related questions:
- "Which organization am I in?" → Use get_tenants
- "What organizations do I belong to?" → Use get_tenants
- "Switch to organization X" → First use get_tenants to show options, then use switch_tenant with the tenant_id
- "Change my organization" → Use get_tenants first, then switch_tenant

Be concise but thorough. Format responses clearly with proper structure."""
    
    try:
        # Create or update conversation
        if not conversation_id:
            conversation_id = str(ObjectId())
        
        # Add user message to conversation
        conversation_history.append({
            "role": "user",
            "parts": [{"text": user_message}]
        })
        
        # Save user message to database
        timestamp = datetime.utcnow()
        support_chat_messages_collection.insert_one({
            "conversationId": conversation_id,
            "userId": user_id,
            "orgId": org_id,
            "role": "user",
            "content": user_message,
            "createdAt": timestamp
        })
        
        # Prepare tools for Gemini (function declarations)
        # Note: The Google GenAI SDK may not support 'tools' parameter directly
        # We'll use a hybrid approach: describe tools in prompt and use structured output
        
        # Maximum iterations to prevent infinite loops
        max_iterations = 5
        iteration = 0
        final_reply = None
        
        while iteration < max_iterations:
            iteration += 1
            
            try:
                # Build tools description for the prompt
                tools_description = ""
                if function_declarations:
                    tools_description = "\n\nAvailable tools you can use:\n"
                    for tool_schema in function_declarations[:20]:  # Increased limit to include more tools
                        tool_name = tool_schema.get("name", "")
                        tool_desc = tool_schema.get("description", "")
                        params = tool_schema.get("parameters", {}).get("properties", {})
                        required = tool_schema.get("parameters", {}).get("required", [])
                        
                        tools_description += f"\n- {tool_name}: {tool_desc}\n"
                        if params:
                            tools_description += "  Parameters:\n"
                            for param_name, param_info in params.items():
                                param_type = param_info.get("type", "string")
                                param_desc = param_info.get("description", "")
                                req_marker = " (required)" if param_name in required else ""
                                tools_description += f"    - {param_name} ({param_type}){req_marker}: {param_desc}\n"
                    
                    tools_description += "\nWhen you want to use a tool, respond in this exact JSON format:\n"
                    tools_description += '{"tool_call": {"name": "tool_name", "args": {"param1": "value1", "param2": "value2"}}}\n'
                    tools_description += "IMPORTANT: After tool execution results are provided, you MUST respond in plain, readable text - NOT in JSON format. Always explain results in a conversational, user-friendly way.\n"
                    tools_description += "If you are not using a tool, respond normally with text.\n"
                
                # Build conversation text from history
                conversation_text = ""
                for msg in conversation_history:
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
                    elif role == "model" or role == "assistant":
                        conversation_text += f"Assistant: {text}\n"
                
                # Build full prompt with system instruction, tools, and conversation embedded
                full_prompt = f"""{system_instruction}

{tools_description}

{conversation_text}"""
                
                # Call Gemini with simple parameters only (no system_instruction or tools kwargs)
                response = gemini_client.models.generate_content(
                    model="gemini-2.0-flash",
                    contents=full_prompt
                )
                
                # Extract response text
                response_text = ""
                if hasattr(response, "text") and response.text:
                    response_text = response.text
                elif hasattr(response, "candidates") and response.candidates:
                    candidate = response.candidates[0]
                    if hasattr(candidate, "content") and hasattr(candidate.content, "parts"):
                        text_parts = []
                        for part in candidate.content.parts:
                            if hasattr(part, "text"):
                                text_parts.append(part.text)
                        response_text = " ".join(text_parts) if text_parts else ""
                
                # Strip markdown code blocks from response (Gemini sometimes wraps JSON in ```json blocks)
                # Remove markdown code blocks (```json ... ``` or ``` ... ```)
                response_text = re.sub(r'```json\s*\n?(.*?)\n?```', r'\1', response_text, flags=re.DOTALL)
                response_text = re.sub(r'```\s*\n?(.*?)\n?```', r'\1', response_text, flags=re.DOTALL)
                response_text = response_text.strip()
                
                # Try to parse JSON tool calls from response
                function_calls = []
                try:
                    # Look for JSON blocks in the response
                    # Try to find JSON starting with { and ending with }
                    # We'll try to extract the tool_call JSON
                    # Handle multiple formats:
                    # 1. {"tool_call": {...}}
                    # 2. {"name": "...", "args": {...}}
                    # 3. tool_call\n{...} (text before JSON)
                    json_start = response_text.find('{"tool_call"')
                    if json_start == -1:
                        json_start = response_text.find('{"name"')
                    # Also check for JSON after "tool_call" text (common in Telegram)
                    if json_start == -1:
                        tool_call_text_pos = response_text.lower().find('tool_call')
                        if tool_call_text_pos != -1:
                            # Look for { after "tool_call" text
                            potential_start = response_text.find('{', tool_call_text_pos)
                            if potential_start != -1:
                                json_start = potential_start
                    
                    if json_start != -1:
                        # Find the matching closing brace
                        brace_count = 0
                        json_end = json_start
                        for i in range(json_start, len(response_text)):
                            if response_text[i] == '{':
                                brace_count += 1
                            elif response_text[i] == '}':
                                brace_count -= 1
                                if brace_count == 0:
                                    json_end = i + 1
                                    break
                        
                        if json_end > json_start:
                            json_str = response_text[json_start:json_end]
                            tool_call_json = json.loads(json_str)
                            
                            # Check if it's wrapped in tool_call
                            if "tool_call" in tool_call_json:
                                function_calls.append(tool_call_json["tool_call"])
                            # Or if it's directly a tool call object with name and args
                            elif "name" in tool_call_json and "args" in tool_call_json:
                                function_calls.append(tool_call_json)
                except (json.JSONDecodeError, KeyError, ValueError) as e:
                    print(f"[Support AI] Could not parse tool call JSON: {str(e)}")
                    # Even if parsing fails, if we detected JSON-like structure, don't return it
                    if '{"tool_call"' in response_text or ('{"name"' in response_text and '"args"' in response_text):
                        print(f"[Support AI] Detected JSON tool call format but couldn't parse - will skip")
                        function_calls = ["_skip_this_response"]  # Marker to skip this response
                    pass
                
                # If there are function calls, execute them
                if function_calls:
                    # Check if this is a skip marker
                    if function_calls == ["_skip_this_response"]:
                        if iteration < max_iterations:
                            # Add a message telling Gemini not to use JSON
                            conversation_history.append({
                                "role": "user",
                                "parts": [{"text": "Your previous response contained JSON format which cannot be shown to users. Please respond in plain, readable text only. Do NOT use JSON or code blocks. Just explain what you're doing in natural language."}]
                            })
                            continue
                        else:
                            final_reply = "I'm having trouble processing that request. Please try rephrasing your question."
                            break
                    
                    # Strip any JSON tool call from response_text before proceeding
                    # This prevents text like "Okay, I will... {tool_call: {...}}" from being saved
                    if response_text:
                        # Remove JSON tool call patterns from response_text
                        response_text = re.sub(r'\{[^}]*"tool_call"[^}]*\}|"tool_call"[^}]*\}', '', response_text, flags=re.DOTALL)
                        response_text = re.sub(r'\{[^}]*"name"[^}]*"args"[^}]*\}', '', response_text, flags=re.DOTALL)
                        response_text = response_text.strip()
                    
                    # DO NOT add the JSON tool call to conversation history - it's internal only
                    # We'll only add the final readable response
                    
                    # Execute each function call
                    function_results = []
                    for func_call in function_calls:
                        # Parse function name and arguments from JSON format
                        func_name = None
                        func_args = {}
                        
                        if isinstance(func_call, dict):
                            func_name = func_call.get("name") or func_call.get("function_name", "")
                            func_args = func_call.get("args", func_call.get("arguments", {}))
                        elif hasattr(func_call, "name"):
                            func_name = func_call.name
                            if hasattr(func_call, "args"):
                                func_args = func_call.args if isinstance(func_call.args, dict) else {}
                            elif hasattr(func_call, "arguments"):
                                func_args = func_call.arguments if isinstance(func_call.arguments, dict) else {}
                        
                        if not func_name:
                            print(f"[Support AI] Warning: Could not parse function name from {func_call}")
                            continue
                        
                        if func_name not in available_tools:
                            result = {
                                "name": func_name,
                                "response": {"error": f"Unknown tool: {func_name}"}
                            }
                        else:
                            try:
                                # Execute the tool
                                tool_result = await call_mcp_tool(func_name, func_args, auth_token)
                                
                                # Convert result to JSON-serializable format
                                if isinstance(tool_result, (dict, list, str, int, float, bool, type(None))):
                                    result_data = tool_result
                                else:
                                    result_data = str(tool_result)
                                
                                result = {
                                    "name": func_name,
                                    "response": result_data
                                }
                                print(f"[Support AI] Executed tool {func_name} successfully")
                            except Exception as e:
                                print(f"[Support AI] Error executing tool {func_name}: {str(e)}")
                                
                                # Check if it's an authentication error (401/403)
                                error_str = str(e).lower()
                                if "401" in error_str or "unauthorized" in error_str or "authentication" in error_str:
                                    error_msg = "Authentication failed. Your session may have expired. Please log in again."
                                elif "403" in error_str or "forbidden" in error_str or "permission" in error_str:
                                    error_msg = "You don't have permission to perform this action."
                                else:
                                    error_msg = f"Tool execution failed: {str(e)}"
                                
                                result = {
                                    "name": func_name,
                                    "response": {"error": error_msg}
                                }
                        
                        function_results.append(result)
                    
                    # Format function results as text for Gemini to understand
                    results_text = "Tool execution results:\n"
                    for fr in function_results:
                        func_name = fr.get("name", "unknown") if isinstance(fr, dict) else "unknown"
                        func_response = fr.get("response", {}) if isinstance(fr, dict) else fr
                        
                        # Format the result nicely
                        if isinstance(func_response, dict):
                            if "error" in func_response:
                                results_text += f"\n{func_name}: ERROR - {func_response['error']}\n"
                            else:
                                results_text += f"\n{func_name}: SUCCESS\n{json.dumps(func_response, indent=2)}\n"
                        else:
                            results_text += f"\n{func_name}: {json.dumps(func_response)}\n"
                    
                    # IMPORTANT: Explicitly tell Gemini to respond in plain text, NOT JSON
                    results_text += "\n\nIMPORTANT: Respond to the user in plain, readable text. Do NOT use JSON format. Explain the results in a friendly, conversational way."
                    
                    # Add function results back to conversation as user message
                    conversation_history.append({
                        "role": "user",
                        "parts": [{"text": results_text}]
                    })
                    
                    # Continue loop to get final response from Gemini
                    continue
                
                # No function calls - use the response text as final reply
                # But first, check if it's JSON and strip it if so
                if response_text:
                    # Check if response contains JSON tool call format (even if not parsed)
                    stripped_text = response_text.strip()
                    # Check for JSON tool call patterns (with or without markdown - already stripped above)
                    # More aggressive detection: look for JSON anywhere in the response
                    has_json_tool_call = (
                        '{"tool_call"' in stripped_text or 
                        ('{"name"' in stripped_text and '"args"' in stripped_text) or
                        stripped_text.startswith('{"tool_call"') or
                        (stripped_text.startswith('{') and '"name"' in stripped_text and '"args"' in stripped_text) or
                        # Also check for JSON-like patterns in the middle of text
                        ('"tool_call"' in stripped_text and '"name"' in stripped_text) or
                        (stripped_text.count('{') >= 1 and '"name"' in stripped_text and '"args"' in stripped_text)
                    )
                    
                    if has_json_tool_call:
                        # This looks like a JSON tool call that wasn't parsed - skip it
                        print(f"[Support AI] Warning: Response contains JSON tool call format, skipping: {stripped_text[:100]}")
                        # Try one more iteration with stronger instruction
                        if iteration < max_iterations:
                            # Add a very strong instruction to not use JSON
                            conversation_history.append({
                                "role": "user",
                                "parts": [{"text": f"CRITICAL: Your response contained JSON code which users cannot see. You MUST respond ONLY in plain, natural language text. Do NOT use JSON, code blocks, or any structured format. Just write a normal sentence explaining what happened. This is attempt {iteration + 1}."}]
                            })
                            continue
                        else:
                            # After max iterations, strip JSON and return what's left, or a fallback message
                            # Try to extract any non-JSON text before/after the JSON
                            parts = re.split(r'\{[^}]*"tool_call"[^}]*\}|"tool_call"[^}]*\}', stripped_text)
                            non_json_parts = [p.strip() for p in parts if p.strip() and not p.strip().startswith('{')]
                            if non_json_parts:
                                final_reply = ' '.join(non_json_parts)
                            else:
                                final_reply = "I processed your request, but I'm having trouble formatting the response. Please try asking again."
                            break
                    else:
                        final_reply = response_text
                        break
                else:
                    # If no text response, try to extract from response object
                    if hasattr(response, "text") and response.text:
                        final_reply = response.text
                        break
                    elif hasattr(response, "candidates") and response.candidates:
                        candidate = response.candidates[0]
                        if hasattr(candidate, "content") and hasattr(candidate.content, "parts"):
                            text_parts = []
                            for part in candidate.content.parts:
                                if hasattr(part, "text"):
                                    text_parts.append(part.text)
                            final_reply = " ".join(text_parts) if text_parts else None
                            if final_reply:
                                break
                    
            except Exception as e:
                print(f"[Support AI] Gemini API error (iteration {iteration}): {str(e)}")
                if iteration == 1:
                    # First iteration failed - return error
                    final_reply = f"I encountered an error while processing your request: {str(e)}. Please try again."
                break
        
        # If we still don't have a reply after all iterations
        if not final_reply:
            final_reply = "I apologize, but I couldn't generate a response. Please try rephrasing your question."
        
        # Final safeguard: Strip any JSON tool call format that might have leaked through
        if final_reply:
            stripped = final_reply.strip()
            # If the entire response is just a JSON tool call, replace it with a helpful message
            if (stripped.startswith('{"tool_call"') or 
                (stripped.startswith('{') and '"name"' in stripped and '"args"' in stripped and len(stripped) < 500)):
                # This looks like a JSON tool call - replace with helpful message
                final_reply = "I processed your request, but I'm having trouble formatting the response. Please try asking again or rephrasing your question."
        
        # Save assistant reply to database
        support_chat_messages_collection.insert_one({
            "conversationId": conversation_id,
            "userId": user_id,
            "orgId": org_id,
            "role": "assistant",
            "content": final_reply,
            "createdAt": datetime.utcnow()
        })
        
        return final_reply, conversation_id
        
    except Exception as e:
        error_msg = f"I encountered an error: {str(e)}. Please try again or contact support."
        print(f"[Support AI] Error: {str(e)}")
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

