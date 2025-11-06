"""Firebase initialization and configuration"""
import firebase_admin
from firebase_admin import credentials
from config.settings import FIREBASE_PROJECT_ID

def initialize_firebase():
    """Initialize Firebase Admin SDK"""
    try:
        # Try to get existing default app
        firebase_admin.get_app()
    except ValueError:
        # Initialize with default credentials if available
        # Otherwise, we'll use REST API verification
        try:
            cred = credentials.ApplicationDefault()
            firebase_admin.initialize_app(cred, {
                'projectId': FIREBASE_PROJECT_ID,
            })
        except Exception:
            # If ApplicationDefault fails, we'll verify tokens via REST API
            pass

