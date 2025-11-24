import { useMemo } from 'react';

import { useAuthStore } from '../../../store/authStore';
import { useStreamVideoClient } from '../../../hooks/useStreamVideoClient';
import { useCallStore } from '../../../store/callStore';

type Props = {
  participantId: string;
  participantName?: string;
  conversationId: string;
};

export default function StreamAudioCallButton({
  participantId,
  participantName,
  conversationId,
}: Props) {
  const { user } = useAuthStore();
  const { client, isLoading } = useStreamVideoClient();
  const startCall = useCallStore((state) => state.startCall);
  const { isCallStarting, activeCall } = useCallStore((state) => ({
    isCallStarting: state.isCallStarting,
    activeCall: state.activeCall,
  }));

  const disabled = useMemo(() => {
    if (isLoading) return true;
    if (!client) return true;
    if (isCallStarting) return true;
    if (activeCall) return true;
    if (!participantId) return true;
    return false;
  }, [client, isLoading, isCallStarting, activeCall, participantId]);

  const handleStartCall = () => {
    if (!client || !user?.id) return;
    startCall({
      videoClient: client,
      callId: conversationId,
      currentUserId: user.id,
      otherUserId: participantId,
      participantName,
    });
  };

  return (
    <button
      type="button"
      onClick={handleStartCall}
      disabled={disabled}
      title={
        disabled
          ? 'Audio call not available right now'
          : `Call ${participantName ?? 'participant'}`
      }
      className="w-9 h-9 rounded-md bg-emerald-500 hover:bg-emerald-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white flex items-center justify-center transition-colors"
    >
      <svg
        className="w-4 h-4"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"
        />
      </svg>
    </button>
  );
}


