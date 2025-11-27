/**
 * Utility functions for date formatting and handling
 */

/**
 * Parse a date string and ensure it's treated as UTC if no timezone is specified
 */
export function parseUTCDate(dateString: string): Date {
  if (!dateString) return new Date();
  
  // If date string ends with 'Z', it's already UTC
  if (dateString.endsWith('Z')) {
    return new Date(dateString);
  }
  
  // If date string is ISO format without timezone, append 'Z' to treat as UTC
  if (dateString.includes('T') && !dateString.match(/[+-]\d{2}:?\d{2}$/)) {
    return new Date(dateString + 'Z');
  }
  
  // Otherwise, parse as-is (might be local time)
  return new Date(dateString);
}

/**
 * Format relative time (e.g., "2h ago", "3d ago")
 */
export function formatRelativeTime(dateString: string): string {
  if (!dateString) return '';
  
  const date = parseUTCDate(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);
  
  if (diffInSeconds < 60) return 'Just now';
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}m ago`;
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}h ago`;
  if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)}d ago`;
  
  return date.toLocaleDateString();
}

