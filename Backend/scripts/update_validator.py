"""Script to update MongoDB validator to allow orgId as string or array"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from pymongo import MongoClient
from config.settings import MONGO_URI

def update_users_validator():
    """Update users collection validator to allow orgId as string or array"""
    client = MongoClient(MONGO_URI)
    db = client.lighthousecrm
    users_collection = db.users
    
    try:
        # Get current validator
        collection_info = db.list_collections(filter={"name": "users"})
        collection_info = list(collection_info)
        
        if collection_info:
            current_options = collection_info[0].get("options", {})
            current_validator = current_options.get("validator", {})
            
            print(f"Current validator: {current_validator}")
            
            # Create new validator that allows orgId as string or array
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
                            "bsonType": ["string", "array", "null"],
                            # If array, items must be strings
                            "items": {
                                "bsonType": "string"
                            }
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
            
            # Update validator
            db.command({
                "collMod": "users",
                "validator": new_validator,
                "validationLevel": "moderate",  # Only validate on updates/inserts, not on existing docs
                "validationAction": "error"
            })
            
            print("✅ Successfully updated users collection validator")
            print("   orgId can now be: string, array of strings, or null")
        else:
            print("⚠️ Users collection not found")
            
    except Exception as e:
        print(f"❌ Error updating validator: {str(e)}")
        # Try to drop validator and recreate
        try:
            print("Attempting to drop existing validator...")
            db.command({
                "collMod": "users",
                "validator": {}
            })
            print("✅ Dropped existing validator")
            
            # Now create new one
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
                            "bsonType": ["string", "array", "null"],
                            "items": {
                                "bsonType": "string"
                            }
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
            
            db.command({
                "collMod": "users",
                "validator": new_validator,
                "validationLevel": "moderate",
                "validationAction": "error"
            })
            print("✅ Successfully created new validator")
        except Exception as e2:
            print(f"❌ Error in fallback: {str(e2)}")
            raise

if __name__ == "__main__":
    update_users_validator()

