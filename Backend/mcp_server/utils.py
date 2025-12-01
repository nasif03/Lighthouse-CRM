"""Shared utilities for MCP server"""
import os
import httpx
from typing import Optional
from config.settings import PORT, HOST

# Backend API base URL - defaults to localhost but can be configured
API_BASE_URL = os.getenv("CRM_API_BASE_URL", f"http://{HOST}:{PORT}")

# Global HTTP client for making API requests
http_client = httpx.AsyncClient(timeout=30.0)

# LOCAL TESTING ONLY: hardcoded Lighthouse CRM auth token.
# This is a single-user hardcoded token used for all MCP calls.
# WARNING: This is insecure and should NOT be used in production.
# For production, implement a proper login flow or token management system.
# MCP is acting as a single fixed Lighthouse CRM user determined by this token.
# All MCP tools will run under that user's org and permissions.
# For multi-user or dynamic login, a separate login flow/tool would be needed.
CRM_API_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjdjNzQ5NTFmNjBhMDE0NzE3ZjFlMzA4ZDZiMjgwZjQ4ZjFlODhmZGEiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiUy5NLiBOaWxveSBBc2hyYWYgMjIzMTg2NjY0MiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BQ2c4b2NMSnF1ckFaSTRIcXc2ekxQalcxTldYX0VrUUIxWl8zVHVfc0g4N25yR05DOVJfOWc9czk2LWMiLCJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vbGlnaHRob3VzZWNybS02Y2FmMiIsImF1ZCI6ImxpZ2h0aG91c2Vjcm0tNmNhZjIiLCJhdXRoX3RpbWUiOjE3NjQ0MDI0OTYsInVzZXJfaWQiOiJFdTExRUZ6dDU4UmNsNjJrMXRHaElya3ZsQ0ozIiwic3ViIjoiRXUxMUVGenQ1OFJjbDYyazF0R2hJcmt2bENKMyIsImlhdCI6MTc2NDYyOTg5OSwiZXhwIjoxNzY0NjMzNDk5LCJlbWFpbCI6Im5pbG95LmFzaHJhZkBub3J0aHNvdXRoLmVkdSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJmaXJlYmFzZSI6eyJpZGVudGl0aWVzIjp7Imdvb2dsZS5jb20iOlsiMTE0NDEwMzQwMjE5OTE2ODU2NTAwIl0sImVtYWlsIjpbIm5pbG95LmFzaHJhZkBub3J0aHNvdXRoLmVkdSJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifX0.Ra1KuUj4Y0BGApVKLq9TA25QHMJACYJlwRZ2qbr5w27f9RGhtToDyiCMCyGce-sXGL8713ClttrrLEZ7tiqj5GSZUznBFZqixuAAPxwDUgmxCHPUXfUfiW8X21s_GWPn80w3xvCGDx8iBcRj36460JbXtl6d24RprJOsnz2WUy02LR-9dHW_YbVoQs-Yq8XzNValTsX6crZNmy7s-rbzYe6wUhuNPpvx8hsoOjSZMQiGF0fkLzqW7W0ET2XEKqw6Ld1o31kyZ71xBtDdJKYhrAz4IhFl3cxraqdQAwrkj5yWNQDnDue5_fsAYcgRhxO1Yy9gfhYcBOB1FjbjvUlmPA"


def get_auth_headers(token: Optional[str] = None) -> dict:
    """
    Get authorization headers for API requests.
    
    For local testing: If no token is provided, uses the hardcoded CRM_API_TOKEN.
    This allows MCP tools to authenticate automatically without requiring Claude
    to pass tokens for each call.
    
    Args:
        token: Optional Firebase authentication token. If None, uses CRM_API_TOKEN.
    
    Returns:
        Dictionary with Content-Type and Authorization headers.
    """
    headers = {"Content-Type": "application/json"}
    
    # Use provided token, or fall back to hardcoded token for local testing
    auth_token = token or CRM_API_TOKEN
    
    if auth_token:
        headers["Authorization"] = f"Bearer {auth_token}"
    
    return headers

