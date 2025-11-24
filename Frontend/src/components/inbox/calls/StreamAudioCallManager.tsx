import { useEffect, useMemo, memo } from 'react';
import {
  StreamVideo,
  StreamCall,
  StreamTheme,
  SpeakerLayout,
} from '@stream-io/video-react-sdk';

import '@stream-io/video-react-sdk/dist/css/styles.css';

import { useStreamVideoClient } from '../../../hooks/useStreamVideoClient';
import { useCallStore } from '../../../store/callStore';

export default function StreamAudioCallManager() {
  const { client } = useStreamVideoClient();
  const {
    activeCall,
    activeCallMeta,
    incomingCall,
    acceptIncomingCall,
    declineIncomingCall,
    endActiveCall,
    callError,
    setCallError,
  } = useCallStore();

  useEffect(() => {
    if (callError) {
      console.error('[StreamAudioCallManager]', callError);
    }
  }, [callError]);

  useEffect(() => {
    if (incomingCall) {
      console.log('[StreamAudioCallManager] Incoming call detected:', incomingCall);
    }
  }, [incomingCall]);

  if (!client) return null;

  return (
    <StreamVideo client={client}>
      {incomingCall && (
        <IncomingCallDialog
          callerName={incomingCall.from?.name || 'Someone'}
          onAccept={() => acceptIncomingCall(client)}
          onDecline={() => declineIncomingCall(client)}
        />
      )}
      {activeCall && (
        <StreamCall call={activeCall}>
          <StreamTheme>
            <ActiveAudioCall
              participantName={
                activeCallMeta?.participantName || 'Call participant'
              }
              onHangup={async () => {
                await endActiveCall();
                setCallError(null);
              }}
            />
          </StreamTheme>
        </StreamCall>
      )}
      {callError && (
        <div className="fixed bottom-4 left-1/2 -translate-x-1/2 z-50 bg-white shadow-lg border border-red-200 rounded-md px-4 py-3 flex items-start gap-3 max-w-md">
          <div className="text-red-600 font-medium">Audio Call</div>
          <p className="text-sm text-gray-700 flex-1">{callError}</p>
          <button
            type="button"
            onClick={() => setCallError(null)}
            className="text-xs text-gray-500 hover:text-gray-800"
          >
            Dismiss
          </button>
        </div>
      )}
    </StreamVideo>
  );
}

type IncomingCallProps = {
  callerName: string;
  onAccept: () => void;
  onDecline: () => void;
};

function IncomingCallDialog({
  callerName,
  onAccept,
  onDecline,
}: IncomingCallProps) {
  return (
    <div className="fixed inset-0 z-30 flex items-end md:items-center justify-center pointer-events-none">
      <div className="bg-white shadow-xl rounded-lg p-4 w-full max-w-sm mx-4 mb-6 pointer-events-auto border border-gray-200">
        <p className="text-sm text-gray-500">Incoming audio call</p>
        <h3 className="text-lg font-semibold text-gray-900 mt-1">{callerName}</h3>
        <div className="mt-4 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onDecline}
            className="px-4 py-2 rounded-md bg-red-500 hover:bg-red-600 text-white text-sm font-medium transition-colors"
          >
            Decline
          </button>
          <button
            type="button"
            onClick={onAccept}
            className="px-4 py-2 rounded-md bg-emerald-500 hover:bg-emerald-600 text-white text-sm font-medium transition-colors"
          >
            Accept
          </button>
        </div>
      </div>
    </div>
  );
}

type ActiveCallProps = {
  participantName: string;
  onHangup: () => void | Promise<void>;
};

// Memoized wrapper for SpeakerLayout to prevent getSnapshot warning
// Note: This warning is from Stream SDK's internal implementation and may persist
const MemoizedSpeakerLayout = memo(() => <SpeakerLayout />, () => true); // Always return true to prevent re-renders
MemoizedSpeakerLayout.displayName = 'MemoizedSpeakerLayout';

function ActiveAudioCall({ participantName, onHangup }: ActiveCallProps) {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-40">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl mx-4 overflow-hidden">
        <div className="bg-gradient-to-br from-emerald-500 to-blue-600 text-white px-6 py-4">
          <p className="text-sm uppercase tracking-wider opacity-80">
            Audio Call
          </p>
          <h2 className="text-2xl font-semibold mt-1">{participantName}</h2>
        </div>
        <div className="p-6">
          <MemoizedSpeakerLayout />
          <div className="mt-4 flex justify-center">
            <button
              type="button"
              onClick={onHangup}
              className="px-6 py-3 bg-red-500 hover:bg-red-600 text-white rounded-lg font-medium transition-colors flex items-center gap-2"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
              End Call
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}


