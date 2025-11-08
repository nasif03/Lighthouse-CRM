"""Jira integration service"""
import requests
from requests.auth import HTTPBasicAuth
import json
from typing import Optional, Dict, List
from jira import JIRA
from config.settings import JIRA_SERVER, JIRA_EMAIL, JIRA_TOKEN

def get_jira_client() -> Optional[JIRA]:
    """Get Jira client instance"""
    try:
        jira = JIRA(server=JIRA_SERVER, basic_auth=(JIRA_EMAIL, JIRA_TOKEN))
        return jira
    except Exception as e:
        print(f"Failed to connect to Jira: {str(e)}")
        return None

def get_user_account_id(email: str) -> Optional[str]:
    """Get Jira account ID for a user email"""
    try:
        user_search_url = f"{JIRA_SERVER}/rest/api/3/user/search?query={JIRA_EMAIL}"
        response = requests.get(
            user_search_url,
            auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
            headers={"Accept": "application/json"}
        )
        
        if response.status_code == 200:
            users = response.json()
            if users:
                return users[0]["accountId"]
        return None
    except Exception as e:
        print(f"Error getting user account ID: {str(e)}")
        return None

def create_jira_project(org_name: str, org_id: str, admin_email: str) -> Optional[Dict]:
    """Create a Jira project for an organization"""
    try:
        # Generate project key from org name (uppercase, 2-10 chars, alphanumeric)
        project_key = org_name.upper().replace(" ", "")[:10]
        if len(project_key) < 2:
            project_key = f"ORG{org_id[:8].upper()}"
        
        # Get admin's account ID
        account_id = get_user_account_id(admin_email)
        if not account_id:
            print(f"Could not find Jira account for {admin_email}")
            return None
        
        # Create project
        project_url = f"{JIRA_SERVER}/rest/api/3/project"
        payload = {
            "key": project_key,
            "name": f"{org_name} Support",
            "projectTypeKey": "software",
            "projectTemplateKey": "com.pyxis.greenhopper.jira:gh-simplified-scrum-classic",
            "leadAccountId": account_id,
            "description": f"Support tickets for {org_name}",
            "assigneeType": "PROJECT_LEAD"
        }
        
        response = requests.post(
            project_url,
            auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
            headers={
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            data=json.dumps(payload)
        )
        
        if response.status_code == 201:
            project = response.json()
            return {
                "projectKey": project.get("key", project_key),
                "projectId": project.get("id", ""),
                "projectName": project.get("name", f"{org_name} Support"),
                "projectUrl": f"{JIRA_SERVER}/browse/{project.get('key', project_key)}"
            }
        else:
            # Check if project already exists
            if response.status_code == 400 and "already exists" in response.text.lower():
                # Try to get existing project
                try:
                    jira = get_jira_client()
                    if jira:
                        project = jira.project(project_key)
                        return {
                            "projectKey": project.key,
                            "projectId": project.id,
                            "projectName": getattr(project, 'name', f"{org_name} Support"),
                            "projectUrl": f"{JIRA_SERVER}/browse/{project.key}"
                        }
                except Exception as e:
                    print(f"Error getting existing Jira project: {str(e)}")
            print(f"Failed to create Jira project: {response.status_code} - {response.text}")
            return None
    except KeyError as e:
        print(f"Error creating Jira project: Missing key in response - {str(e)}")
        return None
    except Exception as e:
        print(f"Error creating Jira project: {str(e)}")
        return None

def create_jira_issue(project_key: str, summary: str, description: str, issue_type: str = "Task") -> Optional[Dict]:
    """Create a Jira issue"""
    try:
        jira = get_jira_client()
        if not jira:
            return None
        
        new_issue = jira.create_issue(
            project=project_key,
            summary=summary,
            description=description,
            issuetype={'name': issue_type}
        )
        
        return {
            "issueKey": new_issue.key,
            "issueId": new_issue.id,
            "issueUrl": f"{JIRA_SERVER}/browse/{new_issue.key}"
        }
    except Exception as e:
        print(f"Error creating Jira issue: {str(e)}")
        return None

def get_jira_issues(project_key: str, jql: Optional[str] = None) -> List[Dict]:
    """Get Jira issues for a project"""
    try:
        jira = get_jira_client()
        if not jira:
            return []
        
        # Build JQL query
        if jql:
            query = jql
        else:
            query = f"project = {project_key} ORDER BY created DESC"
        
        issues = jira.search_issues(query, maxResults=100)
        
        result = []
        for issue in issues:
            result.append({
                "key": issue.key,
                "id": issue.id,
                "summary": issue.fields.summary,
                "description": issue.fields.description or "",
                "status": issue.fields.status.name,
                "issueType": issue.fields.issuetype.name,
                "created": issue.fields.created,
                "updated": issue.fields.updated,
                "url": f"{JIRA_SERVER}/browse/{issue.key}"
            })
        
        return result
    except Exception as e:
        print(f"Error retrieving Jira issues: {str(e)}")
        return []

def get_jira_issue(issue_key: str) -> Optional[Dict]:
    """Get a single Jira issue by key"""
    try:
        jira = get_jira_client()
        if not jira:
            return None
        
        issue = jira.issue(issue_key)
        
        return {
            "key": issue.key,
            "id": issue.id,
            "summary": issue.fields.summary,
            "description": issue.fields.description or "",
            "status": issue.fields.status.name,
            "issueType": issue.fields.issuetype.name,
            "created": issue.fields.created,
            "updated": issue.fields.updated,
            "url": f"{JIRA_SERVER}/browse/{issue.key}"
        }
    except Exception as e:
        print(f"Error retrieving Jira issue: {str(e)}")
        return None

