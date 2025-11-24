import { create } from 'zustand';
import { CallingState } from '@stream-io/video-client';
import type { Call, StreamVideoClient } from '@stream-io/video-client';

type IncomingCall = {
  callId: string;
  callType: string;
  from?: {
    id: string;
    name?: string | null;
  };
  members?: Array<{ user_id: string; role?: string }>;
};

type ActiveCallMeta = {
  participantName?: string;
  participantId?: string;
};

type StartCallArgs = {
  videoClient: StreamVideoClient | null;
  callId: string;
  currentUserId: string;
  otherUserId: string;
  participantName?: string;
};

type CallStore = {
  activeCall: Call | null;
  activeCallMeta: ActiveCallMeta | null;
  activeCallCleanup: Array<() => void>;
  incomingCall: IncomingCall | null;
  isCallStarting: boolean;
  callError: string | null;
  setIncomingCall: (call: IncomingCall | null) => void;
  clearIncomingCall: (callId?: string) => void;
  startCall: (args: StartCallArgs) => Promise<void>;
  acceptIncomingCall: (videoClient: StreamVideoClient | null) => Promise<void>;
  declineIncomingCall: (
    videoClient: StreamVideoClient | null,
    reason?: string,
  ) => Promise<void>;
  endActiveCall: () => Promise<void>;
  setCallError: (message: string | null) => void;
};

const registerCallEndedCleanup = (
  call: Call,
  cleanup: Array<() => void>,
  set: (updater: any) => void,
  get: () => CallStore,
) => {
  const offEnded = call.on?.('call.ended', () => {
    const callId = call.id;
    console.log('[useCallStore] Call ended event received for active call:', callId);
    
    // Execute all cleanup functions
    get().activeCallCleanup.forEach((fn) => fn());
    
    // Clear all call-related state
    set({
      activeCall: null,
      activeCallMeta: null,
      activeCallCleanup: [],
      incomingCall: get().incomingCall?.callId === callId ? null : get().incomingCall,
      callError: null, // Clear any errors too
    });
  });
  
  // Also listen for participant left events
  const offParticipantLeft = call.on?.('participant.left', (event: any) => {
    console.log('[useCallStore] Participant left event:', event);
    // If the other participant left, end the call for us too
    const { activeCall } = get();
    if (activeCall && activeCall.id === call.id) {
      console.log('[useCallStore] Other participant left, ending call');
      get().activeCallCleanup.forEach((fn) => fn());
      set({
        activeCall: null,
        activeCallMeta: null,
        activeCallCleanup: [],
        callError: null,
      });
    }
  });
  
  return offEnded && offParticipantLeft 
    ? [...cleanup, offEnded, offParticipantLeft] 
    : offEnded 
    ? [...cleanup, offEnded] 
    : cleanup;
};

const joinAudioOnly = async (
  call: Call,
  options: { create?: boolean },
): Promise<{ mutedFallback: boolean }> => {
  try {
    await call.camera?.disable();
  } catch (err) {
    console.warn('[useCallStore] Unable to disable camera before join', err);
  }

  try {
    await call.join({ create: options.create, audio: true, video: false });
    return { mutedFallback: false };
  } catch (error: any) {
    if (error?.name === 'NotFoundError') {
      await call.join({ create: options.create, audio: false, video: false });
      return { mutedFallback: true };
    }
    throw error;
  }
};

