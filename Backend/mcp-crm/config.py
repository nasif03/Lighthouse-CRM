"""MCP server configuration"""
import os
from dotenv import load_dotenv

load_dotenv()

# MCP Server Configuration
MCP_SERVER_PORT = int(os.getenv("MCP_SERVER_PORT", 3001))
MCP_SERVER_HOST = os.getenv("MCP_SERVER_HOST", "0.0.0.0")

# MCP Transport Configuration
# Options: "stdio" (for local development with Claude Desktop) or "http" (for production)
MCP_TRANSPORT = os.getenv("MCP_TRANSPORT", "stdio")

# Server Name
MCP_SERVER_NAME = "Lighthouse CRM MCP Server"
MCP_SERVER_VERSION = "1.0.0"

# Authentication
# MCP will use the same Firebase authentication as FastAPI
# Set to False for development/testing (no token required)
REQUIRE_AUTH = os.getenv("MCP_REQUIRE_AUTH", "false").lower() == "true"

# Development: Specify which user email to use when auth is disabled
# Leave empty to auto-select first user with valid orgId
MCP_DEV_USER_EMAIL = os.getenv("MCP_DEV_USER_EMAIL", "")

