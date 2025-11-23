from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException
from googleapiclient.errors import HttpError
from pydantic import BaseModel, Field

from api.dependencies import get_current_user
from services.google_calendar import (
    CalendarAuthError,
    create_calendar_event,
)

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

    try:
        event = create_calendar_event(
            user_email=user_email,
            title=data.title,
            start_time=data.start_time,
            end_time=data.end_time,
            attendees=data.attendees,
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