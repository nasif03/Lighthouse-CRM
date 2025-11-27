"""MCP tools registration for Lighthouse CRM"""
import sys
from pathlib import Path
from typing import List

# Add parent directory to path
backend_dir = Path(__file__).parent.parent.parent
sys.path.insert(0, str(backend_dir))

from mcp.types import Tool

# Import all tool modules using importlib for hyphenated directory
import importlib.util

def _load_module(module_name, file_path):
    spec = importlib.util.spec_from_file_location(module_name, file_path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

tools_dir = Path(__file__).parent
leads = _load_module("leads", tools_dir / "leads.py")
deals = _load_module("deals", tools_dir / "deals.py")
contacts = _load_module("contacts", tools_dir / "contacts.py")
accounts = _load_module("accounts", tools_dir / "accounts.py")
tickets = _load_module("tickets", tools_dir / "tickets.py")
dashboard = _load_module("dashboard", tools_dir / "dashboard.py")
gmail = _load_module("gmail", tools_dir / "gmail.py")
jira = _load_module("jira", tools_dir / "jira.py")


def get_all_tools() -> List[Tool]:
    """
    Get all registered MCP tools.
    
    Returns:
        List of Tool objects for MCP server
    """
    tools = []
    
    # Lead management tools
    tools.extend([
        Tool(
            name="create_lead",
            description="Create a new lead in the CRM. Use this when a user wants to add a new potential customer.",
            inputSchema={
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Full name of the lead"},
                    "email": {"type": "string", "description": "Email address"},
                    "source": {"type": "string", "description": "Lead source (e.g., website, linkedin, referral)", "default": "manual"},
                    "status": {"type": "string", "description": "Lead status (new, contacted, qualified, converted, lost)", "default": "new"},
                    "phone": {"type": "string", "description": "Phone number (optional)"},
                    "firstName": {"type": "string", "description": "First name (optional, will be parsed from name if not provided)"},
                    "lastName": {"type": "string", "description": "Last name (optional, will be parsed from name if not provided)"}
                },
                "required": ["name", "email"]
            }
        ),
        Tool(
            name="get_leads",
            description="Get leads from the CRM. Use this to search, filter, or list leads.",
            inputSchema={
                "type": "object",
                "properties": {
                    "filters": {"type": "string", "description": "Filter string (e.g., 'status:new,source:website')"},
                    "limit": {"type": "integer", "description": "Maximum number of leads to return", "default": 50}
                }
            }
        ),
        Tool(
            name="update_lead_status",
            description="Update the status of a lead (e.g., from 'new' to 'contacted' or 'qualified').",
            inputSchema={
                "type": "object",
                "properties": {
                    "lead_id": {"type": "string", "description": "ID of the lead to update"},
                    "status": {"type": "string", "description": "New status (new, contacted, qualified, converted, lost)"}
                },
                "required": ["lead_id", "status"]
            }
        ),
        Tool(
            name="convert_lead",
            description="Convert a lead to Account, Contact, and Deal. Use this when a lead is ready to become a customer.",
            inputSchema={
                "type": "object",
                "properties": {
                    "lead_id": {"type": "string", "description": "ID of the lead to convert"}
                },
                "required": ["lead_id"]
            }
        )
    ])
    
    # Deal management tools
    tools.extend([
        Tool(
            name="create_deal",
            description="Create a new deal in the sales pipeline.",
            inputSchema={
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Deal name"},
                    "account_id": {"type": "string", "description": "Associated account ID (optional)"},
                    "contact_id": {"type": "string", "description": "Associated contact ID (optional)"},
                    "amount": {"type": "number", "description": "Deal amount (optional)"},
                    "stage": {"type": "string", "description": "Deal stage (prospecting, qualification, proposal, negotiation, closed-won, closed-lost)", "default": "prospecting"},
                    "currency": {"type": "string", "description": "Currency code", "default": "USD"},
                    "probability": {"type": "integer", "description": "Win probability percentage (0-100, optional)"},
                    "close_date": {"type": "string", "description": "Expected close date in ISO format (optional)"}
                },
                "required": ["name"]
            }
        ),
        Tool(
            name="get_deals",
            description="Get deals from the CRM. Use this to search, filter, or list deals.",
            inputSchema={
                "type": "object",
                "properties": {
                    "filters": {"type": "string", "description": "Filter string (e.g., 'stageId:prospecting,status:open')"},
                    "limit": {"type": "integer", "description": "Maximum number of deals to return", "default": 50}
                }
            }
        ),
        Tool(
            name="update_deal",
            description="Update a deal's information (name, amount, stage, probability, etc.).",
            inputSchema={
                "type": "object",
                "properties": {
                    "deal_id": {"type": "string", "description": "ID of the deal to update"},
                    "name": {"type": "string", "description": "New deal name (optional)"},
                    "amount": {"type": "number", "description": "New deal amount (optional)"},
                    "stage": {"type": "string", "description": "New deal stage (optional)"},
                    "probability": {"type": "integer", "description": "New win probability (optional)"}
                },
                "required": ["deal_id"]
            }
        ),
        Tool(
            name="update_deal_stage",
            description="Update the stage of a deal (move it through the sales pipeline).",
            inputSchema={
                "type": "object",
                "properties": {
                    "deal_id": {"type": "string", "description": "ID of the deal to update"},
                    "stage": {"type": "string", "description": "New stage (prospecting, qualification, proposal, negotiation, closed-won, closed-lost)"}
                },
                "required": ["deal_id", "stage"]
            }
        )
    ])
    
    # Contact management tools
    tools.extend([
        Tool(
            name="create_contact",
            description="Create a new contact in the CRM.",
            inputSchema={
                "type": "object",
                "properties": {
                    "first_name": {"type": "string", "description": "First name"},
                    "last_name": {"type": "string", "description": "Last name"},
                    "email": {"type": "string", "description": "Email address"},
                    "account_id": {"type": "string", "description": "Associated account ID (optional)"},
                    "phone": {"type": "string", "description": "Phone number (optional)"},
                    "title": {"type": "string", "description": "Job title (optional)"},
                    "tags": {"type": "array", "description": "List of tags (optional)", "items": {"type": "string"}}
                },
                "required": ["first_name", "last_name", "email"]
            }
        ),
        Tool(
            name="get_contacts",
            description="Get contacts from the CRM. Use this to search, filter, or list contacts.",
            inputSchema={
                "type": "object",
                "properties": {
                    "filters": {"type": "string", "description": "Filter string (e.g., 'title:Manager')"},
                    "limit": {"type": "integer", "description": "Maximum number of contacts to return", "default": 50}
                }
            }
        ),
        Tool(
            name="update_contact",
            description="Update a contact's information.",
            inputSchema={
                "type": "object",
                "properties": {
                    "contact_id": {"type": "string", "description": "ID of the contact to update"},
                    "first_name": {"type": "string", "description": "New first name (optional)"},
                    "last_name": {"type": "string", "description": "New last name (optional)"},
                    "email": {"type": "string", "description": "New email (optional)"},
                    "phone": {"type": "string", "description": "New phone (optional)"},
                    "title": {"type": "string", "description": "New title (optional)"}
                },
                "required": ["contact_id"]
            }
        )
    ])
    
    # Account management tools
    tools.extend([
        Tool(
            name="create_account",
            description="Create a new account (company) in the CRM.",
            inputSchema={
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Account/company name"},
                    "domain": {"type": "string", "description": "Website domain (optional)"},
                    "industry": {"type": "string", "description": "Industry type (optional)"},
                    "phone": {"type": "string", "description": "Phone number (optional)"}
                },
                "required": ["name"]
            }
        ),
        Tool(
            name="get_accounts",
            description="Get accounts from the CRM. Use this to search, filter, or list accounts.",
            inputSchema={
                "type": "object",
                "properties": {
                    "filters": {"type": "string", "description": "Filter string (e.g., 'industry:Technology')"},
                    "limit": {"type": "integer", "description": "Maximum number of accounts to return", "default": 50}
                }
            }
        ),
        Tool(
            name="update_account",
            description="Update an account's information.",
            inputSchema={
                "type": "object",
                "properties": {
                    "account_id": {"type": "string", "description": "ID of the account to update"},
                    "name": {"type": "string", "description": "New account name (optional)"},
                    "domain": {"type": "string", "description": "New domain (optional)"},
                    "industry": {"type": "string", "description": "New industry (optional)"},
                    "phone": {"type": "string", "description": "New phone (optional)"}
                },
                "required": ["account_id"]
            }
        )
    ])
    
    # Ticket management tools (JSM Service Requests)
    tools.extend([
        Tool(
            name="create_ticket",
            description="Create a new support ticket in Jira Service Management (JSM). If category is 'bug_report' or 'feature_request', also creates a linked Jira Software issue.",
            inputSchema={
                "type": "object",
                "properties": {
                    "subject": {"type": "string", "description": "Ticket subject"},
                    "description": {"type": "string", "description": "Ticket description"},
                    "name": {"type": "string", "description": "Customer name"},
                    "email": {"type": "string", "description": "Customer email"},
                    "priority": {"type": "string", "description": "Priority level (low, medium, high, urgent)", "default": "medium"},
                    "category": {"type": "string", "description": "Ticket category (optional). If 'bug_report' or 'feature_request', will also create Jira Software issue."},
                    "phone": {"type": "string", "description": "Customer phone (optional)"}
                },
                "required": ["subject", "description", "name", "email"]
            }
        ),
        Tool(
            name="get_tickets",
            description="Get support tickets from Jira Service Management (JSM). Use this to search, filter, or list JSM service requests.",
            inputSchema={
                "type": "object",
                "properties": {
                    "filters": {"type": "string", "description": "Filter string (e.g., 'status:open,priority:high')"},
                    "limit": {"type": "integer", "description": "Maximum number of tickets to return", "default": 50}
                }
            }
        ),
        Tool(
            name="update_ticket_status",
            description="Update the status of a JSM support ticket. ticket_id should be a JSM issue key (e.g., 'SR-123').",
            inputSchema={
                "type": "object",
                "properties": {
                    "ticket_id": {"type": "string", "description": "JSM issue key (e.g., 'SR-123') of the ticket to update"},
                    "status": {"type": "string", "description": "New status (open, in_progress, resolved, closed)"}
                },
                "required": ["ticket_id", "status"]
            }
        )
    ])
    
    # Dashboard and analytics tools
    tools.extend([
        Tool(
            name="get_dashboard_stats",
            description="Get dashboard statistics including lead counts, deal values, conversion rates, etc.",
            inputSchema={
                "type": "object",
                "properties": {}
            }
        ),
        Tool(
            name="get_analytics",
            description="Get analytics data for a specific timeframe.",
            inputSchema={
                "type": "object",
                "properties": {
                    "timeframe": {"type": "string", "description": "Timeframe (7days, 30days, 90days, all)", "default": "30days"}
                }
            }
        )
    ])
    
    # Gmail integration tools
    tools.extend([
        Tool(
            name="send_email",
            description="Send an email via Gmail. Requires Gmail to be authenticated first.",
            inputSchema={
                "type": "object",
                "properties": {
                    "to": {"type": "string", "description": "Recipient email address"},
                    "subject": {"type": "string", "description": "Email subject"},
                    "body": {"type": "string", "description": "Email body/content"}
                },
                "required": ["to", "subject", "body"]
            }
        ),
        Tool(
            name="get_emails",
            description="Search and retrieve Gmail messages.",
            inputSchema={
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Gmail search query (e.g., 'from:example@gmail.com', 'subject:meeting')", "default": ""},
                    "limit": {"type": "integer", "description": "Maximum number of emails to return", "default": 10}
                }
            }
        ),
        Tool(
            name="create_lead_from_email",
            description="Extract lead information from a Gmail email and create a lead in the CRM.",
            inputSchema={
                "type": "object",
                "properties": {
                    "email_id": {"type": "string", "description": "Gmail message ID"}
                },
                "required": ["email_id"]
            }
        )
    ])
    
    # Jira integration tools
    tools.extend([
        Tool(
            name="create_jira_issue_from_ticket",
            description="Create a Jira Software issue from a JSM ticket. Requires Jira Software project to be set up for the organization. ticket_id should be a JSM issue key (e.g., 'SR-123').",
            inputSchema={
                "type": "object",
                "properties": {
                    "ticket_id": {"type": "string", "description": "JSM issue key (e.g., 'SR-123') of the ticket to convert to Jira Software issue"}
                },
                "required": ["ticket_id"]
            }
        ),
        Tool(
            name="get_jira_issues_for_project",
            description="Get Jira issues for a project. Can fetch JSM Service Requests (project_type='jsm') or Jira Software issues (project_type='software'). Uses organization's project if project_key not provided.",
            inputSchema={
                "type": "object",
                "properties": {
                    "project_type": {"type": "string", "description": "Project type: 'jsm' for JSM Service Requests, 'software' for Jira Software issues", "default": "jsm"},
                    "project_key": {"type": "string", "description": "Jira project key (optional, uses org's project if not provided)"}
                }
            }
        ),
        Tool(
            name="sync_ticket_to_jira",
            description="Sync a JSM ticket to Jira Software (creates Jira Software issue from JSM ticket). ticket_id should be a JSM issue key (e.g., 'SR-123').",
            inputSchema={
                "type": "object",
                "properties": {
                    "ticket_id": {"type": "string", "description": "JSM issue key (e.g., 'SR-123') of the ticket to sync"}
                },
                "required": ["ticket_id"]
            }
        )
    ])
    
    return tools


