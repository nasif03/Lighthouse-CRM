"""Application configuration and settings"""
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# MongoDB Configuration
MONGO_URI = os.getenv("MONGO_URI")
if not MONGO_URI:
    raise ValueError("MONGO_URI environment variable is not set")

# Firebase Configuration
FIREBASE_PROJECT_ID = os.getenv("FIREBASE_PROJECT_ID", "lighthousecrm-6caf2")
FIREBASE_API_KEY = os.getenv("FIREBASE_API_KEY", "AIzaSyB_nk1k3gqAr1KCEljJdEtAcGIlGRArrSw")

# CORS Configuration
CORS_ORIGINS = [
    "http://localhost:5173",
    "http://localhost:3000"
]

# Server Configuration
PORT = int(os.getenv("PORT", 3000))
HOST = os.getenv("HOST", "0.0.0.0")

# Cache Configuration
USER_CACHE_TTL_MINUTES = 5

# Jira Configuration
JIRA_SERVER = os.getenv("JIRA_SERVER", "https://lighthouse-crm.atlassian.net")
JIRA_EMAIL = os.getenv("JIRA_EMAIL", "niloy.ashraf@northsouth.edu")
JIRA_TOKEN = os.getenv("JIRA_TOKEN")

