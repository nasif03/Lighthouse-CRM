"""Lighthouse CRM Backend - Main Application Entry Point"""
from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
import uvicorn

from config.settings import CORS_ORIGINS, PORT, HOST
from config.database import initialize_database
from services.firebase import initialize_firebase
from api.routes import auth, leads, contacts, accounts, deals, activities, tenants, tickets, dashboard, organizations, employees, roles, jira, gmail, chat, support_chat
from api.routes.fireflies_routes import router as fireflies_router
from api.routes import calendar_routes
from telegram_bot import init_telegram_bot, shutdown_telegram_bot, telegram_webhook

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

# Add validation error handler
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Handle validation errors and return detailed error messages"""
    print(f"Validation error on {request.url.path}: {exc.errors()}")
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={"detail": exc.errors(), "body": str(exc.body)},
    )

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
app.include_router(fireflies_router)
app.include_router(calendar_routes.router)
app.include_router(chat.router)
app.include_router(support_chat.router)

# Telegram webhook route
app.add_api_route(
    "/telegram_webhook",
    telegram_webhook,
    methods=["POST"],
    name="telegram_webhook",
)

# Startup event for Telegram bot
@app.on_event("startup")
async def startup_telegram():
    await init_telegram_bot()

# Shutdown event for Telegram bot
@app.on_event("shutdown")
async def shutdown_telegram():
    await shutdown_telegram_bot()

if __name__ == "__main__":
    uvicorn.run(app, host=HOST, port=PORT)
