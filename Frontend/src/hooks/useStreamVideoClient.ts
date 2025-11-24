import { useEffect, useState } from 'react';
import { StreamVideoClient } from '@stream-io/video-react-sdk';
import type { CallRingEvent, CallEndedEvent } from '@stream-io/video-client';

import { useAuthStore } from '../store/authStore';
import { apiGet } from '../utils/api';
import { STREAM_CHAT_API_KEY } from '../config/streamChat';
import { useCallStore } from '../store/callStore';

type StreamVideoState = {
  client: StreamVideoClient | null;
  isLoading: boolean;
  error: string | null;
};

let cachedClient: StreamVideoClient | null = null;
let cachedUserId: string | null = null;
let clientInitPromise: Promise<StreamVideoClient | null> | null = null;
let watchedCallsForUser: string | null = null;
let watchCallsPromise: Promise<void> | null = null;

export function useStreamVideoClient() {
  const { token, user } = useAuthStore();
  const setIncomingCall = useCallStore((state) => state.setIncomingCall);
  const clearIncomingCall = useCallStore((state) => state.clearIncomingCall);
  const [state, setState] = useState<StreamVideoState>({
    client: cachedClient,
    isLoading: !cachedClient,
    error: null,
  });

  useEffect(() => {
    let mounted = true;

    async function init() {
      if (!token || !user?.id) {
        if (mounted) {
          setState({ client: null, isLoading: false, error: null });
        }
        return;
      }

      if (cachedClient && cachedUserId === user.id) {
        if (mounted) {
          setState({ client: cachedClient, isLoading: false, error: null });
        }
        return;
      }

      if (!clientInitPromise) {
        clientInitPromise = (async () => {
          try {
            const response = await apiGet<{ token: string; user_id: string }>(
              '/api/chat/token',
              token,
            );

            const videoClient = StreamVideoClient.getOrCreateInstance({
              apiKey: STREAM_CHAT_API_KEY,
              user: {
                id: response.user_id,
                name: user.name ?? 'You',
                image: user.picture,
              },
              token: response.token,
              options: {
                rejectCallWhenBusy: true,
              },
            });

            cachedClient = videoClient;
            cachedUserId = response.user_id;
            return videoClient;
          } catch (err: any) {
            console.error('[useStreamVideoClient] init error', err);
            throw err;
          }
        })();
      }

      try {
        const videoClient = await clientInitPromise;
        if (mounted) {
          setState({ client: videoClient, isLoading: false, error: null });
        }
      } catch (err: any) {
        if (mounted) {
          setState({
            client: null,
            isLoading: false,
            error: err?.message ?? 'Failed to initialise Stream Video',
          });
        }
      } finally {
        clientInitPromise = null;
      }
    }

    init();

    return () => {
      mounted = false;
    };
  }, [token, user?.id, user?.name, user?.picture]);

  useEffect(() => {
    if (!state.client || !user?.id) return;

    const handleIncomingRing = (event: CallRingEvent) => {
      console.log('[useStreamVideoClient] Incoming call ring event:', {
        callId: event.call.id,
        createdBy: event.call.created_by.id,
        currentUserId: user.id,
        members: event.members,
      });

      // Don't show incoming call if we created it
      if (event.call.created_by.id === user.id) {
        console.log('[useStreamVideoClient] Ignoring call we created');
        return;
      }

      // Don't show if we're already on a call (check via store)
      const currentActiveCall = useCallStore.getState().activeCall;
      if (currentActiveCall) {
        console.log('[useStreamVideoClient] Already on a call, ignoring incoming');
        return;
      }

      // Set the incoming call - this will trigger the dialog
      console.log('[useStreamVideoClient] Setting incoming call state');
      setIncomingCall({
        callId: event.call.id,
        callType: event.call.type,
        from: {
          id: event.call.created_by.id,
          name: event.call.created_by.name,
        },
        members: event.members,
      });
    };

    const handleCallEnded = (event: CallEndedEvent) => {
      const targetId = event.call?.id;
      if (!targetId) return;
      console.log('[useStreamVideoClient] Call ended event received:', targetId);
      
      // Clear incoming call if it matches
      clearIncomingCall(targetId);
      
      // Also clear active call if it matches (in case the other user hung up)
      const { activeCall } = useCallStore.getState();
      if (activeCall && activeCall.id === targetId) {
        console.log('[useStreamVideoClient] Clearing active call due to call.ended event');
        useCallStore.getState().endActiveCall();
      }
    };

    const handleCallAccepted = (event: any) => {
      console.log('[useStreamVideoClient] Call accepted event:', event.call?.id);
      clearIncomingCall(event.call?.id);
    };

    const offRing = state.client.on('call.ring', handleIncomingRing);
    const offEnded = state.client.on('call.ended', handleCallEnded);
    const offAccepted = state.client.on('call.accepted', handleCallAccepted);

    return () => {
      offRing?.();
      offEnded?.();
      offAccepted?.();
    };
  }, [state.client, user?.id, setIncomingCall, clearIncomingCall]);

  useEffect(() => {
    if (!state.client || !user?.id) return;
    if (watchedCallsForUser === user.id) return;

    let cancelled = false;

    const watchCalls = async () => {
      if (watchCallsPromise) {
        await watchCallsPromise;
        if (cancelled) return;
      }

      watchCallsPromise = (async () => {
        try {
          await state.client.queryCalls({
            filter_conditions: {
              members: { $in: [user.id] },
            },
            watch: true,
            state: true,
            sort: [{ field: 'created_at', direction: -1 }],
            limit: 30,
          });
          if (!cancelled) {
            watchedCallsForUser = user.id;
          }
        } catch (err) {
          console.error('[useStreamVideoClient] Failed to watch calls', err);
          if (!cancelled) {
            setState((prev) => ({
              ...prev,
              error:
                'Unable to connect to Stream calling service. Calls may not ring until the connection recovers.',
            }));
          }
        }
      })();

      try {
        await watchCallsPromise;
      } finally {
        watchCallsPromise = null;
      }
    };

    watchCalls();

    return () => {
      cancelled = true;
      if (watchedCallsForUser === user.id) {
        watchedCallsForUser = null;
      }
    };
  }, [state.client, user?.id]);

  return state;
}


