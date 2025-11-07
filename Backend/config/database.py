"""Database connection and setup"""
from pymongo import MongoClient
from config.settings import MONGO_URI

# MongoDB connection
mongo_client = MongoClient(MONGO_URI)
db = mongo_client.lighthousecrm

# Collections
users_collection = db.users
organizations_collection = db.organizations
leads_collection = db.leads
contacts_collection = db.contacts
accounts_collection = db.accounts
deals_collection = db.deals
activities_collection = db.activities
tickets_collection = db.tickets
roles_collection = db.roles
ticket_comments_collection = db.ticketComments
conversations_collection = db.conversations
messages_collection = db.messages
campaigns_collection = db.campaigns
segments_collection = db.segments
templates_collection = db.templates
communications_collection = db.communications
files_collection = db.files
webforms_collection = db.webforms
integrations_collection = db.integrations
reports_collection = db.reports
exports_collection = db.exports
audit_collection = db.audit
jira_integration_collection = db.jiraIntegration

def test_connection():
    """Test MongoDB connection"""
    try:
        mongo_client.admin.command('ping')
        print("✅ MongoDB connection successful")
        return True
    except Exception as e:
        print(f"❌ MongoDB connection failed: {str(e)}")
        raise

def create_indexes():
    """Create indexes for frequently queried fields"""
    def safe_create_index(collection, index_spec, name=None, **kwargs):
        """Safely create an index, handling conflicts by dropping and recreating"""
        import re
        try:
            # Try to drop existing index with same name first (if name provided)
            if name:
                try:
                    collection.drop_index(name)
                except Exception:
                    pass  # Index doesn't exist, continue
            
            # Also try to drop auto-generated index names that might conflict
            # For single field indexes, try dropping the auto-generated name pattern
            if isinstance(index_spec, str):
                auto_name = f"{index_spec}_1"
                try:
                    collection.drop_index(auto_name)
                except Exception:
                    pass
            
            # Create the index with explicit name
            if name:
                collection.create_index(index_spec, name=name, **kwargs)
            else:
                collection.create_index(index_spec, **kwargs)
        except Exception as e:
            error_str = str(e)
            error_code = None
            errmsg = None
            
            # Extract error code and message from exception if available
            if hasattr(e, 'details') and isinstance(e.details, dict):
                error_code = e.details.get('code')
                errmsg = e.details.get('errmsg', '')
            elif "code" in error_str:
                # Try to extract code from error string
                code_match = re.search(r"'code':\s*(\d+)", error_str)
                if code_match:
                    error_code = int(code_match.group(1))
            
            # Use errmsg if available, otherwise use error_str
            search_text = errmsg if errmsg else error_str
            
            # Handle IndexOptionsConflict (code 85) - index exists with different name
            if "IndexOptionsConflict" in search_text or error_code == 85:
                try:
                    # Extract the existing index name from error message
                    # Pattern matches: "with a different name: <name>" where name can contain word chars and underscores
                    name_match = re.search(r"with a different name:\s*([\w_-]+)", search_text)
                    if name_match:
                        existing_name = name_match.group(1)
                        collection.drop_index(existing_name)
                        # Recreate with desired name
                        if name:
                            collection.create_index(index_spec, name=name, **kwargs)
                        else:
                            collection.create_index(index_spec, **kwargs)
                    else:
                        # Fallback: list all indexes and find matching key spec
                        indexes = list(collection.list_indexes())
                        for idx in indexes:
                            idx_key = idx.get("key", {})
                            # Check if keys match
                            if isinstance(index_spec, str):
                                if idx_key == {index_spec: 1}:
                                    collection.drop_index(idx["name"])
                                    break
                            elif isinstance(index_spec, list):
                                expected_key = {k: v for k, v in index_spec}
                                if idx_key == expected_key:
                                    collection.drop_index(idx["name"])
                                    break
                        # Recreate with desired name
                        if name:
                            collection.create_index(index_spec, name=name, **kwargs)
                        else:
                            collection.create_index(index_spec, **kwargs)
                except Exception as e2:
                    print(f"⚠️ Warning: Could not create index {name or index_spec}: {str(e2)}")
            # Handle IndexKeySpecsConflict (code 86) - index exists with same name but different options
            elif "IndexKeySpecsConflict" in search_text or error_code == 86:
                try:
                    # List all indexes and find the conflicting one
                    indexes = list(collection.list_indexes())
                    for idx in indexes:
                        idx_name = idx.get("name", "")
                        # Check if this index matches our field (for single field) or has the same name
                        if isinstance(index_spec, str):
                            if idx_name == f"{index_spec}_1" or (name and idx_name == name):
                                collection.drop_index(idx_name)
                                break
                        elif name and idx_name == name:
                            collection.drop_index(idx_name)
                            break
                    
                    # Recreate with explicit name
                    if name:
                        collection.create_index(index_spec, name=name, **kwargs)
                    else:
                        collection.create_index(index_spec, **kwargs)
                except Exception as e2:
                    print(f"⚠️ Warning: Could not create index {name or index_spec}: {str(e2)}")
            else:
                print(f"⚠️ Warning: Could not create index {name or index_spec}: {str(e)}")
    
    try:
        # Users collection indexes
        safe_create_index(users_collection, "email", name="email_unique", unique=True)
        safe_create_index(users_collection, "orgId", name="orgId_idx")
        safe_create_index(users_collection, "activeOrgId", name="activeOrgId_idx")
        safe_create_index(users_collection, "firebaseUid", name="firebaseUid_idx")
        safe_create_index(users_collection, [("orgId", 1), ("roleIds", 1)], name="orgId_roleIds_idx")
        
        # Organizations collection indexes
        safe_create_index(organizations_collection, "domain", name="domain_unique", unique=True)
        safe_create_index(organizations_collection, "admins", name="admins_idx")
        
        # Roles collection indexes
        safe_create_index(roles_collection, [("orgId", 1), ("name", 1)], name="orgId_name_unique", unique=True)
        safe_create_index(roles_collection, "orgId", name="roles_orgId_idx")
        
        # Leads collection indexes
        safe_create_index(leads_collection, "orgId", name="leads_orgId_idx")
        safe_create_index(leads_collection, [("orgId", 1), ("ownerId", 1)], name="leads_orgId_ownerId_idx")
        safe_create_index(leads_collection, [("orgId", 1), ("createdAt", -1)], name="leads_orgId_createdAt_idx")
        safe_create_index(leads_collection, [("orgId", 1), ("status", 1)], name="leads_orgId_status_idx")
        safe_create_index(leads_collection, [("orgId", 1), ("converted", 1)], name="leads_orgId_converted_idx")
        safe_create_index(leads_collection, [("orgId", 1), ("email", 1)], name="leads_orgId_email_idx")
        
        # Contacts collection indexes
        safe_create_index(contacts_collection, "orgId", name="contacts_orgId_idx")
        safe_create_index(contacts_collection, [("orgId", 1), ("ownerId", 1)], name="contacts_orgId_ownerId_idx")
        safe_create_index(contacts_collection, [("orgId", 1), ("createdAt", -1)], name="contacts_orgId_createdAt_idx")
        safe_create_index(contacts_collection, [("orgId", 1), ("accountId", 1)], name="contacts_orgId_accountId_idx")
        safe_create_index(contacts_collection, [("orgId", 1), ("email", 1)], name="contacts_orgId_email_idx")
        safe_create_index(contacts_collection, [("orgId", 1), ("deleted", 1)], name="contacts_orgId_deleted_idx")
        
        # Accounts collection indexes
        safe_create_index(accounts_collection, "orgId", name="accounts_orgId_idx")
        safe_create_index(accounts_collection, [("orgId", 1), ("ownerId", 1)], name="accounts_orgId_ownerId_idx")
        safe_create_index(accounts_collection, [("orgId", 1), ("createdAt", -1)], name="accounts_orgId_createdAt_idx")
        safe_create_index(accounts_collection, [("orgId", 1), ("deleted", 1), ("createdAt", -1)], name="accounts_orgId_deleted_createdAt_idx")
        safe_create_index(accounts_collection, [("orgId", 1), ("status", 1)], name="accounts_orgId_status_idx")
        
        # Deals collection indexes
        safe_create_index(deals_collection, "orgId", name="deals_orgId_idx")
        safe_create_index(deals_collection, [("orgId", 1), ("ownerId", 1)], name="deals_orgId_ownerId_idx")
        safe_create_index(deals_collection, [("orgId", 1), ("createdAt", -1)], name="deals_orgId_createdAt_idx")
        safe_create_index(deals_collection, [("orgId", 1), ("accountId", 1)], name="deals_orgId_accountId_idx")
        safe_create_index(deals_collection, [("orgId", 1), ("status", 1)], name="deals_orgId_status_idx")
        safe_create_index(deals_collection, [("orgId", 1), ("stageId", 1)], name="deals_orgId_stageId_idx")
        safe_create_index(deals_collection, [("orgId", 1), ("closeDate", 1)], name="deals_orgId_closeDate_idx")
        
        # Tickets collection indexes
        safe_create_index(tickets_collection, "ticketNumber", name="tickets_ticketNumber_unique", unique=True)
        safe_create_index(tickets_collection, "orgId", name="tickets_orgId_idx")
        safe_create_index(tickets_collection, [("orgId", 1), ("status", 1)], name="tickets_orgId_status_idx")
        safe_create_index(tickets_collection, [("orgId", 1), ("createdAt", -1)], name="tickets_orgId_createdAt_idx")
        safe_create_index(tickets_collection, [("orgId", 1), ("assignedTo", 1)], name="tickets_orgId_assignedTo_idx")
        safe_create_index(tickets_collection, [("orgId", 1), ("priority", 1)], name="tickets_orgId_priority_idx")
        safe_create_index(tickets_collection, [("orgId", 1), ("email", 1)], name="tickets_orgId_email_idx")
        safe_create_index(tickets_collection, "email", name="tickets_email_idx")
        
        # Ticket Comments collection indexes
        safe_create_index(ticket_comments_collection, "ticketId", name="ticketComments_ticketId_idx")
        safe_create_index(ticket_comments_collection, [("orgId", 1), ("ticketId", 1)], name="ticketComments_orgId_ticketId_idx")
        safe_create_index(ticket_comments_collection, [("ticketId", 1), ("createdAt", -1)], name="ticketComments_ticketId_createdAt_idx")
        
        # Conversations collection indexes
        safe_create_index(conversations_collection, "orgId", name="conversations_orgId_idx")
        safe_create_index(conversations_collection, [("orgId", 1), ("updatedAt", -1)], name="conversations_orgId_updatedAt_idx")
        safe_create_index(conversations_collection, "participants", name="conversations_participants_idx")
        safe_create_index(conversations_collection, [("orgId", 1), ("participants", 1)], name="conversations_orgId_participants_idx")
        
        # Messages collection indexes
        safe_create_index(messages_collection, "conversationId", name="messages_conversationId_idx")
        safe_create_index(messages_collection, [("conversationId", 1), ("createdAt", -1)], name="messages_conversationId_createdAt_idx")
        safe_create_index(messages_collection, [("orgId", 1), ("conversationId", 1)], name="messages_orgId_conversationId_idx")
        safe_create_index(messages_collection, [("orgId", 1), ("senderId", 1)], name="messages_orgId_senderId_idx")
        
        # Activities collection indexes
        safe_create_index(activities_collection, "orgId", name="activities_orgId_idx")
        safe_create_index(activities_collection, [("orgId", 1), ("entityType", 1), ("entityId", 1)], name="activities_orgId_entity_idx")
        safe_create_index(activities_collection, [("orgId", 1), ("createdAt", -1)], name="activities_orgId_createdAt_idx")
        safe_create_index(activities_collection, [("entityType", 1), ("entityId", 1)], name="activities_entity_idx")
        safe_create_index(activities_collection, [("orgId", 1), ("userId", 1)], name="activities_orgId_userId_idx")
        
        # Campaigns collection indexes
        safe_create_index(campaigns_collection, "orgId", name="campaigns_orgId_idx")
        safe_create_index(campaigns_collection, [("orgId", 1), ("status", 1)], name="campaigns_orgId_status_idx")
        safe_create_index(campaigns_collection, [("orgId", 1), ("createdAt", -1)], name="campaigns_orgId_createdAt_idx")
        
        # Segments collection indexes
        safe_create_index(segments_collection, "orgId", name="segments_orgId_idx")
        safe_create_index(segments_collection, [("orgId", 1), ("type", 1)], name="segments_orgId_type_idx")
        safe_create_index(segments_collection, [("orgId", 1), ("createdAt", -1)], name="segments_orgId_createdAt_idx")
        
        # Templates collection indexes
        safe_create_index(templates_collection, "orgId", name="templates_orgId_idx")
        safe_create_index(templates_collection, [("orgId", 1), ("type", 1)], name="templates_orgId_type_idx")
        safe_create_index(templates_collection, [("orgId", 1), ("createdAt", -1)], name="templates_orgId_createdAt_idx")
        
        # Communications collection indexes
        safe_create_index(communications_collection, "orgId", name="communications_orgId_idx")
        safe_create_index(communications_collection, [("orgId", 1), ("type", 1)], name="communications_orgId_type_idx")
        safe_create_index(communications_collection, [("orgId", 1), ("createdAt", -1)], name="communications_orgId_createdAt_idx")
        safe_create_index(communications_collection, [("orgId", 1), ("from", 1)], name="communications_orgId_from_idx")
        safe_create_index(communications_collection, [("orgId", 1), ("to", 1)], name="communications_orgId_to_idx")
        
        # Files collection indexes
        safe_create_index(files_collection, "orgId", name="files_orgId_idx")
        safe_create_index(files_collection, [("orgId", 1), ("uploaderId", 1)], name="files_orgId_uploaderId_idx")
        safe_create_index(files_collection, [("orgId", 1), ("createdAt", -1)], name="files_orgId_createdAt_idx")
        
        # Webforms collection indexes
        safe_create_index(webforms_collection, "publicToken", name="webforms_publicToken_unique", unique=True)
        safe_create_index(webforms_collection, "orgId", name="webforms_orgId_idx")
        safe_create_index(webforms_collection, [("orgId", 1), ("createdAt", -1)], name="webforms_orgId_createdAt_idx")
        
        # Integrations collection indexes
        safe_create_index(integrations_collection, "orgId", name="integrations_orgId_idx")
        safe_create_index(integrations_collection, [("orgId", 1), ("type", 1)], name="integrations_orgId_type_idx")
        safe_create_index(integrations_collection, [("orgId", 1), ("enabled", 1)], name="integrations_orgId_enabled_idx")
        
        # Reports collection indexes
        safe_create_index(reports_collection, "orgId", name="reports_orgId_idx")
        safe_create_index(reports_collection, [("orgId", 1), ("type", 1)], name="reports_orgId_type_idx")
        safe_create_index(reports_collection, [("orgId", 1), ("createdAt", -1)], name="reports_orgId_createdAt_idx")
        
        # Exports collection indexes
        safe_create_index(exports_collection, "orgId", name="exports_orgId_idx")
        safe_create_index(exports_collection, [("orgId", 1), ("status", 1)], name="exports_orgId_status_idx")
        safe_create_index(exports_collection, [("orgId", 1), ("createdAt", -1)], name="exports_orgId_createdAt_idx")
        
        # Audit collection indexes
        safe_create_index(audit_collection, "orgId", name="audit_orgId_idx")
        safe_create_index(audit_collection, [("orgId", 1), ("actorId", 1)], name="audit_orgId_actorId_idx")
        safe_create_index(audit_collection, [("orgId", 1), ("createdAt", -1)], name="audit_orgId_createdAt_idx")
        safe_create_index(audit_collection, [("orgId", 1), ("action", 1)], name="audit_orgId_action_idx")
        safe_create_index(audit_collection, [("entityType", 1), ("entityId", 1)], name="audit_entity_idx")
        
        # Jira Integration collection indexes
        safe_create_index(jira_integration_collection, "ticketId", name="jiraIntegration_ticketId_unique", unique=True)
        safe_create_index(jira_integration_collection, "jiraIssueKey", name="jiraIntegration_jiraIssueKey_unique", unique=True)
        safe_create_index(jira_integration_collection, "orgId", name="jiraIntegration_orgId_idx")
        safe_create_index(jira_integration_collection, [("orgId", 1), ("ticketId", 1)], name="jiraIntegration_orgId_ticketId_idx")
        
        print("[OK] Database indexes created successfully")
    except Exception as e:
        print(f"⚠️ Warning: Failed to create some indexes: {str(e)}")

