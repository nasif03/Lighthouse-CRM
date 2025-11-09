import { create } from 'zustand';
import { apiGet, apiPost } from '../utils/api';

type GmailMessage = {
  id: string;
  threadId: string;
  subject: string;
  from: string;
  date: string;
  snippet: string;
  body: string;
  labels: string[];
};

type GmailState = {
  messages: GmailMessage[];
  isLoading: boolean;
  isAuthenticated: boolean;
  authorizationUrl: string | null;
  error: string | null;
  fetchMessages: (token: string) => Promise<void>;
  checkAuthStatus: (token: string) => Promise<void>;
  sendEmail: (token: string, to: string, subject: string, body: string) => Promise<void>;
  clearError: () => void;
};

export const useGmailStore = create<GmailState>((set, get) => ({
  messages: [],
  isLoading: false,
  isAuthenticated: false,
  authorizationUrl: null,
  error: null,

  checkAuthStatus: async (token: string) => {
    try {
      set({ isLoading: true, error: null });
      
      // Don't use cache key - GET requests are idempotent and can run concurrently
      // The API client now handles GET requests without cancelling each other
      const response = await apiGet<{
        authenticated: boolean;
        authorization_url?: string;
        message: string;
      }>('/api/gmail/auth/status', token, {
        skipCache: true
      });

      // Only update if we got a valid response
      if (response) {
        set({
          isAuthenticated: response.authenticated === true,
          authorizationUrl: response.authorization_url || null,
          isLoading: false,
        });
        
        // If authenticated, automatically fetch messages
        if (response.authenticated) {
          // Don't await to avoid blocking
          get().fetchMessages(token).catch(err => {
            // Only log non-cancellation errors
            if (!err.message?.includes('Request cancelled')) {
              console.error('Error fetching messages after auth check:', err);
            }
          });
        }
      }
    } catch (error: any) {
      // Don't log cancellation errors as they're expected during rapid requests
      if (!error.message?.includes('Request cancelled')) {
        console.error('Error checking Gmail auth status:', error);
      }
      
      // Only set error if it's a real error, not cancellation or unauthenticated
      if (error.message && 
          !error.message.includes('not authenticated') && 
          !error.message.includes('Request cancelled')) {
        set({
          error: error.message || 'Failed to check Gmail authentication status',
          isLoading: false,
        });
      } else {
        set({
          isAuthenticated: false,
          isLoading: false,
        });
      }
    }
  },

  fetchMessages: async (token: string) => {
    try {
      set({ isLoading: true, error: null });
      const response = await apiGet<{
        messages: GmailMessage[];
        total: number;
      }>('/api/gmail/messages?max_results=10', token);

      set({
        messages: response.messages,
        isLoading: false,
      });
    } catch (error: any) {
      console.error('Error fetching Gmail messages:', error);
      set({
        error: error.message || 'Failed to fetch Gmail messages',
        isLoading: false,
      });
    }
  },

  sendEmail: async (token: string, to: string, subject: string, body: string) => {
    try {
      set({ isLoading: true, error: null });
      await apiPost<{ id: string; threadId: string; success: boolean }>(
        '/api/gmail/send',
        token,
        { to, subject, body },
        { skipCache: true }
      );

      // Refresh messages after sending
      await get().fetchMessages(token);
      set({ isLoading: false });
    } catch (error: any) {
      console.error('Error sending email:', error);
      set({
        error: error.message || 'Failed to send email',
        isLoading: false,
      });
      throw error;
    }
  },

  clearError: () => {
    set({ error: null });
  },
}));

