import { useEffect, useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { useGmailStore } from '../../store/gmailStore';
import Button from '../ui/Button';
import Card, { CardContent, CardHeader } from '../ui/Card';

export default function GmailPanel() {
  const { token, createMeeting } = useAuthStore();
  const gmailStore = useGmailStore();
  const {
    messages,
    isLoading,
    isAuthenticated,
    authorizationUrl,
    error,
    fetchMessages,
    checkAuthStatus,
    sendEmail,
    clearError,
  } = gmailStore;

  const [showCompose, setShowCompose] = useState(false);
  const [composeTo, setComposeTo] = useState('');
  const [composeSubject, setComposeSubject] = useState('');
  const [composeBody, setComposeBody] = useState('');
  const [hasCheckedAuth, setHasCheckedAuth] = useState(false);
  const [showMeetingModal, setShowMeetingModal] = useState(false);
  const [meetingTitle, setMeetingTitle] = useState('');
  const [meetingStart, setMeetingStart] = useState('');
  const [meetingEnd, setMeetingEnd] = useState('');
  const [meetingAttendees, setMeetingAttendees] = useState('');
  const [meetingNotes, setMeetingNotes] = useState('');
  const [meetingStatus, setMeetingStatus] = useState<{ type: 'success' | 'error'; message: string; link?: string } | null>(null);
  const [isSchedulingMeeting, setIsSchedulingMeeting] = useState(false);
  const [meetingNeedsReconnect, setMeetingNeedsReconnect] = useState(false);

  // Helper to show Gmail connect button if not authenticated
  const needsGmailReconnect = !isAuthenticated && !!token;

  useEffect(() => {
    console.log('AuthStore token:', useAuthStore.getState().token);
    console.log('GmailStore isAuthenticated:', useGmailStore.getState().isAuthenticated);
  }, []);

  // Automatically fetch messages if authenticated
  useEffect(() => {
    if (isAuthenticated && token && !hasCheckedAuth) {
      setHasCheckedAuth(true);
      fetchMessages(token);
    }
  }, [isAuthenticated, token, hasCheckedAuth]);

  const handleAuthenticate = async () => {
    if (!token) return;

    try {
      setHasCheckedAuth(true);
      await checkAuthStatus(token);

      const currentUrl = useGmailStore.getState().authorizationUrl;
      if (currentUrl) {
        sessionStorage.setItem('gmail_oauth_return', window.location.href);
        window.location.href = currentUrl;
      } else {
        // Already authenticated?
        await checkAuthStatus(token);
        const updatedState = useGmailStore.getState();
        if (updatedState.isAuthenticated) {
          await fetchMessages(token);
        } else {
          console.error('No authorization URL available');
        }
      }
    } catch (err) {
      console.error('Error initiating Gmail authentication:', err);
    }
  };

  // Handle OAuth callback after redirect
  useEffect(() => {
    if (!token) return;

    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    const errorParam = urlParams.get('error');

    if (errorParam) {
      console.error('OAuth error:', errorParam);
      window.history.replaceState({}, document.title, window.location.pathname);
      setHasCheckedAuth(true);
      return;
    }

    if (code) {
      handleAuthCallback(code);
    } else if (!hasCheckedAuth && token) {
      // Silent auth check
      checkAuthStatus(token)
        .then(() => {
          setHasCheckedAuth(true);
          const state = useGmailStore.getState();
          if (state.isAuthenticated) fetchMessages(token);
        })
        .catch(() => setHasCheckedAuth(true));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const handleAuthCallback = async (code: string) => {
    if (!token) return;

    try {
      window.history.replaceState({}, document.title, window.location.pathname);
      const { apiPost } = await import('../../utils/api');
      const response = await apiPost<{ authenticated: boolean; message: string }>(
        '/api/gmail/auth/callback',
        token,
        { authorization_code: code },
        { skipCache: true }
      );

      setHasCheckedAuth(true);
      await new Promise(r => setTimeout(r, 500));
      await checkAuthStatus(token);
      await new Promise(r => setTimeout(r, 200));

      const updatedState = useGmailStore.getState();

      if (updatedState.isAuthenticated && token) {
        await fetchMessages(token);
      } else if (response.authenticated) {
        useGmailStore.setState({ isAuthenticated: true, authorizationUrl: null, isLoading: false });
        await fetchMessages(token);
      } else {
        await checkAuthStatus(token);
        const finalState = useGmailStore.getState();
        if (finalState.isAuthenticated) await fetchMessages(token);
      }
    } catch (err) {
      console.error('Error exchanging code for token:', err);
      setHasCheckedAuth(true);
      await checkAuthStatus(token);
    }
  };

  const handleSendEmail = async () => {
    if (!token || !composeTo || !composeSubject || !composeBody) return;

    try {
      await sendEmail(token, composeTo, composeSubject, composeBody);
      setShowCompose(false);
      setComposeTo('');
      setComposeSubject('');
      setComposeBody('');
    } catch (err) {
      console.error('Error sending email:', err);
    }
  };

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return dateString;
    }
  };

  const handleScheduleMeeting = async () => {
    if (!meetingTitle || !meetingStart || !meetingEnd) {
      setMeetingStatus({ type: 'error', message: 'Title, start time, and end time are required.' });
      return;
    }
    if (!token) {
      setMeetingStatus({ type: 'error', message: 'You must be logged in to schedule a meeting.' });
      return;
    }

    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    const attendees = meetingAttendees.split(',').map(e => e.trim()).filter(Boolean);

    try {
      setIsSchedulingMeeting(true);
      const result = await createMeeting({
        title: meetingTitle,
        startTime: new Date(meetingStart).toISOString(),
        endTime: new Date(meetingEnd).toISOString(),
        attendees,
        description: meetingNotes,
        timezone,
      });
      setMeetingStatus({
        type: 'success',
        message: 'Meeting scheduled successfully.',
        link: result.hangout_link || result.html_link,
      });
      setMeetingNeedsReconnect(false);
      setShowMeetingModal(false);
      setMeetingTitle('');
      setMeetingStart('');
      setMeetingEnd('');
      setMeetingAttendees('');
      setMeetingNotes('');
    } catch (err: any) {
      const detail = err?.message || 'Failed to schedule meeting. Please try again.';
      const needsReconnect = /Google account is not connected|permission was not granted/i.test(detail);
      setMeetingNeedsReconnect(needsReconnect);
      setMeetingStatus({ type: 'error', message: detail });
      if (needsReconnect) {
        setShowMeetingModal(false);
        useGmailStore.setState({ isAuthenticated: false }); // force reconnect
      }
    } finally {
      setIsSchedulingMeeting(false);
    }
  };

  if (error) {
    return (
      <div className="h-full w-full flex flex-col p-4">
        <div className="bg-red-50 border border-red-200 rounded p-3 mb-4">
          <p className="text-red-800 text-sm">{error}</p>
          <Button onClick={clearError} className="mt-2" size="sm">Dismiss</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full w-full flex flex-col">
      <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <h2 className="text-lg font-semibold">Gmail</h2>
        {isAuthenticated && (
          <div className="flex gap-2">
            <Button onClick={() => setShowCompose(true)} size="sm">Compose</Button>
            <Button onClick={() => { setShowMeetingModal(true); setMeetingStatus(null); }} size="sm" variant="secondary">Schedule meeting</Button>
          </div>
        )}
      </div>

      <div className="flex-1 overflow-y-auto">
        {needsGmailReconnect && (
          <div className="p-4">
            <Card>
              <CardContent className="p-6 text-center">
                <p className="text-gray-600 mb-4">Connect your Gmail account to view and send emails</p>
                <Button onClick={handleAuthenticate} disabled={isLoading}>
                  {isLoading ? 'Loading...' : 'Connect Gmail'}
                </Button>
              </CardContent>
            </Card>
          </div>
        )}

        {!needsGmailReconnect && (
          <div className="p-2">
            {isLoading && messages.length === 0 ? (
              <div className="text-center text-gray-500 py-8">Loading messages...</div>
            ) : messages.length === 0 ? (
              <div className="text-center text-gray-500 py-8">No messages found</div>
            ) : (
              <div className="space-y-2">
                {messages.map(msg => (
                  <div key={msg.id} className="p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <div className="font-medium text-sm text-gray-900 truncate">{msg.from}</div>
                        <div className="text-sm font-semibold text-gray-800 mt-1 truncate">{msg.subject || '(No Subject)'}</div>
                        <div className="text-xs text-gray-600 mt-1 line-clamp-2">{msg.snippet}</div>
                      </div>
                      <div className="text-xs text-gray-400 ml-2 flex-shrink-0">{formatDate(msg.date)}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {meetingStatus && (
        <div className={`px-4 py-2 text-sm ${meetingStatus.type === 'success' ? 'text-green-700 bg-green-50' : 'text-red-700 bg-red-50'} border ${meetingStatus.type === 'success' ? 'border-green-200' : 'border-red-200'} flex items-center justify-between`}>
          <span>
            {meetingStatus.message}{' '}
            {meetingStatus.link && (
              <a href={meetingStatus.link} target="_blank" rel="noreferrer" className="underline font-medium">Join link</a>
            )}
            {meetingNeedsReconnect && (
              <button className="ml-3 text-xs uppercase tracking-wide underline"
                onClick={() => { setMeetingStatus(null); handleAuthenticate(); }}>
                Reconnect Google
              </button>
            )}
          </span>
          <button className="text-xs uppercase tracking-wide" onClick={() => setMeetingStatus(null)}>Dismiss</button>
        </div>
      )}

      {showCompose && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-2xl max-h-[80vh] flex flex-col">
            <CardHeader className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">Compose Email</h3>
              <button
                onClick={() => setShowCompose(false)}
                className="text-gray-400 hover:text-gray-600"
                aria-label="Close compose modal"
              >
                ✕
              </button>
            </CardHeader>
            <CardContent className="flex-1 overflow-y-auto">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">To</label>
                  <input
                    type="email"
                    value={composeTo}
                    onChange={(e) => setComposeTo(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    placeholder="recipient@example.com"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Subject</label>
                  <input
                    type="text"
                    value={composeSubject}
                    onChange={(e) => setComposeSubject(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    placeholder="Subject"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Message</label>
                  <textarea
                    value={composeBody}
                    onChange={(e) => setComposeBody(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    rows={10}
                    placeholder="Your message..."
                  />
                </div>
              </div>
            </CardContent>
            <div className="px-6 py-4 border-t border-gray-200 flex justify-end gap-2">
              <Button onClick={() => setShowCompose(false)} variant="secondary">
                Cancel
              </Button>
              <Button
                onClick={handleSendEmail}
                disabled={!composeTo || !composeSubject || !composeBody || isLoading}
              >
                {isLoading ? 'Sending...' : 'Send'}
              </Button>
            </div>
          </Card>
        </div>
      )}

      {showMeetingModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-2xl max-h-[85vh] flex flex-col">
            <CardHeader className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">Schedule Google Meet</h3>
              <button
                onClick={() => setShowMeetingModal(false)}
                className="text-gray-400 hover:text-gray-600"
                aria-label="Close meeting modal"
              >
                ✕
              </button>
            </CardHeader>
            <CardContent className="flex-1 overflow-y-auto">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
                  <input
                    type="text"
                    value={meetingTitle}
                    onChange={(e) => setMeetingTitle(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    placeholder="e.g. Customer onboarding call"
                  />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Start time</label>
                    <input
                      type="datetime-local"
                      value={meetingStart}
                      onChange={(e) => setMeetingStart(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">End time</label>
                    <input
                      type="datetime-local"
                      value={meetingEnd}
                      onChange={(e) => setMeetingEnd(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Attendees (comma separated)</label>
                  <input
                    type="text"
                    value={meetingAttendees}
                    onChange={(e) => setMeetingAttendees(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    placeholder="email1@example.com, email2@example.com"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Agenda / notes (optional)</label>
                  <textarea
                    value={meetingNotes}
                    onChange={(e) => setMeetingNotes(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    rows={4}
                    placeholder="Share context or agenda that will be added to the calendar event"
                  />
                </div>
              </div>
            </CardContent>
            <div className="px-6 py-4 border-t border-gray-200 flex justify-end gap-2">
              <Button onClick={() => setShowMeetingModal(false)} variant="secondary">
                Cancel
              </Button>
              <Button onClick={handleScheduleMeeting} disabled={isSchedulingMeeting}>
                {isSchedulingMeeting ? 'Scheduling…' : 'Create meeting'}
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