def update_validators():
    """Update MongoDB validators to support multi-tenant orgId (string or array)"""
    try:
        # Update users collection validator to allow orgId as string or array
        new_validator = {
            "$jsonSchema": {
                "bsonType": "object",
                "required": ["email", "name", "createdAt", "updatedAt"],
                "properties": {
                    "email": {
                        "bsonType": "string"
                    },
                    "name": {
                        "bsonType": "string"
                    },
                    "password": {
                        "bsonType": ["string", "null"]
                    },
                    "picture": {
                        "bsonType": ["string", "null"]
                    },
                    "roleIds": {
                        "bsonType": ["array"],
                        "items": {
                            "bsonType": "string"
                        }
                    },
                    "orgId": {
                        "anyOf": [
                            {"bsonType": "string"},
                            {"bsonType": "array", "items": {"bsonType": "string"}},
                            {"bsonType": "null"}
                        ]
                    },
                    "activeOrgId": {
                        "bsonType": ["string", "null"]
                    },
                    "lastSeenAt": {
                        "bsonType": ["date", "null"]
                    },
                    "createdAt": {
                        "bsonType": "date"
                    },
                    "firebaseUid": {
                        "bsonType": ["string", "null"]
                    },
                    "updatedAt": {
                        "bsonType": "date"
                    },
                    "isAdmin": {
                        "bsonType": ["bool", "null"]
                    }
                }
            }
        }
        
        # Try to update validator
        try:
            db.command({
                "collMod": "users",
                "validator": new_validator,
                "validationLevel": "moderate",
                "validationAction": "error"
            })
            print("[OK] Successfully updated users collection validator")
        except Exception as e:
            # If validator doesn't exist or can't be updated, try to drop and recreate
            error_str = str(e).lower()
            if "validator" in error_str or "schema" in error_str or "121" in error_str:
                try:
                    # Drop existing validator first
                    try:
                        db.command({
                            "collMod": "users",
                            "validator": {}
                        })
                    except:
                        pass  # Validator might not exist
                    
                    # Create new one
                    db.command({
                        "collMod": "users",
                        "validator": new_validator,
                        "validationLevel": "moderate",
                        "validationAction": "error"
                    })
                    print("[OK] Successfully recreated users collection validator")
                except Exception as e2:
                    print(f"[WARN] Could not update validator: {str(e2)}")
            else:
                print(f"[WARN] Could not update validator: {str(e)}")
    except Exception as e:
        print(f"[WARN] Failed to update validators: {str(e)}")

def initialize_database():
    """Initialize database connection and indexes"""
    test_connection()
    create_indexes()
    update_validators()

