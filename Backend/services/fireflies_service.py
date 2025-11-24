import os
import requests
from typing import List
from models.fireflies import FirefliesTranscript

FIRELIES_API = "https://api.fireflies.ai/graphql"
FIRELIES_KEY = "fdf6c93e-f439-424a-87bf-bfd16e0bfd95"


def fireflies_graphql(query: str, variables=None):
    if variables is None:
        variables = {}

    headers = {
        "Authorization": f"Bearer {FIRELIES_KEY}",
        "Content-Type": "application/json"
    }

    response = requests.post(
        FIRELIES_API,
        json={"query": query, "variables": variables},
        headers=headers
    )

    data = response.json()

    if "errors" in data:
        print("Fireflies API error:", data)
        return None

    return data["data"]


# -------- Fetch transcripts ------

def get_transcripts(limit=10) -> List[FirefliesTranscript]:
    query = """
    query GetTranscripts($limit: Int!) {
      transcripts(limit: $limit) {
        id
        title
        date
        transcript_url
        summary {
          overview
          short_summary
        }
      }
    }
    """

    data = fireflies_graphql(query, {"limit": limit})

    if not data or "transcripts" not in data:
        return []

    return [FirefliesTranscript(**t) for t in data["transcripts"]]


# -------- Fetch Meeting by ID ------

def get_transcript_by_id(transcript_id: str) -> FirefliesTranscript | None:
    query = """
    query GetTranscript($id: ID!) {
      transcript(id: $id) {
        id
        title
        date
        transcript_url
        summary {
          overview
          short_summary
        }
      }
    }
    """

    data = fireflies_graphql(query, {"id": transcript_id})

    if not data or "transcript" not in data or data["transcript"] is None:
        return None

    return FirefliesTranscript(**data["transcript"])


    # -------- Sync transcripts to MongoDB --------
from config.database import db  # your existing MongoDB connection

def sync_transcripts_to_db(limit: int = 10) -> int:
    """
    Fetch transcripts from Fireflies and save/update them in MongoDB.
    Returns the number of transcripts saved.
    """
    transcripts = get_transcripts(limit=limit)

    if not transcripts:
        print("No transcripts returned from Fireflies API.")
        return 0

    col = db["fireflies_transcripts"]  # collection name in MongoDB

    for t in transcripts:
        col.update_one({"id": t.id}, {"$set": t.dict()}, upsert=True)

    print(f"Saved {len(transcripts)} transcripts to MongoDB.")
    return len(transcripts)

