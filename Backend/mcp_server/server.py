"""MCP Server for Lighthouse CRM using FastMCP"""
import sys
import os
from pathlib import Path

# Add the Backend directory to Python path to ensure imports work
# This allows the server to be run from any directory
backend_dir = Path(__file__).parent.parent.absolute()
if str(backend_dir) not in sys.path:
    sys.path.insert(0, str(backend_dir))

from fastmcp import FastMCP

# Initialize FastMCP server
mcp = FastMCP("Lighthouse CRM")


# Import and register tools from modules
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

# Register all tools
mcp.tool()(create_lead)
mcp.tool()(get_leads)
mcp.tool()(update_lead_status)
mcp.tool()(convert_lead_to_deal)
mcp.tool()(delete_lead)

mcp.tool()(create_deal)
mcp.tool()(get_deals)
mcp.tool()(update_deal)
mcp.tool()(delete_deal)

mcp.tool()(create_ticket)
mcp.tool()(get_tickets)
mcp.tool()(get_ticket)
mcp.tool()(update_ticket)
mcp.tool()(get_assignable_employees)

mcp.tool()(create_meeting)
mcp.tool()(get_meetings)

mcp.tool()(get_organizations)
mcp.tool()(create_organization)
mcp.tool()(update_organization)
mcp.tool()(get_employees)
mcp.tool()(create_employee)
mcp.tool()(update_employee)
mcp.tool()(remove_employee)
mcp.tool()(get_roles)
mcp.tool()(create_role)
mcp.tool()(update_role)
mcp.tool()(delete_role)
mcp.tool()(get_tenants)
mcp.tool()(switch_tenant)


if __name__ == "__main__":
    # Run the MCP server
    mcp.run()

