import { useEffect, useState } from 'react';
import { useAuthStore } from '../../store/authStore';
import { useGmailStore } from '../../store/gmailStore';
import Button from '../ui/Button';
import Card, { CardContent, CardHeader } from '../ui/Card';

export default function GmailPanel() {
  const { token, user } = useAuthStore();
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

  // Only check auth status if user is authenticated (after OAuth callback) or when manually requested
  useEffect(() => {
    if (isAuthenticated && token && !hasCheckedAuth) {
      setHasCheckedAuth(true);
      fetchMessages(token);
    }
  }, [isAuthenticated, token, hasCheckedAuth]);

  const handleAuthenticate = async () => {
    if (!token) return;
    
    try {
      // First, check auth status to get the authorization URL
      setHasCheckedAuth(true);
      await checkAuthStatus(token);
      
      // Wait a bit for state to update
      await new Promise(resolve => setTimeout(resolve, 100));
      
      // Get the updated authorization URL from store
      const currentUrl = useGmailStore.getState().authorizationUrl;
      
      if (currentUrl) {
        // Use redirect-based OAuth for better reliability
        // Store current URL to return after OAuth
        sessionStorage.setItem('gmail_oauth_return', window.location.href);
        window.location.href = currentUrl;
      } else {
        // If no URL, check auth status again (might already be authenticated)
        await checkAuthStatus(token);
        const updatedState = useGmailStore.getState();
        if (updatedState.isAuthenticated) {
          // Already authenticated, fetch messages
          await fetchMessages(token);
        } else {
          console.error('No authorization URL available');
        }
      }
    } catch (error) {
      console.error('Error initiating Gmail authentication:', error);
    }
  };

  // Check for OAuth callback on mount - this handles the redirect after OAuth
  useEffect(() => {
    if (!token) return;

    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    const error = urlParams.get('error');

    if (error) {
      console.error('OAuth error:', error);
      // Clean up URL
      window.history.replaceState({}, document.title, window.location.pathname);
      setHasCheckedAuth(true);
      return;
    }

    if (code) {
      handleAuthCallback(code);
    } else {
      // No code in URL - don't check auth automatically
      // User will click "Connect Gmail" button when they want to connect
      // But if we're already authenticated from a previous session, we should check
      // Only do this once on initial mount
      if (!hasCheckedAuth && token) {
        // Check auth status silently - if authenticated, show messages
        checkAuthStatus(token).then(() => {
          setHasCheckedAuth(true);
          const state = gmailStore.getState();
          if (state.isAuthenticated) {
            fetchMessages(token);
          }
        }).catch(err => {
          // Silently fail - user can click connect button
          setHasCheckedAuth(true);
        });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const handleAuthCallback = async (code: string) => {
    if (!token) return;

    try {
      // Clean up URL first
      window.history.replaceState({}, document.title, window.location.pathname);

      // Exchange code for token using API utility
      const { apiPost } = await import('../../utils/api');
      const response = await apiPost<{ authenticated: boolean; message: string }>(
        '/api/gmail/auth/callback',
        token,
        { authorization_code: code },
        { skipCache: true }
      );

      console.log('Gmail auth callback response:', response);

      // Mark as checked
      setHasCheckedAuth(true);

      // Wait a moment for backend to save the token
      await new Promise(resolve => setTimeout(resolve, 500));

      // Force check auth status - this should now return authenticated: true
      await checkAuthStatus(token);
      
      // Wait for state to update
      await new Promise(resolve => setTimeout(resolve, 200));
      
      // Get the updated state
      const updatedState = gmailStore.getState();
      console.log('Gmail store state after callback:', updatedState);
      
      // If authenticated, fetch messages
      if (updatedState.isAuthenticated && token) {
        await fetchMessages(token);
      } else if (response.authenticated) {
        // If backend says authenticated but store doesn't, force update store
        console.log('Backend says authenticated, forcing store update');
        gmailStore.setState({ 
          isAuthenticated: true, 
          authorizationUrl: null,
          isLoading: false 
        });
        await fetchMessages(token);
      } else {
        // Re-check one more time
        console.log('Re-checking auth status...');
        await checkAuthStatus(token);
        const finalState = gmailStore.getState();
        if (finalState.isAuthenticated) {
          await fetchMessages(token);
        }
      }
    } catch (error) {
      console.error('Error exchanging code for token:', error);
      setHasCheckedAuth(true);
      // Still check auth status to update UI
      await checkAuthStatus(token);
    }
  };

  const handleSendEmail = async () => {
    if (!token || !composeTo || !composeSubject || !composeBody) {
      return;
    }

    try {
      await sendEmail(token, composeTo, composeSubject, composeBody);
      setShowCompose(false);
      setComposeTo('');
      setComposeSubject('');
      setComposeBody('');
    } catch (error) {
      console.error('Error sending email:', error);
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

  if (error) {
    return (
      <div className="h-full w-full flex flex-col p-4">
        <div className="bg-red-50 border border-red-200 rounded p-3 mb-4">
          <p className="text-red-800 text-sm">{error}</p>
          <Button onClick={clearError} className="mt-2" size="sm">
            Dismiss
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full w-full flex flex-col">
      <div className="px-4 py-3 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Gmail</h2>
          {isAuthenticated && (
            <Button onClick={() => setShowCompose(true)} size="sm">
              Compose
            </Button>
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {!isAuthenticated && hasCheckedAuth ? (
          <div className="p-4">
            <Card>
              <CardContent className="p-6 text-center">
                <p className="text-gray-600 mb-4">
                  Connect your Gmail account to view and send emails
                </p>
                <Button 
                  onClick={handleAuthenticate} 
                  disabled={isLoading}
                >
                  {isLoading ? 'Loading...' : 'Connect Gmail'}
                </Button>
              </CardContent>
            </Card>
          </div>
        ) : !isAuthenticated && !hasCheckedAuth ? (
          <div className="p-4">
            <Card>
              <CardContent className="p-6 text-center">
                <p className="text-gray-600 mb-4">
                  Connect your Gmail account to view and send emails
                </p>
                <Button 
                  onClick={handleAuthenticate} 
                  disabled={isLoading || !token}
                >
                  {isLoading ? 'Loading...' : 'Connect Gmail'}
                </Button>
              </CardContent>
            </Card>
          </div>
        ) : (
          <div className="p-2">
            {isLoading && messages.length === 0 ? (
              <div className="text-center text-gray-500 py-8">Loading messages...</div>
            ) : messages.length === 0 ? (
              <div className="text-center text-gray-500 py-8">No messages found</div>
            ) : (
              <div className="space-y-2">
                {messages.map((message) => (
                  <div
                    key={message.id}
                    className="p-3 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <div className="font-medium text-sm text-gray-900 truncate">
                          {message.from}
                        </div>
                        <div className="text-sm font-semibold text-gray-800 mt-1 truncate">
                          {message.subject || '(No Subject)'}
                        </div>
                        <div className="text-xs text-gray-600 mt-1 line-clamp-2">
                          {message.snippet}
                        </div>
                      </div>
                      <div className="text-xs text-gray-400 ml-2 flex-shrink-0">
                        {formatDate(message.date)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {showCompose && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-2xl max-h-[80vh] flex flex-col">
            <CardHeader className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">Compose Email</h3>
              <button
                onClick={() => setShowCompose(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </CardHeader>
            <CardContent className="flex-1 overflow-y-auto">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    To
                  </label>
                  <input
                    type="email"
                    value={composeTo}
                    onChange={(e) => setComposeTo(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    placeholder="recipient@example.com"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Subject
                  </label>
                  <input
                    type="text"
                    value={composeSubject}
                    onChange={(e) => setComposeSubject(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    placeholder="Subject"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Message
                  </label>
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
              <Button
                onClick={() => setShowCompose(false)}
                variant="secondary"
              >
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
    </div>
  );
}