export const useCallStore = create<CallStore>((set, get) => ({
  activeCall: null,
  activeCallMeta: null,
  activeCallCleanup: [],
  incomingCall: null,
  isCallStarting: false,
  callError: null,
  setIncomingCall: (call) => {
    console.log('[useCallStore] Setting incoming call:', call);
    // Always set the incoming call, even if there was a previous one
    // This ensures subsequent calls show the dialog
    set({ incomingCall: call, callError: null });
  },
  clearIncomingCall: (callId) => {
    console.log('[useCallStore] clearIncomingCall called:', callId);
    set((state) => {
      if (!state.incomingCall) {
        console.log('[useCallStore] No incoming call to clear');
        return state;
      }
      if (!callId || state.incomingCall.callId === callId) {
        console.log('[useCallStore] Clearing incoming call:', state.incomingCall.callId);
        return { incomingCall: null };
      }
      console.log('[useCallStore] Incoming call ID mismatch, not clearing');
      return state;
    });
  },
  setCallError: (message) => set({ callError: message }),
  startCall: async ({
    videoClient,
    callId,
    currentUserId,
    otherUserId,
    participantName,
  }) => {
    if (!videoClient) {
      set({ callError: 'Chat client not ready yet.' });
      return;
    }
    if (!currentUserId || !otherUserId) {
      set({ callError: 'Missing participant information for call.' });
      return;
    }
    const { activeCall, incomingCall } = get();
    if (activeCall) {
      set({ callError: 'You are already on a call.' });
      return;
    }

    // Clear any incoming call when starting a new call
    if (incomingCall) {
      set({ incomingCall: null });
    }

    try {
      set({ isCallStarting: true, callError: null });
      console.log('[useCallStore] Starting call:', { callId, currentUserId, otherUserId });
      const call = videoClient.call('default', callId);

      await call.getOrCreate({
        ring: true,
        data: {
          members: [
            { user_id: currentUserId },
            { user_id: otherUserId },
          ],
        },
      });

      const { mutedFallback } = await joinAudioOnly(call, { create: true });
      if (mutedFallback) {
        set({
          callError:
            'Microphone not detected. You joined the call muted. Please connect a microphone to speak.',
        });
      }

      // When the SFU ends or remote hangs up, close modal automatically
      const cleanup = registerCallEndedCleanup(call, [], set, get);

      set({
        activeCall: call,
        activeCallMeta: {
          participantName,
          participantId: otherUserId,
        },
        activeCallCleanup: cleanup,
        isCallStarting: false,
        incomingCall: null,
      });
    } catch (error: any) {
      console.error('[useCallStore] startCall failed', error);
      set({
        callError: error?.message || 'Failed to start audio call',
        isCallStarting: false,
      });
    }
  },
  acceptIncomingCall: async (videoClient) => {
    const { incomingCall, activeCall } = get();
    if (!incomingCall) {
      console.warn('[useCallStore] acceptIncomingCall: No incoming call');
      return;
    }
    if (!videoClient) {
      set({ callError: 'Chat client not ready yet.' });
      return;
    }
    if (activeCall) {
      set({ callError: 'You are already on a call.' });
      return;
    }

    // Save call info before clearing
    const callId = incomingCall.callId;
    const callType = incomingCall.callType || 'default';
    const callerName = incomingCall.from?.name;
    const otherMemberId = incomingCall.members?.find(
      (m) => m.user_id !== videoClient.state.connectedUser?.id,
    )?.user_id;

    // Clear incoming call immediately to prevent double-accept
    set({ incomingCall: null });

    try {
      console.log('[useCallStore] Accepting call:', { callId, callType });
      const call = videoClient.call(callType, callId);

      // Check call state first
      try {
        const callState = await call.get();
        console.log('[useCallStore] Call state:', callState?.state?.callingState);
        
        // If call is already ended, don't try to accept
        if (callState?.state?.callingState === 'ended' || callState?.state?.callingState === 'left') {
          throw new Error('Call has already ended');
        }
      } catch (getError: any) {
        // If get() fails, the call might not exist or be invalid
        console.warn('[useCallStore] Could not get call state:', getError);
        // Continue anyway - might still be able to join
      }

      // First, accept the call (this responds to the ring)
      try {
        await call.accept();
        console.log('[useCallStore] Call accepted successfully');
      } catch (acceptError: any) {
        // If accept fails with 400, the call might already be accepted or ended
        if (acceptError?.response?.status === 400 || acceptError?.message?.includes('400')) {
          console.warn('[useCallStore] Call accept returned 400 (might already be accepted/ended)');
          // Try to join anyway - the call might still be active
        } else {
          console.warn('[useCallStore] Call accept failed:', acceptError);
        }
      }

      // Then join the call
      const { mutedFallback } = await joinAudioOnly(call, { create: false });
      if (mutedFallback) {
        set({
          callError:
            'Microphone not detected. You joined the call muted. Please connect a microphone to speak.',
        });
      }

      const cleanup = registerCallEndedCleanup(call, [], set, get);

      set({
        activeCall: call,
        activeCallMeta: {
          participantName: callerName,
          participantId: otherMemberId,
        },
        activeCallCleanup: cleanup,
        callError: null,
      });
    } catch (error: any) {
      console.error('[useCallStore] acceptIncomingCall failed', error);
      // If accept fails, the call might have already ended or been accepted
      const errorMessage = error?.message || 'Failed to join audio call';
      const isCallEnded = errorMessage.includes('already ended') || errorMessage.includes('400');
      
      set({
        callError: isCallEnded 
          ? 'The call has already ended or was cancelled.' 
          : errorMessage,
      });
    }
  },
  declineIncomingCall: async (videoClient, reason = 'rejected') => {
    const { incomingCall } = get();
    if (!incomingCall) return;
    if (videoClient) {
      try {
        const call = videoClient.call(
          incomingCall.callType || 'default',
          incomingCall.callId,
        );
        await call.reject(reason);
      } catch (error) {
        console.warn('[useCallStore] decline call failed', error);
      }
    }
    set({ incomingCall: null });
  },
  endActiveCall: async () => {
    const { activeCall, activeCallCleanup } = get();
    if (!activeCall) return;
    const callId = activeCall.id;
    try {
      if (activeCall.state.callingState !== CallingState.LEFT) {
        console.log('[useCallStore] Leaving call:', callId);
        await activeCall.leave({ message: 'ended-by-user' });
      } else {
        console.log('[useCallStore] Call already left:', callId);
      }
    } catch (error: any) {
      // Ignore "already left" errors
      if (!error?.message?.includes('already been left')) {
        console.error('[useCallStore] endActiveCall failed', error);
        set({ callError: error?.message || 'Failed to end call' });
      }
    } finally {
      activeCallCleanup.forEach((fn) => fn());
      // Clear active call state but DON'T clear incoming call
      // Incoming call should only be cleared when it's accepted, declined, or the call ends
      console.log('[useCallStore] Clearing active call state');
      set({
        activeCall: null,
        activeCallMeta: null,
        activeCallCleanup: [],
        callError: null,
        // Don't clear incomingCall here - it should be managed separately
      });
    }
  },
}));


