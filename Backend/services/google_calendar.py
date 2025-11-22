"""Google Calendar service helpers."""
from __future__ import annotations

import uuid
from typing import List, Optional

from google.auth.transport.requests import Request
from googleapiclient.discovery import build

from services.gmail import (
    CALENDAR_SCOPE,
    get_credentials,
    save_credentials,
)


class CalendarAuthError(Exception):
    """Raised when Google Calendar credentials are missing or insufficient."""


def _get_calendar_service(user_email: str):
    creds = get_credentials(user_email)
    if not creds:
        raise CalendarAuthError("Google account is not connected. Please connect Gmail/Calendar.")

    # Refresh credentials if needed
    if creds.expired and creds.refresh_token:
        creds.refresh(Request())
        save_credentials(user_email, creds)

    if not creds.valid:
        raise CalendarAuthError("Stored Google credentials are invalid. Please reconnect.")

    # Ensure Calendar scope is present
    scopes = set(creds.scopes or [])
    if CALENDAR_SCOPE not in scopes:
        raise CalendarAuthError(
            "Google Calendar permission was not granted. Please reconnect your Google account."
        )

    return build("calendar", "v3", credentials=creds)


def create_calendar_event(
    *,
    user_email: str,
    title: str,
    start_time: str,
    end_time: str,
    attendees: Optional[List[str]] = None,
    description: Optional[str] = None,
    timezone: str = "UTC",
):
    """Create a Calendar event with a Google Meet link."""
    service = _get_calendar_service(user_email)

    body = {
        "summary": title,
        "description": description,
        "start": {"dateTime": start_time, "timeZone": timezone},
        "end": {"dateTime": end_time, "timeZone": timezone},
        "conferenceData": {
            "createRequest": {
                "requestId": str(uuid.uuid4()),
                "conferenceSolutionKey": {"type": "hangoutsMeet"},
            }
        },
    }

    if attendees:
        body["attendees"] = [{"email": email} for email in attendees]

    event = (
        service.events()
        .insert(
            calendarId="primary",
            body=body,
            conferenceDataVersion=1,
            sendUpdates="all",
        )
        .execute()
    )

    return event

