from pydantic import BaseModel
from typing import Optional, List


# ---- SUMMARY MODELS ----

class FirefliesSummary(BaseModel):
    overview: Optional[str]
    short_summary: Optional[str]


# ---- TRANSCRIPT MODEL ----

class FirefliesTranscript(BaseModel):
    id: str
    title: Optional[str]
    date: int
    transcript_url: Optional[str]
    summary: Optional[FirefliesSummary]


# ---- RESPONSE WRAPPER (optional) ----

class FirefliesTranscriptListResponse(BaseModel):
    transcripts: List[FirefliesTranscript]
