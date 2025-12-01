from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException
from googleapiclient.errors import HttpError
from pydantic import BaseModel, Field

from api.dependencies import get_current_user
from services.google_calendar import (
    CalendarAuthError,
    create_calendar_event,
    list_calendar_events,
)
from config.settings import FIREFLIES_BOT_EMAIL

router = APIRouter(prefix="/api/calendar", tags=["calendar"])


class MeetingRequest(BaseModel):
    title: str = Field(..., description="Event title")
    start_time: str = Field(..., description="ISO 8601 start time")
    end_time: str = Field(..., description="ISO 8601 end time")
    attendees: List[str] = Field(default_factory=list, description="Participant emails")
    description: Optional[str] = None
    timezone: str = Field(default="UTC", description="IANA timezone, e.g., 'UTC' or 'Asia/Dhaka'")


class MeetingResponse(BaseModel):
    event_id: str
    hangout_link: Optional[str]
    html_link: Optional[str]
    start_time: str
    end_time: str


@router.post("/meetings", response_model=MeetingResponse)
def create_meeting(
    data: MeetingRequest,
    current_user: dict = Depends(get_current_user),
):
    user_doc = current_user.get("user_doc")
    if not user_doc:
        raise HTTPException(status_code=404, detail="User not found")

    user_email = user_doc.get("email")
    if not user_email:
        raise HTTPException(status_code=400, detail="User email missing")

    # Build attendee list and optionally append Fireflies bot
    attendees = list(dict.fromkeys(data.attendees or []))  # dedupe while preserving order

    # Only add Fireflies when:
    # - Bot email is configured
    # - Meeting has at least two human participants (organizer + at least one attendee)
    if FIREFLIES_BOT_EMAIL:
        participant_count = 1 + len(attendees)  # organizer + attendees
        if participant_count >= 2 and FIREFLIES_BOT_EMAIL not in attendees:
            attendees.append(FIREFLIES_BOT_EMAIL)

    try:
        event = create_calendar_event(
            user_email=user_email,
            title=data.title,
            start_time=data.start_time,
            end_time=data.end_time,
            attendees=attendees,
            description=data.description,
            timezone=data.timezone,
        )
        return MeetingResponse(
            event_id=event.get("id"),
            hangout_link=event.get("hangoutLink") or event.get("conferenceData", {})
            .get("entryPoints", [{}])[0]
            .get("uri"),
            html_link=event.get("htmlLink"),
            start_time=event.get("start", {}).get("dateTime", data.start_time),
            end_time=event.get("end", {}).get("dateTime", data.end_time),
        )
    except CalendarAuthError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except HttpError as exc:
        raise HTTPException(status_code=400, detail=f"Google Calendar error: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Failed to create meeting") from exc


@router.get("/meetings")
def get_meetings(
    time_min: Optional[str] = None,
    time_max: Optional[str] = None,
    max_results: int = 50,
    current_user: dict = Depends(get_current_user),
):
    """Get upcoming meetings from Google Calendar"""
    user_doc = current_user.get("user_doc")
    if not user_doc:
        raise HTTPException(status_code=404, detail="User not found")

    user_email = user_doc.get("email")
    if not user_email:
        raise HTTPException(status_code=400, detail="User email missing")

    try:
        events = list_calendar_events(
            user_email=user_email,
            time_min=time_min,
            time_max=time_max,
            max_results=max_results,
        )

        # Format events for response
        meetings = []
        for event in events:
            start = event.get("start", {}).get("dateTime") or event.get("start", {}).get("date")
            end = event.get("end", {}).get("dateTime") or event.get("end", {}).get("date")
            
            # Extract Google Meet link
            hangout_link = event.get("hangoutLink")
            if not hangout_link:
                conference_data = event.get("conferenceData", {})
                entry_points = conference_data.get("entryPoints", [])
                if entry_points:
                    hangout_link = entry_points[0].get("uri")

            meetings.append({
                "event_id": event.get("id"),
                "title": event.get("summary", "No Title"),
                "description": event.get("description"),
                "start_time": start,
                "end_time": end,
                "hangout_link": hangout_link,
                "html_link": event.get("htmlLink"),
                "attendees": [att.get("email") for att in event.get("attendees", [])],
                "status": event.get("status", "confirmed"),
            })

        return {"meetings": meetings}
    except CalendarAuthError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Failed to fetch meetings: {str(exc)}") from exc