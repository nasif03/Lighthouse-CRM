import { useInboxStore, Conversation } from '../../store/inboxStore';
import { useTenantStore } from '../../store/tenantStore';
import { useAuthStore } from '../../store/authStore';
import { useTwilioStore } from '../../store/twilioStore';
import { clsx } from 'clsx';
import { useState } from 'react';

function formatTime(date: Date): string {
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days < 7) return `${days}d ago`;
  return date.toLocaleDateString();
}

function ConversationItem({ conversation }: { conversation: Conversation }) {
  const { activeConversationId, setActiveConversation } = useInboxStore();
  const isActive = activeConversationId === conversation.id;

  return (
    <button
      onClick={() => setActiveConversation(conversation.id)}
      className={clsx(
        'w-full px-3 py-2.5 rounded-md text-left transition-colors',
        isActive ? 'bg-brand-50 border-l-2 border-brand-600' : 'hover:bg-gray-50'
      )}
    >
      <div className="flex items-start gap-3">
        <div className="w-10 h-10 rounded-full bg-brand-600 text-white flex items-center justify-center font-semibold text-sm flex-shrink-0">
          {conversation.participantAvatar || conversation.participantName[0]}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between gap-2 mb-0.5">
            <span className="text-sm font-semibold text-gray-900 truncate">
              {conversation.participantName}
            </span>
            <span className="text-xs text-gray-500 flex-shrink-0">
              {formatTime(conversation.lastMessageTime)}
            </span>
          </div>
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs text-gray-600 truncate">{conversation.lastMessage}</p>
            {conversation.unreadCount > 0 && (
              <span className="bg-brand-600 text-white text-xs font-semibold px-1.5 py-0.5 rounded-full flex-shrink-0">
                {conversation.unreadCount}
              </span>
            )}
          </div>
        </div>
      </div>
    </button>
  );
}

export default function InboxSidebar() {
  const { conversations } = useInboxStore();
  const { activeTenantId } = useTenantStore();
  const { token } = useAuthStore();
  const { makeCall, isLoading } = useTwilioStore();
  const [callStatus, setCallStatus] = useState<string | null>(null);

  const handleTopCall = async () => {
    if (!token) {
      alert('Please login to make calls');
      return;
    }

    // Always call the hardcoded allowed number
    const allowedNumber = '+8801957128594';
    
    try {
      setCallStatus('Calling...');
      await makeCall(token, allowedNumber, 'Hello, this is a call from Lighthouse CRM.');
      setCallStatus('Call initiated');
      setTimeout(() => setCallStatus(null), 3000);
    } catch (err: any) {
      console.error('Call error:', err);
      alert(`Failed to make call: ${err.message || 'Unknown error'}`);
      setCallStatus(null);
    }
  };

  return (
    <div className="h-full w-full flex flex-col">
      <div className="px-4 py-3 border-b border-gray-200">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-lg font-semibold">Inbox</h2>
          <div className="flex items-center gap-2">
            <span className="text-xs text-gray-500">Tenant: {activeTenantId}</span>
            <button
              onClick={handleTopCall}
              disabled={isLoading || !token}
              className="w-8 h-8 rounded-md bg-green-500 hover:bg-green-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white flex items-center justify-center transition-colors"
              title="Call +8801957128594"
            >
              {isLoading ? (
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              ) : (
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
              )}
            </button>
            {callStatus && (
              <span className="text-xs text-gray-600">{callStatus}</span>
            )}
          </div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto p-2 space-y-1">
        {conversations.map((conv) => (
          <ConversationItem key={conv.id} conversation={conv} />
        ))}
      </div>
    </div>
  );
}

