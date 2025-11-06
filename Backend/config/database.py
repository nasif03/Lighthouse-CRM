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
    try:
        # Users collection indexes
        users_collection.create_index("email", unique=True)
        users_collection.create_index("orgId")
        users_collection.create_index("firebaseUid")
        
        # Organizations collection indexes
        organizations_collection.create_index("domain", unique=True)
        
        # Leads collection indexes
        leads_collection.create_index("orgId")
        leads_collection.create_index([("orgId", 1), ("createdAt", -1)])
        leads_collection.create_index([("orgId", 1), ("status", 1)])
        
        # Contacts collection indexes
        contacts_collection.create_index("orgId")
        contacts_collection.create_index([("orgId", 1), ("createdAt", -1)])
        contacts_collection.create_index([("orgId", 1), ("accountId", 1)])
        
        # Accounts collection indexes
        accounts_collection.create_index("orgId")
        accounts_collection.create_index([("orgId", 1), ("createdAt", -1)])
        
        # Deals collection indexes
        deals_collection.create_index("orgId")
        deals_collection.create_index([("orgId", 1), ("createdAt", -1)])
        deals_collection.create_index([("orgId", 1), ("accountId", 1)])
        deals_collection.create_index([("orgId", 1), ("status", 1)])
        
        print("✅ Database indexes created successfully")
    except Exception as e:
        print(f"⚠️ Warning: Failed to create some indexes: {str(e)}")

def initialize_database():
    """Initialize database connection and indexes"""
    test_connection()
    create_indexes()

