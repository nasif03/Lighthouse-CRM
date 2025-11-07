"""Firebase initialization and configuration"""
import os
import firebase_admin
from firebase_admin import credentials
from config.settings import FIREBASE_PROJECT_ID

def initialize_firebase():
    """Initialize Firebase Admin SDK"""
    try:
        # Try to get existing default app
        firebase_admin.get_app()
        print("✅ Firebase Admin SDK already initialized")
    except ValueError:
        # Try to load from service account JSON file (in Backend directory)
        service_account_path = os.path.join(os.path.dirname(__file__), '..', 'firebase_admin_sdk.json')
        
        if os.path.exists(service_account_path):
            try:
                cred = credentials.Certificate(service_account_path)
                firebase_admin.initialize_app(cred, {
                    'projectId': FIREBASE_PROJECT_ID,
                })
                print("✅ Firebase Admin SDK initialized from firebase_admin_sdk.json")
            except Exception as e:
                print(f"⚠️  Failed to initialize Firebase Admin SDK from file: {str(e)}")
                print("⚠️  Firebase Admin SDK not available - will use REST API or JWT decode")
        else:
            print(f"⚠️  Firebase credentials file not found at: {service_account_path}")
            print("⚠️  Firebase Admin SDK not available - will use REST API or JWT decode")

