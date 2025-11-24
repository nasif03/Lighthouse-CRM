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

def create_jsm_service_project(org_name: str, org_id: str, admin_email: str) -> Optional[Dict]:
    """Create a Jira Service Management (JSM) service project for an organization"""
    try:
        # Generate project key from org name (uppercase, 2-10 chars, alphanumeric ONLY - no hyphens or special chars)
        import re
        # Remove all non-alphanumeric characters, convert to uppercase
        project_key = re.sub(r'[^A-Z0-9]', '', org_name.upper())[:10]
        if len(project_key) < 2:
            # Use org ID if name doesn't give enough characters
            org_id_clean = re.sub(r'[^A-Z0-9]', '', org_id.upper())[:8]
            project_key = f"ORG{org_id_clean}"
        
        # Get admin's account ID
        print(f"Looking up Jira account ID for: {admin_email}")
        account_id = get_user_account_id(admin_email)
        if not account_id:
            print(f"ERROR: Could not find Jira account ID for {admin_email}. Make sure this email exists in Jira.")
            return None
        print(f"Found Jira account ID: {account_id}")
        
        # Create JSM Service Project
        project_url = f"{JIRA_SERVER}/rest/api/3/project"
        
        # Try to get available templates first
        templates_url = f"{JIRA_SERVER}/rest/api/3/project/templates"
        templates_response = requests.get(
            templates_url,
            auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
            headers={"Accept": "application/json"}
        )
        
        project_template_key = None
        if templates_response.status_code == 200:
            templates = templates_response.json()
            # Look for service desk templates
            for template in templates:
                if template.get("projectTypeKey") == "service_desk":
                    project_template_key = template.get("key")
                    print(f"Found service desk template: {project_template_key}")
                    break
        
        # Build payload - try with template if found, otherwise without
        payload = {
            "key": project_key,
            "name": f"{org_name} Support",
            "projectTypeKey": "service_desk",
            "leadAccountId": account_id,
            "description": f"Customer support tickets for {org_name}",
            "assigneeType": "PROJECT_LEAD"
        }
        
        # Only add template if we found one
        if project_template_key:
            payload["projectTemplateKey"] = project_template_key
        else:
            print("Warning: No service desk template found, creating without template")
        
        print(f"Creating JSM project with key: {project_key}, name: {org_name} Support")
        response = requests.post(
            project_url,
            auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
            headers={
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            data=json.dumps(payload)
        )
        
        print(f"JSM project creation response status: {response.status_code}")
        if response.status_code != 201:
            print(f"JSM project creation error response: {response.text}")
        
        if response.status_code == 201:
            project = response.json()
            project_id = project.get("id", "")
            
            # Get service desk ID for the project
            service_desk_id = None
            try:
                # Get service desks for this project
                service_desks_url = f"{JIRA_SERVER}/rest/servicedeskapi/servicedesk"
                sd_response = requests.get(
                    service_desks_url,
                    auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
                    headers={"Accept": "application/json"}
                )
                if sd_response.status_code == 200:
                    service_desks = sd_response.json().get("values", [])
                    for sd in service_desks:
                        if sd.get("projectId") == project_id:
                            service_desk_id = str(sd.get("id"))
                            break
            except Exception as e:
                print(f"Warning: Could not get service desk ID: {str(e)}")
            
            return {
                "projectKey": project.get("key", project_key),
                "projectId": project_id,
                "projectName": project.get("name", f"{org_name} Support"),
                "projectUrl": f"{JIRA_SERVER}/browse/{project.get('key', project_key)}",
                "serviceDeskId": service_desk_id
            }
        else:
            # Check if project already exists
            if response.status_code == 400 and "already exists" in response.text.lower():
                # Try to get existing project
                try:
                    jira = get_jira_client()
                    if jira:
                        project = jira.project(project_key)
                        # Try to get service desk ID
                        service_desk_id = None
                        try:
                            service_desks_url = f"{JIRA_SERVER}/rest/servicedeskapi/servicedesk"
                            sd_response = requests.get(
                                service_desks_url,
                                auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
                                headers={"Accept": "application/json"}
                            )
                            if sd_response.status_code == 200:
                                service_desks = sd_response.json().get("values", [])
                                for sd in service_desks:
                                    if sd.get("projectId") == project.id:
                                        service_desk_id = str(sd.get("id"))
                                        break
                        except:
                            pass
                        
                        return {
                            "projectKey": project.key,
                            "projectId": project.id,
                            "projectName": getattr(project, 'name', f"{org_name} Support"),
                            "projectUrl": f"{JIRA_SERVER}/browse/{project.key}",
                            "serviceDeskId": service_desk_id
                        }
                except Exception as e:
                    print(f"Error getting existing JSM project: {str(e)}")
            print(f"Failed to create JSM project: {response.status_code} - {response.text}")
            return None
    except KeyError as e:
        print(f"ERROR creating JSM project: Missing key in response - {str(e)}")
        import traceback
        traceback.print_exc()
        return None
    except Exception as e:
        print(f"ERROR creating JSM project: {str(e)}")
        import traceback
        traceback.print_exc()
        return None

def create_jira_software_project(org_name: str, org_id: str, admin_email: str) -> Optional[Dict]:
    """Create a Jira Software project for development (optional)"""
    try:
        # Generate project key from org name (uppercase, 2-10 chars, alphanumeric)
        project_key = f"{org_name.upper().replace(' ', '')[:8]}DEV"
        if len(project_key) < 2:
            project_key = f"ORG{org_id[:6].upper()}DEV"
        
        # Get admin's account ID
        account_id = get_user_account_id(admin_email)
        if not account_id:
            print(f"Could not find Jira account for {admin_email}")
            return None
        
        # Create Jira Software Project
        project_url = f"{JIRA_SERVER}/rest/api/3/project"
        payload = {
            "key": project_key,
            "name": f"{org_name} Development",
            "projectTypeKey": "software",
            "projectTemplateKey": "com.pyxis.greenhopper.jira:gh-simplified-scrum-classic",
            "leadAccountId": account_id,
            "description": f"Development issues for {org_name}",
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
                "projectName": project.get("name", f"{org_name} Development"),
                "projectUrl": f"{JIRA_SERVER}/browse/{project.get('key', project_key)}"
            }
        else:
            # Check if project already exists
            if response.status_code == 400 and "already exists" in response.text.lower():
                try:
                    jira = get_jira_client()
                    if jira:
                        project = jira.project(project_key)
                        return {
                            "projectKey": project.key,
                            "projectId": project.id,
                            "projectName": getattr(project, 'name', f"{org_name} Development"),
                            "projectUrl": f"{JIRA_SERVER}/browse/{project.key}"
                        }
                except Exception as e:
                    print(f"Error getting existing Jira Software project: {str(e)}")
            print(f"Failed to create Jira Software project: {response.status_code} - {response.text}")
            return None
    except Exception as e:
        print(f"Error creating Jira Software project: {str(e)}")
        return None

# Keep old function name for backward compatibility, but it now creates JSM projects
def create_jira_project(org_name: str, org_id: str, admin_email: str) -> Optional[Dict]:
    """Create a Jira project for an organization (creates JSM Service Project)"""
    return create_jsm_service_project(org_name, org_id, admin_email)

def create_jsm_service_request(
    project_key: str,
    summary: str,
    description: str,
    reporter_email: str,
    reporter_name: str,
    priority: str = "Medium"
) -> Optional[Dict]:
    """Create a JSM Service Request using Service Desk API (sets customer as reporter)"""
    try:
        from config.database import organizations_collection
        from bson import ObjectId
        
        # Get service desk ID from organization
        org = organizations_collection.find_one({"jiraProjectKey": project_key})
        if not org:
            print(f"Organization not found for project key: {project_key}")
            return None
        
        service_desk_id = org.get("jsmServiceDeskId")
        
        # If service desk ID not found, try to get it from Jira
        if not service_desk_id:
            print(f"Service desk ID not found in org, querying Jira...")
            try:
                # Get project ID first
                project_url = f"{JIRA_SERVER}/rest/api/3/project/{project_key}"
                project_response = requests.get(
                    project_url,
                    auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
                    headers={"Accept": "application/json"}
                )
                
                if project_response.status_code == 200:
                    project_data = project_response.json()
                    project_id = project_data.get("id")
                    
                    # Get service desks
                    service_desks_url = f"{JIRA_SERVER}/rest/servicedeskapi/servicedesk"
                    sd_response = requests.get(
                        service_desks_url,
                        auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
                        headers={"Accept": "application/json"}
                    )
                    if sd_response.status_code == 200:
                        service_desks = sd_response.json().get("values", [])
                        for sd in service_desks:
                            if sd.get("projectId") == project_id:
                                service_desk_id = str(sd.get("id"))
                                # Save it for future use
                                organizations_collection.update_one(
                                    {"_id": ObjectId(org["_id"])},
                                    {"$set": {"jsmServiceDeskId": service_desk_id}}
                                )
                                print(f"Found and saved service desk ID: {service_desk_id}")
                                break
            except Exception as e:
                print(f"Error getting service desk ID: {str(e)}")
        
        if not service_desk_id:
            print("ERROR: Could not find service desk ID. Falling back to standard issue creation.")
            # Fallback to standard issue creation
            return _create_jsm_issue_fallback(project_key, summary, description, priority)
        
        # Map priority
        priority_map = {"low": "Low", "medium": "Medium", "high": "High", "urgent": "Highest"}
        jsm_priority = priority_map.get(priority.lower(), "Medium")
        
        # Step 1: Get request types for this service desk
        print(f"Getting request types for service desk: {service_desk_id}")
        request_types_url = f"{JIRA_SERVER}/rest/servicedeskapi/servicedesk/{service_desk_id}/requesttype"
        rt_response = requests.get(
            request_types_url,
            auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
            headers={"Accept": "application/json"}
        )
        
        if rt_response.status_code != 200:
            print(f"Failed to get request types: {rt_response.status_code} - {rt_response.text}")
            print("Falling back to standard issue creation...")
            return _create_jsm_issue_fallback(project_key, summary, description, priority)
        
        request_types = rt_response.json().get("values", [])
        if not request_types:
            print("No request types found for service desk. Falling back to standard issue creation...")
            return _create_jsm_issue_fallback(project_key, summary, description, priority)
        
        # Use the first available request type (usually the default)
        request_type = request_types[0]
        request_type_id = request_type.get("id")
        print(f"Using request type: {request_type.get('name')} (ID: {request_type_id})")
        
        # Step 2: Get request type fields to build proper payload
        request_type_fields_url = f"{JIRA_SERVER}/rest/servicedeskapi/servicedesk/{service_desk_id}/requesttype/{request_type_id}/field"
        fields_response = requests.get(
            request_type_fields_url,
            auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
            headers={"Accept": "application/json"}
        )
        
        request_field_values = {}
        if fields_response.status_code == 200:
            fields_data = fields_response.json()
            # Find summary and description fields
            for field in fields_data.get("requestFieldValues", []):
                field_id = field.get("fieldId")
                # Common field IDs in JSM
                if field_id in ["summary", "Summary"] or "summary" in field_id.lower():
                    request_field_values[field_id] = summary
                elif field_id in ["description", "Description"] or "description" in field_id.lower():
                    request_field_values[field_id] = description
        
        # If no fields found, use default field IDs
        if not request_field_values:
            request_field_values = {
                "summary": summary,
                "description": description
            }
        
        # Step 3: Create the Service Request using Service Desk API with customer as reporter
        print(f"Creating Service Request on behalf of: {reporter_email}")
        request_url = f"{JIRA_SERVER}/rest/servicedeskapi/request"
        
        payload = {
            "serviceDeskId": service_desk_id,
            "requestTypeId": request_type_id,
            "requestFieldValues": request_field_values,
            "raiseOnBehalfOf": reporter_email  # This sets the customer as the reporter
        }
        
        response = requests.post(
            request_url,
            auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
            headers={
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            data=json.dumps(payload)
        )
        
        print(f"Service Desk API response: {response.status_code}")
        if response.status_code == 201:
            issue_data = response.json()
            issue_key = issue_data.get("issueKey")
            issue_id = issue_data.get("issueId")
            print(f"Successfully created Service Request: {issue_key} with customer {reporter_email} as reporter")
            print(f"Issue URL: {JIRA_SERVER}/browse/{issue_key}")
            return {
                "issueKey": issue_key,
                "issueId": issue_id,
                "issueUrl": f"{JIRA_SERVER}/browse/{issue_key}"
            }
        else:
            print(f"Service Desk API error: {response.status_code} - {response.text}")
            print("Falling back to standard issue creation...")
            return _create_jsm_issue_fallback(project_key, summary, description, priority)
            
    except Exception as e:
        print(f"Error creating JSM service request: {str(e)}")
        import traceback
        traceback.print_exc()
        print("Falling back to standard issue creation...")
        return _create_jsm_issue_fallback(project_key, summary, description, priority)

def _create_jsm_issue_fallback(
    project_key: str,
    summary: str,
    description: str,
    priority: str = "Medium"
) -> Optional[Dict]:
    """Fallback: Create JSM issue using standard API (reporter will be authenticated user)"""
    try:
        jira = get_jira_client()
        if not jira:
            print("Failed to get Jira client")
            return None
        
        # Map priority
        priority_map = {"low": "Low", "medium": "Medium", "high": "High", "urgent": "Highest"}
        jsm_priority = priority_map.get(priority.lower(), "Medium")
        
        # Get available issue types for this project
        print(f"Getting available issue types for project: {project_key}")
        try:
            createmeta = jira.createmeta(
                projectKeys=project_key,
                expand="projects.issuetypes"
            )
            
            if not createmeta or not createmeta.get('projects'):
                print("No projects found in createmeta")
                return None
            
            project_meta = createmeta['projects'][0]
            issue_types = project_meta.get('issuetypes', [])
            
            if not issue_types:
                print("No issue types found for project")
                return None
            
            print(f"Found {len(issue_types)} issue type(s): {[it.get('name') for it in issue_types]}")
            
            # Try each issue type until one works
            for issue_type in issue_types:
                issue_type_name = issue_type.get('name', '')
                issue_type_id = issue_type.get('id', '')
                
                print(f"Trying issue type: {issue_type_name} (ID: {issue_type_id})")
                
                try:
                    new_issue = jira.create_issue(
                        project=project_key,
                        summary=summary,
                        description=description,
                        issuetype={'id': issue_type_id},
                        priority={'name': jsm_priority}
                    )
                    
                    print(f"Successfully created issue: {new_issue.key} with issue type: {issue_type_name}")
                    return {
                        "issueKey": new_issue.key,
                        "issueId": new_issue.id,
                        "issueUrl": f"{JIRA_SERVER}/browse/{new_issue.key}"
                    }
                except Exception as e:
                    try:
                        new_issue = jira.create_issue(
                            project=project_key,
                            summary=summary,
                            description=description,
                            issuetype={'name': issue_type_name},
                            priority={'name': jsm_priority}
                        )
                        
                        print(f"Successfully created issue: {new_issue.key}")
                        return {
                            "issueKey": new_issue.key,
                            "issueId": new_issue.id,
                            "issueUrl": f"{JIRA_SERVER}/browse/{new_issue.key}"
                        }
                    except Exception as e2:
                        print(f"Failed with issue type {issue_type_name}: {str(e2)}")
                        continue
            
            print("All issue types failed")
            return None
            
        except Exception as e:
            print(f"Error getting issue types: {str(e)}")
            import traceback
            traceback.print_exc()
            return None
            
    except Exception as e:
        print(f"Error in fallback issue creation: {str(e)}")
        import traceback
        traceback.print_exc()
        return None

def create_jira_software_issue(
    project_key: str,
    summary: str,
    description: str,
    issue_type: str = "Task"
) -> Optional[Dict]:
    """Create a Jira Software issue (for development)"""
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
        print(f"Error creating Jira Software issue: {str(e)}")
        return None

# Keep old function for backward compatibility
def create_jira_issue(project_key: str, summary: str, description: str, issue_type: str = "Task") -> Optional[Dict]:
    """Create a Jira issue (defaults to Jira Software issue)"""
    return create_jira_software_issue(project_key, summary, description, issue_type)

def get_jsm_service_requests(project_key: str, jql: Optional[str] = None) -> List[Dict]:
    """Get JSM Service Requests for a project (or all issues if Service Request type doesn't exist)"""
    try:
        jira = get_jira_client()
        if not jira:
            return []
        
        # Build JQL query - try Service Request first, but fallback to all issues
        if jql:
            query = jql
        else:
            # First try to get Service Request issues
            query = f"project = {project_key} AND issuetype = 'Service Request' ORDER BY created DESC"
            try:
                issues = jira.search_issues(query, maxResults=1)
                # If no results, try without issue type filter (get all issues)
                if len(issues) == 0:
                    print(f"No 'Service Request' issues found, querying all issues in project {project_key}")
                    query = f"project = {project_key} ORDER BY created DESC"
            except:
                # If query fails, try without issue type filter
                print(f"Service Request query failed, querying all issues in project {project_key}")
                query = f"project = {project_key} ORDER BY created DESC"
        
        print(f"Querying JSM issues with JQL: {query}")
        issues = jira.search_issues(query, maxResults=100)
        print(f"Found {len(issues)} issue(s) in project {project_key}")
        
        if len(issues) == 0:
            print(f"WARNING: No issues found for query: {query}")
            return []
        
        result = []
        processed_count = 0
        for issue in issues:
            try:
                # Extract reporter info
                reporter = issue.fields.reporter
                reporter_email = getattr(reporter, 'emailAddress', '') if reporter else ''
                reporter_name = getattr(reporter, 'displayName', '') if reporter else ''
                
                # Extract assignee info
                assignee = issue.fields.assignee
                assignee_account_id = getattr(assignee, 'accountId', None) if assignee else None
                
                # Map priority
                priority_name = getattr(issue.fields.priority, 'name', 'Medium') if issue.fields.priority else 'Medium'
                priority_map = {
                    "Lowest": "low",
                    "Low": "low",
                    "Medium": "medium",
                    "High": "high",
                    "Highest": "urgent"
                }
                priority = priority_map.get(priority_name, "medium")
                
                # Map status
                status_name = issue.fields.status.name
                # Check if resolution is set - if "Done" status has resolution, it's "closed" in CRM
                has_resolution = False
                try:
                    has_resolution = hasattr(issue.fields, 'resolution') and issue.fields.resolution is not None
                except:
                    # If we can't check resolution, assume it's not set
                    has_resolution = False
                
                status_map = {
                    "To Do": "open",
                    "In Progress": "in_progress",
                    "Done": "closed" if has_resolution else "resolved",  # Done with resolution = closed, Done without = resolved
                    "Resolved": "resolved",
                    "Closed": "closed"
                }
                status = status_map.get(status_name, "open")
                
                result.append({
                    "key": issue.key,
                    "id": issue.id,
                    "summary": issue.fields.summary,
                    "description": issue.fields.description or "",
                    "status": status,
                    "statusName": status_name,
                    "issueType": issue.fields.issuetype.name,
                    "priority": priority,
                    "priorityName": priority_name,
                    "reporterEmail": reporter_email,
                    "reporterName": reporter_name,
                    "assigneeAccountId": assignee_account_id,
                    "created": issue.fields.created,
                    "updated": issue.fields.updated,
                    "url": f"{JIRA_SERVER}/browse/{issue.key}"
                })
                processed_count += 1
            except Exception as e:
                issue_key = issue.key if hasattr(issue, 'key') else 'unknown'
                print(f"Error processing issue {issue_key}: {str(e)}")
                import traceback
                traceback.print_exc()
                continue
        
        print(f"Successfully processed {processed_count} out of {len(issues)} issues")
        return result
    except Exception as e:
        print(f"Error retrieving JSM service requests: {str(e)}")
        return []

def get_jira_software_issues(project_key: str, jql: Optional[str] = None) -> List[Dict]:
    """Get Jira Software issues for a project"""
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
        print(f"Error retrieving Jira Software issues: {str(e)}")
        return []

# Keep old function for backward compatibility
def get_jira_issues(project_key: str, jql: Optional[str] = None) -> List[Dict]:
    """Get Jira issues for a project (defaults to Jira Software issues)"""
    return get_jira_software_issues(project_key, jql)

def get_jsm_service_request(issue_key: str) -> Optional[Dict]:
    """Get a single JSM Service Request by key"""
    try:
        jira = get_jira_client()
        if not jira:
            return None
        
        issue = jira.issue(issue_key)
        
        # Extract reporter info
        reporter = issue.fields.reporter
        reporter_email = getattr(reporter, 'emailAddress', '') if reporter else ''
        reporter_name = getattr(reporter, 'displayName', '') if reporter else ''
        
        # Extract assignee info
        assignee = issue.fields.assignee
        assignee_account_id = getattr(assignee, 'accountId', None) if assignee else None
        
        # Map priority
        priority_name = getattr(issue.fields.priority, 'name', 'Medium') if issue.fields.priority else 'Medium'
        priority_map = {
            "Lowest": "low",
            "Low": "low",
            "Medium": "medium",
            "High": "high",
            "Highest": "urgent"
        }
        priority = priority_map.get(priority_name, "medium")
        
        # Map status
        status_name = issue.fields.status.name
        # Check if resolution is set - if "Done" status has resolution, it's "closed" in CRM
        has_resolution = hasattr(issue.fields, 'resolution') and issue.fields.resolution is not None
        
        status_map = {
            "To Do": "open",
            "In Progress": "in_progress",
            "Done": "closed" if has_resolution else "resolved",  # Done with resolution = closed, Done without = resolved
            "Resolved": "resolved",
            "Closed": "closed"
        }
        status = status_map.get(status_name, "open")
        
        return {
            "key": issue.key,
            "id": issue.id,
            "summary": issue.fields.summary,
            "description": issue.fields.description or "",
            "status": status,
            "statusName": status_name,
            "issueType": issue.fields.issuetype.name,
            "priority": priority,
            "priorityName": priority_name,
            "reporterEmail": reporter_email,
            "reporterName": reporter_name,
            "assigneeAccountId": assignee_account_id,
            "created": issue.fields.created,
            "updated": issue.fields.updated,
            "url": f"{JIRA_SERVER}/browse/{issue.key}"
        }
    except Exception as e:
        print(f"Error retrieving JSM service request: {str(e)}")
        return None

def get_jira_software_issue(issue_key: str) -> Optional[Dict]:
    """Get a single Jira Software issue by key"""
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
        print(f"Error retrieving Jira Software issue: {str(e)}")
        return None

# Keep old function for backward compatibility
def get_jira_issue(issue_key: str) -> Optional[Dict]:
    """Get a single Jira issue by key (works for both JSM and Jira Software)"""
    try:
        jira = get_jira_client()
        if not jira:
            return None
        
        issue = jira.issue(issue_key)
        
        # Check if it's a service request
        if issue.fields.issuetype.name == 'Service Request':
            return get_jsm_service_request(issue_key)
        else:
            return get_jira_software_issue(issue_key)
    except Exception as e:
        print(f"Error retrieving Jira issue: {str(e)}")
        return None

def update_jsm_service_request(
    issue_key: str,
    status: Optional[str] = None,
    priority: Optional[str] = None,
    assignee_account_id: Optional[str] = None
) -> Optional[Dict]:
    """Update a JSM Service Request"""
    try:
        jira = get_jira_client()
        if not jira:
            return None
        
        issue = jira.issue(issue_key)
        update_fields = {}
        
        # Map priority
        if priority:
            priority_map = {
                "low": "Low",
                "medium": "Medium",
                "high": "High",
                "urgent": "Highest"
            }
            jsm_priority = priority_map.get(priority.lower(), "Medium")
            update_fields['priority'] = {'name': jsm_priority}
        
        # Assignee
        if assignee_account_id:
            update_fields['assignee'] = {'accountId': assignee_account_id}
        
        # Handle status change using transitions (required in Jira)
        if status:
            # Special handling for "closed" - set resolution to "Done" AND change status to "Done"
            if status == "closed":
                try:
                    # Get available resolutions - use "Done" for closed tickets
                    resolution_field = None
                    try:
                        resolutions = jira.resolutions()
                        # Look for "Done" resolution first
                        for res in resolutions:
                            if res.name == "Done":
                                resolution_field = {"name": res.name}
                                break
                        # If "Done" not found, try "Fixed" or "Resolved"
                        if not resolution_field:
                            for res in resolutions:
                                if res.name in ["Fixed", "Resolved"]:
                                    resolution_field = {"name": res.name}
                                    break
                        # If still not found, use first available
                        if not resolution_field and resolutions:
                            resolution_field = {"name": resolutions[0].name}
                    except Exception as e:
                        print(f"Warning: Could not get resolutions: {str(e)}")
                        # Default to "Done"
                        resolution_field = {"name": "Done"}
                    
                    # Transition to "Done" status with resolution
                    target_status = "Done"
                    current_status = issue.fields.status.name
                    
                    if current_status != target_status:
                        try:
                            transitions = jira.transitions(issue)
                            
                            # Find transition to "Done" status
                            transition_id = None
                            transition_name = None
                            for transition in transitions:
                                if transition.get('to') and transition['to'].get('name') == target_status:
                                    transition_id = transition.get('id')
                                    transition_name = transition.get('name')
                                    break
                            
                            # If no direct transition, try common transition names
                            if not transition_id:
                                for transition in transitions:
                                    if transition.get('name') in ["Done", "Resolve", "Resolve Issue"]:
                                        if transition.get('to') and transition['to'].get('name') == "Done":
                                            transition_id = transition.get('id')
                                            transition_name = transition.get('name')
                                            break
                            
                            if transition_id:
                                # Execute transition first, then set resolution
                                jira.transition_issue(issue, transition_id)
                                issue = jira.issue(issue_key)  # Refresh
                                
                                # Try to set resolution after transition
                                if resolution_field:
                                    try:
                                        issue.update(fields={'resolution': resolution_field})
                                        issue = jira.issue(issue_key)  # Refresh
                                    except Exception:
                                        # Resolution may be set automatically by workflow
                                        pass
                            else:
                                print(f"ERROR: Could not find transition to 'Done' for {issue_key}")
                        except Exception as e:
                            print(f"Error closing issue {issue_key}: {str(e)}")
                            import traceback
                            traceback.print_exc()
                    else:
                        # Status is already Done, try to set resolution if not already set
                        if resolution_field:
                            current_resolution = getattr(issue.fields, 'resolution', None)
                            current_resolution_name = getattr(current_resolution, 'name', None) if current_resolution else None
                            
                            if not current_resolution_name or current_resolution_name != resolution_field['name']:
                                try:
                                    issue.update(fields={'resolution': resolution_field})
                                    issue = jira.issue(issue_key)  # Refresh
                                    new_resolution = getattr(issue.fields.resolution, 'name', None) if hasattr(issue.fields, 'resolution') and issue.fields.resolution else None
                                    pass  # Resolution set
                                except Exception as e:
                                    print(f"Could not set resolution for {issue_key}: {str(e)}")
                            pass  # Resolution already set
                except Exception as e:
                    print(f"Error handling closed status for {issue_key}: {str(e)}")
                    import traceback
                    traceback.print_exc()
            else:
                # Map CRM status to Jira status names (for non-closed statuses)
                status_map = {
                    "open": "To Do",
                    "in_progress": "In Progress",
                    "resolved": "Done"
                }
                target_status = status_map.get(status, "To Do")
                
                # Get current status
                current_status = issue.fields.status.name
                
                # Check if issue currently has a resolution (i.e., it's closed)
                current_resolution = None
                if hasattr(issue.fields, 'resolution') and issue.fields.resolution:
                    current_resolution = getattr(issue.fields.resolution, 'name', None)
                
                # If transitioning away from Done status (with resolution) to another status, clear resolution
                needs_resolution_clear = (current_status == "Done" and current_resolution) and (target_status != "Done")
                
                # Only transition if status is different
                if current_status != target_status:
                    try:
                        # Get available transitions for this issue
                        transitions = jira.transitions(issue)
                        
                        # Find the transition that leads to the target status
                        transition_id = None
                        transition_name = None
                        for transition in transitions:
                            # Check if this transition leads to our target status
                            if transition.get('to') and transition['to'].get('name') == target_status:
                                transition_id = transition.get('id')
                                transition_name = transition.get('name')
                                break
                        
                        # If no direct transition found, try common transition names
                        if not transition_id:
                            transition_name_map = {
                                "To Do": ["To Do", "Reopen"],
                                "In Progress": ["In Progress", "Start Progress", "Start work"],
                                "Done": ["Done", "Resolve", "Resolve Issue"]
                            }
                            possible_names = transition_name_map.get(target_status, [])
                            for transition in transitions:
                                if transition.get('name') in possible_names:
                                    transition_id = transition.get('id')
                                    transition_name = transition.get('name')
                                    break
                        
                        if transition_id:
                            jira.transition_issue(issue, transition_id)
                            issue = jira.issue(issue_key)  # Refresh
                            
                            # Note: Resolution cannot be cleared via API once set (Jira workflow limitation)
                            if needs_resolution_clear:
                                new_resolution = getattr(issue.fields.resolution, 'name', None) if hasattr(issue.fields, 'resolution') and issue.fields.resolution else None
                                if new_resolution:
                                    pass  # Resolution remains set (read-only in Jira once closed)
                        else:
                            print(f"Warning: Could not find transition to {target_status} for {issue_key}")
                    except Exception as e:
                        print(f"Error transitioning issue {issue_key} to {target_status}: {str(e)}")
                        import traceback
                        traceback.print_exc()
                elif needs_resolution_clear:
                    # Status is already correct, but resolution is still set
                    # Note: Resolution cannot be cleared via API once set (Jira workflow limitation)
                    current_resolution = getattr(issue.fields.resolution, 'name', None) if hasattr(issue.fields, 'resolution') and issue.fields.resolution else None
                    if current_resolution:
                        pass  # Resolution remains set (read-only in Jira once closed)
        
        # Update non-status fields (priority, assignee) after status transitions
        # Note: resolution is handled during the "closed" transition
        if update_fields and 'resolution' not in update_fields:
            issue.update(fields=update_fields)
            print(f"Updated fields for {issue_key}: {list(update_fields.keys())}")
        
        return get_jsm_service_request(issue_key)
    except Exception as e:
        print(f"Error updating JSM service request: {str(e)}")
        import traceback
        traceback.print_exc()
        return None

def link_jsm_to_jira_software(jsm_issue_key: str, jira_software_issue_key: str) -> bool:
    """Link a JSM Service Request to a Jira Software issue"""
    try:
        # Use Jira's issue linking API
        link_url = f"{JIRA_SERVER}/rest/api/3/issueLink"
        payload = {
            "type": {"name": "Relates"},
            "inwardIssue": {"key": jsm_issue_key},
            "outwardIssue": {"key": jira_software_issue_key}
        }
        
        response = requests.post(
            link_url,
            auth=HTTPBasicAuth(JIRA_EMAIL, JIRA_TOKEN),
            headers={
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            data=json.dumps(payload)
        )
        
        return response.status_code == 201
    except Exception as e:
        print(f"Error linking JSM to Jira Software issue: {str(e)}")
        return False

