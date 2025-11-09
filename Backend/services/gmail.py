"""Gmail service - OAuth and API operations"""
import os
import json
from pathlib import Path
from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from typing import Optional, Dict, List
from datetime import datetime

# Gmail API scopes
SCOPES = [
    'https://www.googleapis.com/auth/gmail.readonly',
    'https://www.googleapis.com/auth/gmail.send',
    'https://www.googleapis.com/auth/gmail.modify'
]

# Paths
CLIENT_SECRET_PATH = Path(__file__).parent.parent / 'client_secret.json'
TOKEN_DIR = Path(__file__).parent.parent / 'token'

# Ensure token directory exists
TOKEN_DIR.mkdir(exist_ok=True)

def get_token_path(user_email: str) -> Path:
    """Get the token file path for a user"""
    # Sanitize email for filename
    safe_email = user_email.replace('@', '_at_').replace('.', '_')
    return TOKEN_DIR / f'{safe_email}_token.json'

def get_credentials(user_email: str) -> Optional[Credentials]:
    """Get stored credentials for a user"""
    token_path = get_token_path(user_email)
    
    if not token_path.exists():
        return None
    
    try:
        # Load credentials - use None for scopes to accept any scopes in the token file
        # This handles cases where Google adds additional scopes (openid, userinfo, etc.)
        creds = Credentials.from_authorized_user_file(str(token_path), scopes=None)
        return creds
    except Exception as e:
        print(f"Error loading credentials: {e}")
        return None

def save_credentials(user_email: str, creds: Credentials) -> None:
    """Save credentials for a user"""
    token_path = get_token_path(user_email)
    
    with open(token_path, 'w') as token:
        token.write(creds.to_json())

def get_authorization_url(user_email: str, redirect_uri: str) -> Dict[str, str]:
    """Get authorization URL for Gmail OAuth flow"""
    if not CLIENT_SECRET_PATH.exists():
        raise FileNotFoundError(f"Client secret file not found at {CLIENT_SECRET_PATH}")
    
    # Read client secret
    with open(CLIENT_SECRET_PATH, 'r') as f:
        client_config = json.load(f)
    
    # Handle both 'installed' and 'web' client types
    client_info = client_config.get('web') or client_config.get('installed')
    if not client_info:
        raise ValueError("Invalid client secret format")
    
    # Create flow - use Flow.from_client_config for web apps
    from google_auth_oauthlib.flow import Flow
    
    # Don't set include_granted_scopes to avoid scope conflicts
    # Just request the scopes we need
    flow = Flow.from_client_config(
        client_config,
        SCOPES,
        redirect_uri=redirect_uri
    )
    
    authorization_url, state = flow.authorization_url(
        access_type='offline',
        include_granted_scopes=False,  # Don't include previously granted scopes
        prompt='consent'
    )
    
    return {
        'authorization_url': authorization_url,
        'state': state
    }

def exchange_code_for_token(user_email: str, authorization_code: str, redirect_uri: str, state: str = None) -> Credentials:
    """Exchange authorization code for credentials"""
    if not CLIENT_SECRET_PATH.exists():
        raise FileNotFoundError(f"Client secret file not found at {CLIENT_SECRET_PATH}")
    
    # Read client secret
    with open(CLIENT_SECRET_PATH, 'r') as f:
        client_config = json.load(f)
    
    # Handle both 'installed' and 'web' client types
    from google_auth_oauthlib.flow import Flow
    
    # Create flow with the scopes we requested
    flow = Flow.from_client_config(
        client_config,
        SCOPES,
        redirect_uri=redirect_uri,
        state=state
    )
    
    # Fetch token - this will get the credentials with whatever scopes Google granted
    flow.fetch_token(code=authorization_code)
    creds = flow.credentials
    
    # Verify that we have at least the Gmail scopes we need
    # Google may add additional scopes (openid, userinfo, etc.) which is fine
    granted_scopes = creds.scopes if creds.scopes else []
    required_scopes = set(SCOPES)
    granted_scopes_set = set(granted_scopes)
    
    # Check if we have all required Gmail scopes (ignore additional scopes like openid)
    gmail_scopes_granted = any('gmail' in scope for scope in granted_scopes)
    if not gmail_scopes_granted:
        raise ValueError("Gmail scopes were not granted in the OAuth flow")
    
    # Save credentials with whatever scopes were granted
    save_credentials(user_email, creds)
    
    return creds

def get_gmail_service(user_email: str) -> Optional:
    """Get Gmail service instance for a user"""
    creds = get_credentials(user_email)
    
    if not creds:
        return None
    
    # Refresh token if expired
    if creds.expired and creds.refresh_token:
        try:
            creds.refresh(Request())
            save_credentials(user_email, creds)
        except Exception as e:
            print(f"Error refreshing token: {e}")
            return None
    
    if not creds.valid:
        return None
    
    try:
        service = build('gmail', 'v1', credentials=creds)
        return service
    except Exception as e:
        print(f"Error building Gmail service: {e}")
        return None

