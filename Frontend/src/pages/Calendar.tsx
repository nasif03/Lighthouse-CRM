import { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '../store/authStore';
import { apiGet } from '../utils/api';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import { formatRelativeTime, parseUTCDate } from '../utils/dateUtils';

type Meeting = {
  event_id: string;
  title: string;
  description?: string;
  start_time: string;
  end_time: string;
  hangout_link?: string;
  html_link?: string;
  attendees: string[];
  status: string;
};

export default function Calendar() {
  const { token, user } = useAuthStore();
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchMeetings = useCallback(async () => {
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      
      // Get upcoming meetings (next 30 days)
      const now = new Date();
      const thirtyDaysLater = new Date(now);
      thirtyDaysLater.setDate(thirtyDaysLater.getDate() + 30);
      
      const timeMin = now.toISOString();
      const timeMax = thirtyDaysLater.toISOString();
      
      // Build query string
      const params = new URLSearchParams({
        time_min: timeMin,
        time_max: timeMax,
        max_results: '50',
      });
      
      // Use apiGet utility instead of raw fetch
      const data = await apiGet<{ meetings: Meeting[] }>(
        `/api/calendar/meetings?${params.toString()}`,
        token
      );
      
      setMeetings(data.meetings || []);
    } catch (err: any) {
      console.error('Error fetching meetings:', err);
      // Handle CalendarAuthError specifically
      if (err.response?.status === 403 || err.message?.includes('not connected')) {
        setError('Google Calendar is not connected. Please connect your Google account in Gmail settings.');
      } else {
        setError(err.message || 'Failed to load meetings');
      }
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchMeetings();
  }, [fetchMeetings]);

  const handleJoinMeeting = (hangoutLink: string) => {
    window.open(hangoutLink, '_blank');
  };

  const formatMeetingTime = (startTime: string, endTime: string) => {
    try {
      const start = parseUTCDate(startTime);
      const end = parseUTCDate(endTime);
      
      const startFormatted = start.toLocaleString(undefined, {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
      });
      
      const endFormatted = end.toLocaleTimeString(undefined, {
        hour: 'numeric',
        minute: '2-digit',
      });
      
      return `${startFormatted} - ${endFormatted}`;
    } catch {
      return `${startTime} - ${endTime}`;
    }
  };

  const getUpcomingMeetings = () => {
    const now = new Date();
    return meetings.filter(meeting => {
      try {
        const start = parseUTCDate(meeting.start_time);
        return start >= now && meeting.status === 'confirmed';
      } catch {
        return false;
      }
    }).sort((a, b) => {
      try {
        const startA = parseUTCDate(a.start_time).getTime();
        const startB = parseUTCDate(b.start_time).getTime();
        return startA - startB;
      } catch {
        return 0;
      }
    });
  };

  const getPastMeetings = () => {
    const now = new Date();
    return meetings.filter(meeting => {
      try {
        const start = parseUTCDate(meeting.start_time);
        return start < now || meeting.status !== 'confirmed';
      } catch {
        return false;
      }
    }).sort((a, b) => {
      try {
        const startA = parseUTCDate(a.start_time).getTime();
        const startB = parseUTCDate(b.start_time).getTime();
        return startB - startA;
      } catch {
        return 0;
      }
    });
  };

  const upcomingMeetings = getUpcomingMeetings();
  const pastMeetings = getPastMeetings();

  if (error && error.includes('not connected')) {
    return (
      <div className="max-w-4xl mx-auto">
        <Card>
          <CardContent className="p-8 text-center">
            <div className="mb-4">
              <svg className="w-16 h-16 text-gray-400 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Google Calendar Not Connected</h2>
            <p className="text-gray-600 mb-4">{error}</p>
            <p className="text-sm text-gray-500">
              Please connect your Google account in the Gmail section to view and manage meetings.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Calendar & Meetings</h1>
          <p className="text-sm text-gray-600 mt-1">View and join your scheduled meetings</p>
        </div>
        <Button onClick={fetchMeetings} disabled={isLoading}>
          {isLoading ? 'Refreshing...' : 'Refresh'}
        </Button>
      </div>

      {error && !error.includes('not connected') && (
        <Card>
          <CardContent className="p-4 bg-red-50 border border-red-200 rounded">
            <p className="text-sm text-red-600">{error}</p>
          </CardContent>
        </Card>
      )}

      {isLoading && meetings.length === 0 ? (
        <Card>
          <CardContent className="p-8 text-center">
            <p className="text-gray-600">Loading meetings...</p>
          </CardContent>
        </Card>
      ) : (
        <>
          {/* Upcoming Meetings */}
          <Card>
            <CardHeader>
              <h2 className="text-lg font-semibold">Upcoming Meetings ({upcomingMeetings.length})</h2>
            </CardHeader>
            <CardContent>
              {upcomingMeetings.length === 0 ? (
                <p className="text-gray-500 text-center py-8">No upcoming meetings</p>
              ) : (
                <div className="space-y-3">
                  {upcomingMeetings.map((meeting) => (
                    <div
                      key={meeting.event_id}
                      className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex-1 min-w-0">
                          <h3 className="font-semibold text-gray-900 mb-1">{meeting.title}</h3>
                          <p className="text-sm text-gray-600 mb-2">
                            {formatMeetingTime(meeting.start_time, meeting.end_time)}
                          </p>
                          {meeting.description && (
                            <p className="text-sm text-gray-600 mb-2 line-clamp-2">{meeting.description}</p>
                          )}
                          {meeting.attendees.length > 0 && (
                            <p className="text-xs text-gray-500">
                              Attendees: {meeting.attendees.join(', ')}
                            </p>
                          )}
                        </div>
                        <div className="flex items-center gap-2 flex-shrink-0">
                          {meeting.hangout_link && (
                            <Button
                              onClick={() => handleJoinMeeting(meeting.hangout_link!)}
                              className="bg-green-600 hover:bg-green-700"
                            >
                              Join Meeting
                            </Button>
                          )}
                          {meeting.html_link && (
                            <Button
                              variant="secondary"
                              onClick={() => window.open(meeting.html_link!, '_blank')}
                            >
                              View in Calendar
                            </Button>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Past Meetings */}
          {pastMeetings.length > 0 && (
            <Card>
              <CardHeader>
                <h2 className="text-lg font-semibold">Past Meetings ({pastMeetings.length})</h2>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {pastMeetings.slice(0, 10).map((meeting) => (
                    <div
                      key={meeting.event_id}
                      className="border border-gray-200 rounded-lg p-4 opacity-60"
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex-1 min-w-0">
                          <h3 className="font-semibold text-gray-900 mb-1">{meeting.title}</h3>
                          <p className="text-sm text-gray-600 mb-2">
                            {formatMeetingTime(meeting.start_time, meeting.end_time)}
                          </p>
                          {meeting.description && (
                            <p className="text-sm text-gray-600 mb-2 line-clamp-2">{meeting.description}</p>
                          )}
                        </div>
                        {meeting.html_link && (
                          <Button
                            variant="secondary"
                            onClick={() => window.open(meeting.html_link!, '_blank')}
                            className="flex-shrink-0"
                          >
                            View in Calendar
                          </Button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
}

