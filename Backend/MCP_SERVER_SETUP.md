# MCP Server Setup for Lighthouse CRM

This guide explains how to set up and use the MCP (Model Context Protocol) server for Lighthouse CRM with Claude Desktop.

## Overview

The MCP server provides a bridge between Claude Desktop, web apps, Android apps, and the Lighthouse CRM FastAPI backend. It exposes tools for managing:

- **Leads**: Create, read, update status, convert to deals, delete
- **Deals**: Create, read, update, delete
- **Tickets**: Create, read, update, manage assignments
- **Calendar**: Create meetings, list upcoming events
- **Administration**: Manage organizations, employees, and roles

## Architecture

```
Claude Desktop / Web App / Android App
         ↓
    MCP Server (FastMCP)
         ↓
   FastAPI Backend
         ↓
   MongoDB / Jira / Google Calendar
```

## Prerequisites

1. Python 3.8+ installed
2. Lighthouse CRM backend running (FastAPI server)
3. Claude Desktop app installed
4. Firebase authentication token (for authenticated operations)

## Installation

1. **Install dependencies** (if not already installed):

```bash
cd Backend
pip install fastmcp httpx
```

Or add to `requirements.txt`:
```
fastmcp>=0.9.0
httpx>=0.25.0
```

2. **Configure environment variables** (optional):

The MCP server will use the backend API at `http://localhost:3000` by default. To change this, set:

```bash
export CRM_API_BASE_URL=http://your-backend-url:port
```

## Running the MCP Server

### Standalone Mode (for testing)

**Option 1: Using the module (recommended)**
```bash
cd Backend
python -m mcp_server.server
```

**Option 2: Using the entry script**
```bash
cd Backend
python run_mcp_server.py
```

The server will start and listen for MCP protocol connections via stdio.

### With Claude Desktop

The MCP server is designed to be used with Claude Desktop via stdio transport.

## Claude Desktop Configuration

To configure Claude Desktop to use the Lighthouse CRM MCP server:

1. **Locate Claude Desktop configuration file**:

   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
   - **Linux**: `~/.config/Claude/claude_desktop_config.json`

2. **Edit the configuration file**:

Add the following MCP server configuration:

**Option 1: Using the module (recommended)**
```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "python",
      "args": [
        "-m",
        "mcp_server.server"
      ],
      "cwd": "/path/to/Lighthouse-CRM/Backend",
      "env": {
        "CRM_API_BASE_URL": "http://localhost:3000"
      }
    }
  }
}
```

**Option 2: Using the entry script (alternative)**
```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "python",
      "args": [
        "run_mcp_server.py"
      ],
      "cwd": "/path/to/Lighthouse-CRM/Backend",
      "env": {
        "CRM_API_BASE_URL": "http://localhost:3000"
      }
    }
  }
}
```

**Important**: Replace `/path/to/Lighthouse-CRM/Backend` with the actual path to your Backend directory.

### Windows Configuration Example

**Using the module:**
```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "python",
      "args": [
        "-m",
        "mcp_server.server"
      ],
      "cwd": "C:\\Users\\YourUsername\\Lighthouse-CRM\\Backend",
      "env": {
        "CRM_API_BASE_URL": "http://localhost:3000"
      }
    }
  }
}
```

**Or using the entry script:**
```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "python",
      "args": [
        "run_mcp_server.py"
      ],
      "cwd": "C:\\Users\\YourUsername\\Lighthouse-CRM\\Backend",
      "env": {
        "CRM_API_BASE_URL": "http://localhost:3000"
      }
    }
  }
}
```

### Using Virtual Environment

If you're using a virtual environment, update the `command` to point to the Python executable in your venv:

**Using the module:**
```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "C:\\Users\\YourUsername\\Lighthouse-CRM\\Backend\\winvenv\\Scripts\\python.exe",
      "args": [
        "-m",
        "mcp_server.server"
      ],
      "cwd": "C:\\Users\\YourUsername\\Lighthouse-CRM\\Backend",
      "env": {
        "CRM_API_BASE_URL": "http://localhost:3000"
      }
    }
  }
}
```

**Or using the entry script:**
```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "C:\\Users\\YourUsername\\Lighthouse-CRM\\Backend\\winvenv\\Scripts\\python.exe",
      "args": [
        "run_mcp_server.py"
      ],
      "cwd": "C:\\Users\\YourUsername\\Lighthouse-CRM\\Backend",
      "env": {
        "CRM_API_BASE_URL": "http://localhost:3000"
      }
    }
  }
}
```