# Tool handler mapping
TOOL_HANDLERS = {
    # Leads
    "create_lead": leads.create_lead,
    "get_leads": leads.get_leads,
    "update_lead_status": leads.update_lead_status,
    "convert_lead": leads.convert_lead,
    
    # Deals
    "create_deal": deals.create_deal,
    "get_deals": deals.get_deals,
    "update_deal": deals.update_deal,
    "update_deal_stage": deals.update_deal_stage,
    
    # Contacts
    "create_contact": contacts.create_contact,
    "get_contacts": contacts.get_contacts,
    "update_contact": contacts.update_contact,
    
    # Accounts
    "create_account": accounts.create_account,
    "get_accounts": accounts.get_accounts,
    "update_account": accounts.update_account,
    
    # Tickets
    "create_ticket": tickets.create_ticket,
    "get_tickets": tickets.get_tickets,
    "update_ticket_status": tickets.update_ticket_status,
    
    # Dashboard
    "get_dashboard_stats": dashboard.get_dashboard_stats,
    "get_analytics": dashboard.get_analytics,
    
    # Gmail
    "send_email": gmail.send_email,
    "get_emails": gmail.get_emails,
    "create_lead_from_email": gmail.create_lead_from_email,
    
    # Jira
    "create_jira_issue_from_ticket": jira.create_jira_issue_from_ticket,
    "get_jira_issues_for_project": jira.get_jira_issues_for_project,
    "sync_ticket_to_jira": jira.sync_ticket_to_jira,
}
