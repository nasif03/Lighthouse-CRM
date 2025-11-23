"""Lighthouse CRM Backend - Main Application Entry Point"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from config.settings import CORS_ORIGINS, PORT, HOST
from config.database import initialize_database
from services.firebase import initialize_firebase
from api.routes import auth, leads, contacts, accounts, deals, activities, tenants, tickets, dashboard, organizations, employees, roles, jira, gmail

# Initialize FastAPI app
app = FastAPI(title="Lighthouse CRM Backend")

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize services
initialize_database()
initialize_firebase()

# Health check
@app.get("/")
async def root():
    return {"message": "Lighthouse CRM Backend API"}

# Include routers
app.include_router(auth.router)
app.include_router(leads.router)
app.include_router(contacts.router)
app.include_router(accounts.router)
app.include_router(deals.router)
app.include_router(activities.router)
app.include_router(tenants.router)
app.include_router(tickets.router)
app.include_router(dashboard.router)
app.include_router(organizations.router)
app.include_router(employees.router)
app.include_router(roles.router)
app.include_router(jira.router)
app.include_router(gmail.router)

if __name__ == "__main__":
    uvicorn.run(app, host=HOST, port=PORT)
