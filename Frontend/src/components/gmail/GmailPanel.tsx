import { useEffect, useState, useRef } from 'react';
import { useAuthStore } from '../../store/authStore';
import { useGmailStore } from '../../store/gmailStore';
import Button from '../ui/Button';
import Card, { CardContent, CardHeader } from '../ui/Card';
import Input from '../ui/Input';

// Toast notification component
function Toast({ message, type, onClose }: { message: string; type: 'success' | 'error'; onClose: () => void }) {
  useEffect(() => {
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className={`fixed top-4 right-4 z-[100] px-4 py-3 rounded-lg shadow-lg flex items-center gap-3 ${
      type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'
    }`}>
      <span>{type === 'success' ? '✓' : '✕'}</span>
      <span className="text-sm font-medium">{message}</span>
      <button onClick={onClose} className="ml-2 text-white hover:text-gray-200">×</button>
    </div>
  );
}

// Attendee chip component
function AttendeeChip({ email, onRemove }: { email: string; onRemove: () => void }) {
  return (
    <div className="inline-flex items-center gap-2 px-3 py-1 bg-brand-100 text-brand-800 rounded-full text-sm">
      <span>{email}</span>
      <button
        onClick={onRemove}
        className="text-brand-600 hover:text-brand-900 font-semibold"
        aria-label={`Remove ${email}`}
      >
        ×
      </button>
    </div>
  );
}

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
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [meetingTitle, setMeetingTitle] = useState('');
  const [meetingStart, setMeetingStart] = useState('');
  const [meetingEnd, setMeetingEnd] = useState('');
  const [attendeeList, setAttendeeList] = useState<string[]>([]);
  const [attendeeInput, setAttendeeInput] = useState('');
  const [meetingNotes, setMeetingNotes] = useState('');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [isSchedulingMeeting, setIsSchedulingMeeting] = useState(false);
  const [meetingNeedsReconnect, setMeetingNeedsReconnect] = useState(false);
  const [createdMeeting, setCreatedMeeting] = useState<{ title: string; startTime: string; endTime: string; attendees: string[]; link?: string } | null>(null);
  const [recentAttendees, setRecentAttendees] = useState<string[]>([]);
  const [showAttendeeSuggestions, setShowAttendeeSuggestions] = useState(false);
  const [showMeetingMenu, setShowMeetingMenu] = useState(false);
  const attendeeInputRef = useRef<HTMLInputElement>(null);
  const meetingMenuRef = useRef<HTMLDivElement>(null);

  // Load recent attendees from localStorage
  useEffect(() => {
    const stored = localStorage.getItem('recent_attendees');
    if (stored) {
      try {
        setRecentAttendees(JSON.parse(stored));
      } catch (e) {
        console.error('Error loading recent attendees:', e);
      }
    }
  }, []);

  // Close meeting menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (meetingMenuRef.current && !meetingMenuRef.current.contains(event.target as Node)) {
        setShowMeetingMenu(false);
      }
    };
    if (showMeetingMenu) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [showMeetingMenu]);

  // Save attendees to localStorage
  const saveAttendeeToRecent = (email: string) => {
    const emailLower = email.toLowerCase().trim();
    if (!emailLower || !emailLower.includes('@')) return;
    
    setRecentAttendees(prev => {
      const updated = [emailLower, ...prev.filter(e => e !== emailLower)].slice(0, 10);
      localStorage.setItem('recent_attendees', JSON.stringify(updated));
      return updated;
    });
  };

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

  // Helper to format datetime-local input value
  const formatDateTimeLocal = (date: Date): string => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  // Auto-fill default times (next 30 minutes)
  const getDefaultStartTime = (): string => {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 30);
    // Round to nearest 5 minutes
    now.setMinutes(Math.ceil(now.getMinutes() / 5) * 5);
    return formatDateTimeLocal(now);
  };

  const getDefaultEndTime = (startTime: string): string => {
    const start = new Date(startTime);
    start.setMinutes(start.getMinutes() + 30);
    return formatDateTimeLocal(start);
  };

  // Initialize meeting modal with defaults
  const openMeetingModal = (prefillEmail?: string) => {
    const defaultStart = getDefaultStartTime();
    const defaultEnd = getDefaultEndTime(defaultStart);
    
    setMeetingStart(defaultStart);
    setMeetingEnd(defaultEnd);
    setShowMeetingModal(true);
    setShowConfirmation(false);
    setCreatedMeeting(null);
    
    if (prefillEmail) {
      const email = prefillEmail.trim();
      if (email && !attendeeList.includes(email)) {
        setAttendeeList([email]);
        saveAttendeeToRecent(email);
      }
    } else {
      setAttendeeList([]);
    }
  };

  // Handle adding attendee
  const handleAddAttendee = () => {
    const email = attendeeInput.trim().toLowerCase();
    if (!email || !email.includes('@')) {
      setToast({ message: 'Please enter a valid email address', type: 'error' });
      return;
    }
    if (attendeeList.includes(email)) {
      setToast({ message: 'This attendee is already added', type: 'error' });
      setAttendeeInput('');
      return;
    }
    setAttendeeList([...attendeeList, email]);
    saveAttendeeToRecent(email);
    setAttendeeInput('');
    setShowAttendeeSuggestions(false);
  };

  // Handle removing attendee
  const handleRemoveAttendee = (email: string) => {
    setAttendeeList(attendeeList.filter(e => e !== email));
  };

  // Handle attendee input key press
  const handleAttendeeKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      handleAddAttendee();
    }
  };

  // Filter recent attendees for suggestions
  const filteredSuggestions = recentAttendees.filter(
    email => !attendeeList.includes(email) && 
    (!attendeeInput || email.includes(attendeeInput.toLowerCase()))
  );

  // Helper to format error messages
  const handleMeetingError = (err: any): void => {
    const errorMessage = err?.message || err?.detail || 'Failed to create meeting. Please try again.';
    
    // Extract status code from error message if present (e.g., "HTTP 403")
    const statusMatch = errorMessage.match(/HTTP (\d+)/);
    const statusCode = err?.status || err?.response?.status || (statusMatch ? parseInt(statusMatch[1]) : null);
    
    // Check for permission/access errors - show generic message
    const isPermissionError = 
      statusCode === 403 ||
      /403|forbidden|permission|access denied|insufficient|not authorized/i.test(errorMessage) ||
      /Calendar permission|Calendar access|not granted/i.test(errorMessage);
    
    if (isPermissionError) {
      setToast({ message: 'Calendar access is required. Please reconnect your Google account with calendar permissions.', type: 'error' });
      setShowMeetingModal(false);
      return;
    }
    
    // Check for Google Calendar API errors - show generic message
    const isCalendarApiError = 
      statusCode === 400 ||
      /Invalid conference|conference type|Google Calendar error/i.test(errorMessage);
    
    if (isCalendarApiError) {
      setToast({ message: 'Unable to create meeting. Please try again or contact support if the issue persists.', type: 'error' });
      setShowMeetingModal(false);
      return;
    }
    
    // Check for reconnection needed
    const needsReconnect = /Google account is not connected|permission was not granted|invalid|expired/i.test(errorMessage);
    if (needsReconnect) {
      setMeetingNeedsReconnect(true);
      setShowMeetingModal(false);
      useGmailStore.setState({ isAuthenticated: false });
    }
    
    // Show toast for other errors
    setToast({ message: errorMessage, type: 'error' });
  };

  // Quick meeting now
  const handleQuickMeetingNow = async () => {
    if (!token) {
      setToast({ message: 'You must be logged in to schedule a meeting', type: 'error' });
      return;
    }

    const now = new Date();
    const endTime = new Date(now);
    endTime.setMinutes(endTime.getMinutes() + 30);

    try {
      setIsSchedulingMeeting(true);
      const result = await createMeeting({
        title: 'Quick Meeting',
        startTime: now.toISOString(),
        endTime: endTime.toISOString(),
        attendees: [],
        description: 'Quick meeting created from Lighthouse CRM',
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
      });
      
      setCreatedMeeting({
        title: 'Quick Meeting',
        startTime: now.toISOString(),
        endTime: endTime.toISOString(),
        attendees: [],
        link: result.hangout_link || result.html_link,
      });
      setShowMeetingModal(false);
      setShowConfirmation(true);
      setToast({ message: 'Quick meeting created successfully!', type: 'success' });
    } catch (err: any) {
      handleMeetingError(err);
    } finally {
      setIsSchedulingMeeting(false);
    }
  };

  // Handle schedule meeting
  const handleScheduleMeeting = async () => {
    if (!meetingTitle || !meetingStart || !meetingEnd) {
      setToast({ message: 'Title, start time, and end time are required.', type: 'error' });
      return;
    }
    if (!token) {
      setToast({ message: 'You must be logged in to schedule a meeting.', type: 'error' });
      return;
    }

    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

    try {
      setIsSchedulingMeeting(true);
      const result = await createMeeting({
        title: meetingTitle,
        startTime: new Date(meetingStart).toISOString(),
        endTime: new Date(meetingEnd).toISOString(),
        attendees: attendeeList,
        description: meetingNotes,
        timezone,
      });
      
      // Save all attendees to recent
      attendeeList.forEach(saveAttendeeToRecent);
      
      setCreatedMeeting({
        title: meetingTitle,
        startTime: meetingStart,
        endTime: meetingEnd,
        attendees: attendeeList,
        link: result.hangout_link || result.html_link,
      });
      setMeetingNeedsReconnect(false);
      setShowMeetingModal(false);
      setShowConfirmation(true);
      setToast({ message: 'Meeting scheduled successfully!', type: 'success' });
      
      // Reset form
      setMeetingTitle('');
      setMeetingStart('');
      setMeetingEnd('');
      setAttendeeList([]);
      setAttendeeInput('');
      setMeetingNotes('');
    } catch (err: any) {
      handleMeetingError(err);
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
    <div className="h-full w-full flex flex-col min-h-0">
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
      
      <div className="px-4 py-3 border-b border-gray-200 bg-white">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">Gmail</h2>
          {isAuthenticated && (
            <div className="flex items-center gap-2">
              {/* Compose Button - Primary Action */}
              <Button 
                onClick={() => setShowCompose(true)} 
                size="sm"
                className="flex items-center gap-2"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                <span>Compose</span>
              </Button>
              
              {/* Meeting Menu Dropdown */}
              <div className="relative" ref={meetingMenuRef}>
                <Button 
                  onClick={() => setShowMeetingMenu(!showMeetingMenu)} 
                  size="sm" 
                  variant="secondary"
                  className="flex items-center gap-2"
                  disabled={isSchedulingMeeting}
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  <span>Meetings</span>
                  <svg 
                    className={`w-3 h-3 transition-transform ${showMeetingMenu ? 'rotate-180' : ''}`} 
                    fill="none" 
                    stroke="currentColor" 
                    viewBox="0 0 24 24"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </Button>
                
                {/* Dropdown Menu */}
                {showMeetingMenu && (
                  <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
                    <button
                      onClick={() => {
                        setShowMeetingMenu(false);
                        handleQuickMeetingNow();
                      }}
                      disabled={isSchedulingMeeting}
                      className="w-full px-4 py-2.5 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <svg className="w-4 h-4 text-brand-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                      </svg>
                      <div className="flex-1">
                        <div className="font-medium">Quick Meeting Now</div>
                        <div className="text-xs text-gray-500">Start a 30-min meeting instantly</div>
                      </div>
                    </button>
                    <div className="border-t border-gray-100 my-1"></div>
                    <button
                      onClick={() => {
                        setShowMeetingMenu(false);
                        openMeetingModal();
                      }}
                      className="w-full px-4 py-2.5 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-3"
                    >
                      <svg className="w-4 h-4 text-brand-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                      </svg>
                      <div className="flex-1">
                        <div className="font-medium">Schedule Meeting</div>
                        <div className="text-xs text-gray-500">Plan a meeting with details</div>
                      </div>
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto min-h-0">
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
                {messages.map(msg => {
                  const extractEmail = (from?: string): string => {
                    if (!from) return 'unknown@unknown';
                    const match = from.match(/<(.+)>/);
                    if (match) return match[1];
                    const parts = from.split(' ');
                    return parts.pop() || from || 'unknown@unknown';
                  };
                  const formatRelativeTime = (dateString?: string) => {
                    if (!dateString) return '';
                    try {
                      const date = new Date(dateString);
                      const now = new Date();
                      const diff = now.getTime() - date.getTime();
                      const minutes = Math.floor(diff / (1000 * 60));
                      const hours = Math.floor(diff / (1000 * 60 * 60));
                      const days = Math.floor(diff / (1000 * 60 * 60 * 24));

                      if (days >= 3) {
                        return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
                      }
                      if (days >= 1) {
                        return `${days}d ago`;
                      }
                      if (hours >= 1) {
                        return `${hours}h ago`;
                      }
                      if (minutes >= 1) {
                        return `${minutes}m ago`;
                      }
                      return 'Just now';
                    } catch {
                      return dateString || '';
                    }
                  };

                  return (
                    <div key={msg.id} className="p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors">
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex-1 min-w-0">
                          <div className="text-sm font-semibold text-gray-800 truncate">{msg.subject || '(No Subject)'}</div>
                          <div className="text-xs text-gray-600 mt-1 line-clamp-2">{msg.snippet}</div>
                        </div>
                        <div className="text-xs text-gray-400 flex-shrink-0 mt-0.5">{formatRelativeTime(msg.date)}</div>
                      </div>
                      <button
                        onClick={() => openMeetingModal(extractEmail(msg.from))}
                        className="mt-2 flex items-center gap-1.5 text-xs text-brand-600 hover:text-brand-700 font-medium"
                        title="Schedule a meeting with this sender"
                      >
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                        <span>Schedule Meeting</span>
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>

      {meetingNeedsReconnect && (
        <div className="px-4 py-2 text-sm text-red-700 bg-red-50 border border-red-200 flex items-center justify-between">
          <span>Google account needs to be reconnected</span>
          <button 
            className="text-xs uppercase tracking-wide underline"
            onClick={() => { setMeetingNeedsReconnect(false); handleAuthenticate(); }}
          >
            Reconnect Google
          </button>
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

      {/* Meeting Modal */}
      {showMeetingModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-2xl max-h-[85vh] flex flex-col">
            <CardHeader className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">Schedule Google Meet</h3>
              <button
                onClick={() => {
                  setShowMeetingModal(false);
                  setMeetingTitle('');
                  setAttendeeList([]);
                  setAttendeeInput('');
                  setMeetingNotes('');
                }}
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
                  <Input
                    type="text"
                    value={meetingTitle}
                    onChange={(e) => setMeetingTitle(e.target.value)}
                    placeholder="e.g. Customer onboarding call"
                  />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Start time</label>
                    <Input
                      type="datetime-local"
                      value={meetingStart}
                      onChange={(e) => {
                        setMeetingStart(e.target.value);
                        // Auto-update end time if it's before start time
                        if (e.target.value && new Date(e.target.value) >= new Date(meetingEnd)) {
                          setMeetingEnd(getDefaultEndTime(e.target.value));
                        }
                      }}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">End time</label>
                    <Input
                      type="datetime-local"
                      value={meetingEnd}
                      onChange={(e) => setMeetingEnd(e.target.value)}
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Attendees</label>
                  <div className="space-y-2">
                    <div className="flex gap-2">
                      <div className="flex-1 relative">
                        <Input
                          ref={attendeeInputRef}
                          type="email"
                          value={attendeeInput}
                          onChange={(e) => {
                            setAttendeeInput(e.target.value);
                            setShowAttendeeSuggestions(e.target.value.length > 0 && filteredSuggestions.length > 0);
                          }}
                          onKeyPress={handleAttendeeKeyPress}
                          onFocus={() => {
                            if (filteredSuggestions.length > 0) setShowAttendeeSuggestions(true);
                          }}
                          onBlur={() => {
                            // Delay to allow click on suggestion
                            setTimeout(() => setShowAttendeeSuggestions(false), 200);
                          }}
                          placeholder="Enter email address"
                        />
                        {showAttendeeSuggestions && filteredSuggestions.length > 0 && (
                          <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-40 overflow-y-auto">
                            {filteredSuggestions.map((email) => (
                              <button
                                key={email}
                                type="button"
                                className="w-full text-left px-3 py-2 hover:bg-gray-100 text-sm"
                                onClick={() => {
                                  setAttendeeInput(email);
                                  handleAddAttendee();
                                }}
                              >
                                {email}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                      <Button onClick={handleAddAttendee} size="sm" variant="secondary">
                        Add
                      </Button>
                    </div>
                    {attendeeList.length > 0 && (
                      <div className="flex flex-wrap gap-2 p-2 bg-gray-50 rounded-md min-h-[40px]">
                        {attendeeList.map((email) => (
                          <AttendeeChip key={email} email={email} onRemove={() => handleRemoveAttendee(email)} />
                        ))}
                      </div>
                    )}
                  </div>
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
              <Button 
                onClick={() => {
                  setShowMeetingModal(false);
                  setMeetingTitle('');
                  setAttendeeList([]);
                  setAttendeeInput('');
                  setMeetingNotes('');
                }} 
                variant="secondary"
                disabled={isSchedulingMeeting}
              >
                Cancel
              </Button>
              <Button 
                onClick={handleScheduleMeeting} 
                disabled={isSchedulingMeeting || !meetingTitle || !meetingStart || !meetingEnd}
              >
                {isSchedulingMeeting ? (
                  <span className="flex items-center gap-2">
                    <svg className="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Scheduling…
                  </span>
                ) : (
                  'Create meeting'
                )}
              </Button>
            </div>
          </Card>
        </div>
      )}

      {/* Confirmation View */}
      {showConfirmation && createdMeeting && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-2xl flex flex-col">
            <CardHeader className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-green-700">✓ Meeting Scheduled Successfully</h3>
              <button
                onClick={() => {
                  setShowConfirmation(false);
                  setCreatedMeeting(null);
                }}
                className="text-gray-400 hover:text-gray-600"
                aria-label="Close confirmation"
              >
                ✕
              </button>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-3">
                <div>
                  <label className="text-sm font-medium text-gray-600">Title</label>
                  <p className="text-base font-semibold text-gray-900">{createdMeeting.title}</p>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-gray-600">Start Time</label>
                    <p className="text-base text-gray-900">
                      {new Date(createdMeeting.startTime).toLocaleString()}
                    </p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-600">End Time</label>
                    <p className="text-base text-gray-900">
                      {new Date(createdMeeting.endTime).toLocaleString()}
                    </p>
                  </div>
                </div>
                {createdMeeting.attendees.length > 0 && (
                  <div>
                    <label className="text-sm font-medium text-gray-600 mb-2 block">Attendees</label>
                    <div className="flex flex-wrap gap-2">
                      {createdMeeting.attendees.map((email) => (
                        <span key={email} className="px-3 py-1 bg-brand-100 text-brand-800 rounded-full text-sm">
                          {email}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
                {createdMeeting.link && (
                  <div>
                    <label className="text-sm font-medium text-gray-600 mb-2 block">Google Meet Link</label>
                    <div className="flex items-center gap-2">
                      <a
                        href={createdMeeting.link}
                        target="_blank"
                        rel="noreferrer"
                        className="flex-1 px-3 py-2 bg-brand-600 text-white rounded-md hover:bg-brand-700 text-center font-medium"
                      >
                        Join Meeting
                      </a>
                      <button
                        onClick={() => {
                          navigator.clipboard.writeText(createdMeeting.link!);
                          setToast({ message: 'Link copied to clipboard!', type: 'success' });
                        }}
                        className="px-3 py-2 border border-gray-300 rounded-md hover:bg-gray-50 text-sm"
                      >
                        Copy Link
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
            <div className="px-6 py-4 border-t border-gray-200 flex justify-end">
              <Button 
                onClick={() => {
                  setShowConfirmation(false);
                  setCreatedMeeting(null);
                }}
              >
                Done
              </Button>
            </div>
          </Card>
        </div>
      )}

    </div>
  );
}
