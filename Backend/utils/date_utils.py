"""Utility functions for date formatting in API responses"""
from datetime import datetime
from typing import Optional


def format_iso_utc(dt: Optional[datetime]) -> str:
    """
    Format a datetime object as ISO 8601 string with UTC timezone indicator.
    
    Args:
        dt: datetime object (assumed to be UTC, can be None)
    
    Returns:
        ISO 8601 formatted string with 'Z' suffix (e.g., "2024-01-01T12:00:00Z")
    """
    if dt is None:
        return ""
    
    # Ensure the datetime is treated as UTC and add 'Z' suffix
    if dt.tzinfo is None:
        # Naive datetime is assumed to be UTC - add 'Z' suffix
        iso_str = dt.isoformat()
        # Add 'Z' if not already present
        if not iso_str.endswith('Z') and '+' not in iso_str:
            return iso_str + 'Z'
        return iso_str
    else:
        # Timezone-aware datetime - format with Z
        return dt.isoformat().replace('+00:00', 'Z').replace('-00:00', 'Z')
    

def format_iso_utc_or_empty(dt: Optional[datetime]) -> str:
    """
    Format a datetime object as ISO UTC string, or return empty string if None.
    
    Args:
        dt: datetime object or None
    
    Returns:
        ISO 8601 formatted string with 'Z' suffix, or empty string
    """
    return format_iso_utc(dt)