def get_messages(user_email: str, max_results: int = 10, query: str = '') -> List[Dict]:
    """Get messages for a user"""
    service = get_gmail_service(user_email)
    
    if not service:
        raise Exception("Gmail service not available. Please authenticate first.")
    
    try:
        # List messages
        list_params = {
            'userId': 'me',
            'maxResults': max_results
        }
        
        if query:
            list_params['q'] = query
        
        results = service.users().messages().list(**list_params).execute()
        messages = results.get('messages', [])
        
        if not messages:
            return []
        
        # Get full message details
        message_list = []
        for msg in messages:
            try:
                message = service.users().messages().get(
                    userId='me',
                    id=msg['id'],
                    format='full'
                ).execute()
                
                # Extract headers
                headers = message.get('payload', {}).get('headers', [])
                subject = next((h['value'] for h in headers if h['name'] == 'Subject'), 'No Subject')
                sender = next((h['value'] for h in headers if h['name'] == 'From'), 'Unknown')
                date = next((h['value'] for h in headers if h['name'] == 'Date'), '')
                
                # Extract snippet
                snippet = message.get('snippet', '')
                
                # Extract body
                body = ''
                payload = message.get('payload', {})
                
                def extract_body_from_parts(parts):
                    """Recursively extract body from message parts"""
                    html_body = ''
                    for part in parts:
                        if part.get('mimeType') == 'text/plain':
                            data = part.get('body', {}).get('data', '')
                            if data:
                                try:
                                    import base64
                                    return base64.urlsafe_b64decode(data).decode('utf-8', errors='ignore')
                                except:
                                    return ''
                        elif part.get('mimeType') == 'text/html':
                            # Store HTML as fallback
                            data = part.get('body', {}).get('data', '')
                            if data:
                                try:
                                    import base64
                                    html_body = base64.urlsafe_b64decode(data).decode('utf-8', errors='ignore')
                                except:
                                    pass
                        # Check nested parts
                        if 'parts' in part:
                            nested_body = extract_body_from_parts(part['parts'])
                            if nested_body:
                                return nested_body
                    # Return HTML if no plain text found
                    return html_body if html_body else ''
                
                if 'parts' in payload:
                    body = extract_body_from_parts(payload['parts'])
                elif payload.get('mimeType') == 'text/plain':
                    data = payload.get('body', {}).get('data', '')
                    if data:
                        try:
                            import base64
                            body = base64.urlsafe_b64decode(data).decode('utf-8', errors='ignore')
                        except:
                            body = ''
                elif payload.get('mimeType') == 'text/html':
                    data = payload.get('body', {}).get('data', '')
                    if data:
                        try:
                            import base64
                            body = base64.urlsafe_b64decode(data).decode('utf-8', errors='ignore')
                        except:
                            body = ''
                
                message_list.append({
                    'id': message['id'],
                    'threadId': message.get('threadId', ''),
                    'subject': subject,
                    'from': sender,
                    'date': date,
                    'snippet': snippet,
                    'body': body,
                    'labels': message.get('labelIds', [])
                })
            except Exception as e:
                print(f"Error fetching message {msg['id']}: {e}")
                continue
        
        return message_list
    except HttpError as e:
        print(f"Gmail API error: {e}")
        raise Exception(f"Failed to fetch messages: {str(e)}")

def send_message(user_email: str, to: str, subject: str, body: str) -> Dict:
    """Send an email"""
    service = get_gmail_service(user_email)
    
    if not service:
        raise Exception("Gmail service not available. Please authenticate first.")
    
    try:
        import base64
        from email.mime.text import MIMEText
        
        # Create message
        message = MIMEText(body)
        message['to'] = to
        message['subject'] = subject
        message['from'] = user_email
        
        # Encode message
        raw_message = base64.urlsafe_b64encode(message.as_bytes()).decode('utf-8')
        
        # Send message
        send_message = service.users().messages().send(
            userId='me',
            body={'raw': raw_message}
        ).execute()
        
        return {
            'id': send_message['id'],
            'threadId': send_message.get('threadId', ''),
            'success': True
        }
    except HttpError as e:
        print(f"Gmail API error: {e}")
        raise Exception(f"Failed to send message: {str(e)}")

def is_authenticated(user_email: str) -> bool:
    """Check if user is authenticated with Gmail"""
    creds = get_credentials(user_email)
    
    if not creds:
        return False
    
    # Refresh token if expired
    if creds.expired and creds.refresh_token:
        try:
            creds.refresh(Request())
            save_credentials(user_email, creds)
        except Exception as e:
            print(f"Error refreshing Gmail token for {user_email}: {e}")
            return False
    
    # Check if credentials are valid
    if not creds.valid:
        return False
    
    # Verify credentials are valid (don't make API call every time for performance)
    # The credentials.valid check above should be sufficient
    # Only make API call if we want to be extra sure
    try:
        # Check if credentials have Gmail scopes (they might have additional scopes like openid)
        granted_scopes = creds.scopes if creds.scopes else []
        has_gmail_scope = any('gmail' in scope for scope in granted_scopes)
        
        if not has_gmail_scope:
            print(f"No Gmail scopes found in credentials for {user_email}")
            return False
        
        # Just check if we can build the service without making an API call
        # This is faster and still validates the credentials structure
        service = build('gmail', 'v1', credentials=creds)
        return True
    except Exception as e:
        print(f"Error building Gmail service for {user_email}: {e}")
        return False

def create_credentials_from_firebase_token(user_email: str, access_token: str, refresh_token: str = None) -> Credentials:
    """Create Gmail credentials from Firebase OAuth token"""
    if not CLIENT_SECRET_PATH.exists():
        raise FileNotFoundError(f"Client secret file not found at {CLIENT_SECRET_PATH}")
    
    # Read client secret
    with open(CLIENT_SECRET_PATH, 'r') as f:
        client_config = json.load(f)
    
    client_info = client_config.get('web') or client_config.get('installed')
    if not client_info:
        raise ValueError("Invalid client secret format")
    
    # Create credentials from token
    # Don't restrict scopes - accept whatever scopes the token has
    # Firebase tokens may include additional scopes like openid, userinfo, etc.
    creds = Credentials(
        token=access_token,
        refresh_token=refresh_token,
        token_uri=client_info['token_uri'],
        client_id=client_info['client_id'],
        client_secret=client_info['client_secret'],
        scopes=None  # Don't restrict - accept any scopes in the token
    )
    
    # Save credentials
    save_credentials(user_email, creds)
    
    return creds

