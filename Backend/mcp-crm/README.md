# Lighthouse CRM MCP Server

Model Context Protocol (MCP) integration for Lighthouse CRM, enabling AI assistants to interact with the CRM through natural language commands.

## Overview

The MCP server exposes Lighthouse CRM operations as AI-accessible tools, allowing AI assistants (like Claude, GPT) to:
- Create and manage leads, deals, contacts, and accounts
- View dashboard statistics and analytics
- Send emails via Gmail
- Create Jira issues from tickets

## Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Ensure your environment variables are set (see `config/settings.py`)

3. Authenticate Gmail (if using Gmail tools):
   - Use the CRM interface to authenticate Gmail
   - Or follow instructions in `GMAIL_SETUP.md`

## Running the MCP Server

### Option 1: Standalone Server (Recommended for Development)

Run the MCP server as a separate process:

```bash
cd Lighthouse-CRM/Backend
python -m mcp.server
```

The server will communicate via stdio (standard input/output), which is compatible with Claude Desktop and other MCP clients.

### Option 2: Integration with FastAPI (Production)

You can integrate the MCP server into your FastAPI application by adding it as a background task or separate endpoint.

## Configuration

MCP server configuration is in `mcp/config.py`:

- `MCP_SERVER_PORT`: Port for HTTP transport (default: 3001)
- `MCP_SERVER_HOST`: Host address (default: 0.0.0.0)
- `MCP_TRANSPORT`: Transport type - "stdio" or "http" (default: "stdio")
- `REQUIRE_AUTH`: Whether authentication is required (default: true)

## Available Tools

### Lead Management
- `create_lead`: Create a new lead
- `get_leads`: Get/search leads
- `update_lead_status`: Update lead status
- `convert_lead`: Convert lead to account, contact, and deal

### Deal Management
- `create_deal`: Create a new deal
- `get_deals`: Get/search deals
- `update_deal`: Update deal information
- `update_deal_stage`: Move deal through pipeline

### Contact Management
- `create_contact`: Create a new contact
- `get_contacts`: Get/search contacts
- `update_contact`: Update contact information

### Account Management
- `create_account`: Create a new account
- `get_accounts`: Get/search accounts
- `update_account`: Update account information

### Support Tickets
- `create_ticket`: Create a support ticket
- `get_tickets`: Get/search tickets
- `update_ticket_status`: Update ticket status

### Dashboard & Analytics
- `get_dashboard_stats`: Get dashboard statistics
- `get_analytics`: Get analytics for a timeframe

### Gmail Integration
- `send_email`: Send email via Gmail
- `get_emails`: Search Gmail messages
- `create_lead_from_email`: Extract lead from email

### Jira Integration
- `create_jira_issue_from_ticket`: Create Jira issue from ticket
- `get_jira_issues_for_project`: Get Jira issues
- `sync_ticket_to_jira`: Sync ticket to Jira

## Authentication

The MCP server uses the same Firebase authentication as the FastAPI backend. Authentication tokens should be provided in the MCP request context:

- Via headers: `Authorization: Bearer <token>`
- Via metadata: `metadata.token`

## Usage Examples

### With Claude Desktop

1. Add to Claude Desktop configuration (`~/Library/Application Support/Claude/claude_desktop_config.json` on Mac):

```json
{
  "mcpServers": {
    "lighthouse-crm": {
      "command": "python",
      "args": ["-m", "mcp.server"],
      "cwd": "/path/to/Lighthouse-CRM/Backend"
    }
  }
}
```

2. Restart Claude Desktop

3. Use natural language to interact with the CRM:
   - "Create a lead for John Doe, email john@example.com, from LinkedIn"
   - "Show me all deals in the prospecting stage"
   - "Convert the top 3 qualified leads to deals"
   - "Send an email to john@example.com about our new product"

## Architecture

```
MCP Client (AI Assistant)
    ↓
MCP Server (mcp/server.py)
    ↓
MCP Tools (mcp/tools/*.py)
    ↓
Existing Services (services/*.py)
    ↓
Database (MongoDB)
```

The MCP tools wrap existing FastAPI route functions and services, ensuring:
- No code duplication
- Consistent business logic
- Multi-tenant data isolation
- Proper authentication and authorization

## Development

### Adding New Tools

1. Create tool function in appropriate `mcp/tools/*.py` file
2. Add tool definition to `mcp/tools/__init__.py` in `get_all_tools()`
3. Add handler mapping to `TOOL_HANDLERS` in `mcp/tools/__init__.py`
4. Test the tool with MCP client

### Testing

Test individual tools:
```python
from mcp.tools.leads import create_lead

result = await create_lead(
    name="Test Lead",
    email="test@example.com",
    source="website",
    context={"metadata": {"token": "your-firebase-token"}}
)
print(result)
```

## Troubleshooting

### Authentication Errors
- Ensure Firebase token is valid
- Check that user exists in database
- Verify user belongs to an organization

### Gmail Errors
- Authenticate Gmail through CRM interface first
- Check Gmail API credentials

### Jira Errors
- Ensure Jira project is created for organization
- Verify Jira credentials in settings

## Security Considerations

- All tools respect multi-tenant data isolation
- User can only access their organization's data
- Authentication is required for all operations
- Input validation is performed on all tool arguments

## License

Same as Lighthouse CRM project.