## Authentication

Most tools require a Firebase authentication token. When using the MCP server:

1. **For Claude Desktop**: You'll need to provide the `auth_token` parameter when calling tools that require authentication.

2. **For Web/Android Apps**: The apps should pass the Firebase token they receive from authentication.

3. **Public Endpoints**: The `create_ticket` tool does not require authentication (it's a public endpoint for customers to submit tickets).

## Available Tools

### Leads Management

- `create_lead`: Create a new lead
- `get_leads`: Get all leads (with pagination)
- `update_lead_status`: Update lead status
- `convert_lead_to_deal`: Convert a lead to Account, Contact, and Deal
- `delete_lead`: Delete a lead

### Deals Management

- `create_deal`: Create a new deal
- `get_deals`: Get all deals (with pagination)
- `update_deal`: Update deal information
- `delete_deal`: Delete a deal

### Tickets Management

- `create_ticket`: Create a support ticket (public, no auth required)
- `get_tickets`: Get all tickets (with filters)
- `get_ticket`: Get a single ticket by ID
- `update_ticket`: Update ticket status, priority, or assignment
- `get_assignable_employees`: Get list of employees who can be assigned to tickets

### Calendar Management

- `create_meeting`: Create a calendar meeting/event
- `get_meetings`: Get upcoming meetings from Google Calendar

### Administration

**Organizations:**
- `get_organizations`: Get all organizations
- `create_organization`: Create a new organization
- `update_organization`: Update organization (admin only)

**Employees:**
- `get_employees`: Get all employees in an organization (admin only)
- `create_employee`: Add employee to organization (admin only)
- `update_employee`: Update employee roles/name (admin only)
- `remove_employee`: Remove employee from organization (admin only)

**Roles:**
- `get_roles`: Get all roles in an organization
- `create_role`: Create a new role (admin only)
- `update_role`: Update role permissions (admin only)
- `delete_role`: Delete a role (admin only)

## Usage Examples

### Example: Creating a Lead via Claude Desktop

When using Claude Desktop, you can ask Claude to create a lead:

```
"Create a new lead for John Doe with email john@example.com from the website"
```

Claude will use the `create_lead` tool with appropriate parameters.

### Example: Getting Tickets

```
"Show me all open tickets with high priority"
```

Claude will use `get_tickets` with `status="open"` and `priority="high"`.

### Example: Creating a Meeting

```
"Schedule a meeting tomorrow at 2 PM with alice@example.com about the Q4 project"
```

Claude will use `create_meeting` with the appropriate parameters.

## Troubleshooting

### MCP Server Not Starting

1. **Check Python path**: Ensure Python is in your PATH or use the full path in the config
2. **Check dependencies**: Run `pip install fastmcp httpx`
3. **Check backend**: Ensure the FastAPI backend is running on the configured port
4. **Check logs**: Look for error messages in Claude Desktop's console/logs
5. **Check working directory**: Ensure `cwd` in the config points to the `Backend` directory (not the root)
6. **Module not found error**: If you see `ModuleNotFoundError: No module named 'mcp_server'`, try using `run_mcp_server.py` instead of `-m mcp_server.server`

### Authentication Errors

- Ensure you're providing a valid Firebase authentication token
- Check that the token hasn't expired
- Verify the user has the necessary permissions for the operation

### Connection Errors

- Verify `CRM_API_BASE_URL` is correct
- Ensure the FastAPI backend is accessible
- Check firewall settings if using a remote backend

### Import Errors

- Ensure you're running from the correct directory (Backend folder)
- Check that all Python paths are correct
- Verify the `mcp_server` module is accessible

## Development

To modify or extend the MCP server:

1. **Add new tools**: Create functions in the appropriate tool module (`mcp_server/tools/`)
2. **Register tools**: Add the tool registration in `mcp_server/server.py`
3. **Test locally**: Run `python -m mcp_server.server` to test

## Notes

- The MCP server uses stdio transport for communication with Claude Desktop
- All API calls are made asynchronously using `httpx`
- The server automatically handles authentication headers when tokens are provided
- Public endpoints (like `create_ticket`) don't require authentication tokens

## Support

For issues or questions:
1. Check the FastAPI backend logs
2. Verify the backend endpoints are working directly
3. Test the MCP server standalone mode
4. Review Claude Desktop logs for MCP server errors

