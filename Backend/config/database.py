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
        safe_create_index(users_collection, "firebaseUid", name="firebaseUid_idx")
        
        # Organizations collection indexes
        safe_create_index(organizations_collection, "domain", name="domain_unique", unique=True)
        
        # Leads collection indexes
        safe_create_index(leads_collection, "orgId", name="leads_orgId_idx")
        safe_create_index(leads_collection, [("orgId", 1), ("createdAt", -1)], name="leads_orgId_createdAt_idx")
        safe_create_index(leads_collection, [("orgId", 1), ("status", 1)], name="leads_orgId_status_idx")
        
        # Contacts collection indexes
        safe_create_index(contacts_collection, "orgId", name="contacts_orgId_idx")
        safe_create_index(contacts_collection, [("orgId", 1), ("createdAt", -1)], name="contacts_orgId_createdAt_idx")
        safe_create_index(contacts_collection, [("orgId", 1), ("accountId", 1)], name="contacts_orgId_accountId_idx")
        
        # Accounts collection indexes
        safe_create_index(accounts_collection, "orgId", name="accounts_orgId_idx")
        safe_create_index(accounts_collection, [("orgId", 1), ("createdAt", -1)], name="accounts_orgId_createdAt_idx")
        # Compound index for common query pattern: orgId + deleted + createdAt (for sorting)
        safe_create_index(accounts_collection, [("orgId", 1), ("deleted", 1), ("createdAt", -1)], name="accounts_orgId_deleted_createdAt_idx")
        
        # Deals collection indexes
        safe_create_index(deals_collection, "orgId", name="deals_orgId_idx")
        safe_create_index(deals_collection, [("orgId", 1), ("createdAt", -1)], name="deals_orgId_createdAt_idx")
        safe_create_index(deals_collection, [("orgId", 1), ("accountId", 1)], name="deals_orgId_accountId_idx")
        safe_create_index(deals_collection, [("orgId", 1), ("status", 1)], name="deals_orgId_status_idx")
        
        # Activities collection indexes
        safe_create_index(activities_collection, "orgId", name="activities_orgId_idx")
        safe_create_index(activities_collection, [("orgId", 1), ("entityType", 1), ("entityId", 1)], name="activities_orgId_entity_idx")
        safe_create_index(activities_collection, [("orgId", 1), ("createdAt", -1)], name="activities_orgId_createdAt_idx")
        safe_create_index(activities_collection, [("entityType", 1), ("entityId", 1)], name="activities_entity_idx")
        
        # Tickets collection indexes
        safe_create_index(tickets_collection, "orgId", name="tickets_orgId_idx")
        safe_create_index(tickets_collection, "ticketNumber", name="tickets_ticketNumber_idx", unique=True)
        safe_create_index(tickets_collection, [("orgId", 1), ("status", 1)], name="tickets_orgId_status_idx")
        safe_create_index(tickets_collection, [("orgId", 1), ("createdAt", -1)], name="tickets_orgId_createdAt_idx")
        safe_create_index(tickets_collection, "email", name="tickets_email_idx")
        
        print("✅ Database indexes created successfully")
    except Exception as e:
        print(f"⚠️ Warning: Failed to create some indexes: {str(e)}")

def initialize_database():
    """Initialize database connection and indexes"""
    test_connection()
    create_indexes()

