"""MCP server implementation for Lighthouse CRM"""
import asyncio
import sys
from pathlib import Path

# Add parent directory to path so we can import mcp-crm modules
backend_dir = Path(__file__).parent.parent
sys.path.insert(0, str(backend_dir))

from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent

# Import from mcp-crm using relative path workaround
# Since mcp-crm has a hyphen, we import directly from the files
import importlib.util
spec = importlib.util.spec_from_file_location("mcp_crm_config", backend_dir / "mcp-crm" / "config.py")
mcp_crm_config = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mcp_crm_config)

spec = importlib.util.spec_from_file_location("mcp_crm_tools", backend_dir / "mcp-crm" / "tools" / "__init__.py")
mcp_crm_tools = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mcp_crm_tools)

MCP_SERVER_NAME = mcp_crm_config.MCP_SERVER_NAME
MCP_SERVER_VERSION = mcp_crm_config.MCP_SERVER_VERSION
get_all_tools = mcp_crm_tools.get_all_tools
TOOL_HANDLERS = mcp_crm_tools.TOOL_HANDLERS


# Create MCP server instance
server = Server(MCP_SERVER_NAME)


@server.list_tools()
async def handle_list_tools() -> list[Tool]:
    """List all available tools"""
    return get_all_tools()


@server.call_tool()
async def handle_call_tool(name: str, arguments: dict) -> list[TextContent]:
    """
    Handle tool calls from MCP client.
    
    Args:
        name: Tool name
        arguments: Tool arguments (includes context for authentication)
    
    Returns:
        List of TextContent responses
    """
    try:
        # Get tool handler
        handler = TOOL_HANDLERS.get(name)
        if not handler:
            return [TextContent(
                type="text",
                text=f"Tool '{name}' not found. Available tools: {', '.join(TOOL_HANDLERS.keys())}"
            )]
        
        # Extract context from arguments if present, otherwise use empty dict
        context = arguments.pop("context", {})
        arguments["context"] = context
        
        # Call the handler (all handlers are async)
        result = await handler(**arguments)
        
        # Return result as TextContent
        return [TextContent(
            type="text",
            text=str(result)
        )]
    except Exception as e:
        error_msg = f"Error executing tool '{name}': {str(e)}"
        print(f"MCP Error: {error_msg}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return [TextContent(
            type="text",
            text=error_msg
        )]


async def run_server():
    """Run the MCP server with stdio transport"""
    async with stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            server.create_initialization_options()
        )


if __name__ == "__main__":
    # Run the server
    asyncio.run(run_server())

