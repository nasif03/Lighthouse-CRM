from typing import List

from fastapi import APIRouter, Query

from models.fireflies import FirefliesTranscript
from services.fireflies_service import (
    get_transcript_by_id,
    get_transcripts,
    sync_transcripts_to_db,
)

router = APIRouter(prefix="/api/fireflies", tags=["fireflies"])


@router.get("/transcripts", response_model=List[FirefliesTranscript])
def list_transcripts(limit: int = 10):
    return get_transcripts(limit)


@router.get("/transcripts/{transcript_id}", response_model=FirefliesTranscript)
def get_single_transcript(transcript_id: str):
    transcript = get_transcript_by_id(transcript_id)
    if transcript is None:
        return {"error": "Transcript not found"}
    return transcript


@router.get("/sync_transcripts")
def sync_transcripts(limit: int = Query(10, description="Number of transcripts to fetch")):
    """
    Fetch transcripts from Fireflies and save them to MongoDB.
    """
    saved_count = sync_transcripts_to_db(limit)
    return {"saved_transcripts": saved_count}
