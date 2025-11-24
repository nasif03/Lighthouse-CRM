import { useEffect, useState } from 'react';
import { StreamChat, Channel } from 'stream-chat';
import { useAuthStore } from '../store/authStore';
import { apiGet } from '../utils/api';
import { initializeStreamClient, disconnectStreamClient, getStreamClient } from '../config/streamChat';

type StreamChatState = {
  client: StreamChat | null;
  isLoading: boolean;
  error: string | null;
};

export function useStreamChat() {
  const { token, user } = useAuthStore();
  const [state, setState] = useState<StreamChatState>({
    client: null,
    isLoading: true,
    error: null,
  });

  useEffect(() => {
    let mounted = true;

    async function initializeChat() {
      console.log('[useStreamChat] Initializing chat, token:', !!token, 'user:', !!user, 'user.id:', user?.id);
      
      if (!token || !user?.id) {
        console.log('[useStreamChat] Missing token or user, skipping initialization');
        setState({ client: null, isLoading: false, error: null });
        return;
      }

      try {
        console.log('[useStreamChat] Fetching Stream Chat token from backend...');
        // Get Stream Chat token from backend
        const response = await apiGet<{ token: string; user_id: string }>(
          '/api/chat/token',
          token
        );

        console.log('[useStreamChat] Token received, initializing client...', { 
          hasToken: !!response.token, 
          userId: response.user_id 
        });

        // Initialize Stream Chat client (async)
        const client = await initializeStreamClient(response.token, response.user_id);

        console.log('[useStreamChat] Client initialized:', !!client, 'client.userID:', client.userID);

        if (mounted) {
          setState({ client, isLoading: false, error: null });
          console.log('[useStreamChat] State updated with client');
        }
      } catch (err: any) {
        console.error('[useStreamChat] Failed to initialize Stream Chat:', err);
        console.error('[useStreamChat] Error details:', {
          message: err.message,
          status: err.status,
          response: err.response
        });
        if (mounted) {
          setState({ client: null, isLoading: false, error: err.message || 'Failed to initialize chat' });
        }
      }
    }

    initializeChat();

    return () => {
      mounted = false;
      // Intentionally avoid disconnecting here to prevent race conditions
      // between components that mount/unmount while the user remains logged in.
    };
  }, [token, user?.id]);

  return state;
}

