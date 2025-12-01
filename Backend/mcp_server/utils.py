"""Shared utilities for MCP server"""
import os
import httpx
from typing import Optional
from config.settings import PORT, HOST

# Backend API base URL - defaults to localhost but can be configured
API_BASE_URL = os.getenv("CRM_API_BASE_URL", f"http://{HOST}:{PORT}")

# Global HTTP client for making API requests
http_client = httpx.AsyncClient(timeout=30.0)


def get_auth_headers(token: Optional[str] = None) -> dict:
    """Get authorization headers for API requests"""
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers

