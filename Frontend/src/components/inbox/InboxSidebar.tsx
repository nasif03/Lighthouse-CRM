import { useEffect, useState } from 'react';
import { useInboxStore, Conversation } from '../../store/inboxStore';
import { useTenantStore } from '../../store/tenantStore';
import { useAuthStore } from '../../store/authStore';
import { clsx } from 'clsx';
import NewConversationModal from './NewConversationModal';

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

  const handleClick = () => {
    console.log('Conversation clicked:', {
      id: conversation.id,
      cid: conversation.cid,
      channelId: conversation.channelId,
      channelType: conversation.channelType,
      fullConversation: conversation
    });
    // Try using cid if id doesn't work
    const conversationId = conversation.id || conversation.cid || conversation.channelId;
    console.log('Setting active conversation to:', conversationId);
    setActiveConversation(conversation.cid || null);
  };

  return (
    <button
      onClick={handleClick}
      className={clsx(
        'w-full px-3 py-2.5 rounded-md text-left transition-colors',
        isActive ? 'bg-brand-50 border-l-2 border-brand-600' : 'hover:bg-gray-50'
      )}
    >
      <div className="flex items-start gap-3">
        <div className="w-10 h-10 rounded-full bg-brand-600 text-white flex items-center justify-center font-semibold text-sm flex-shrink-0 overflow-hidden">
          {conversation.participantAvatar ? (
            <img 
              src={conversation.participantAvatar} 
              alt={conversation.participantName}
              className="w-full h-full object-cover"
            />
          ) : (
            <span>{conversation.participantName?.[0]?.toUpperCase() || '?'}</span>
          )}
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
  const { conversations, fetchConversations, isLoading } = useInboxStore();
  const { activeTenantId, tenants } = useTenantStore();  // Add tenants to destructuring
  const { token } = useAuthStore();
  const [showNewConversationModal, setShowNewConversationModal] = useState(false);

  // Get the active tenant name instead of showing the ID
  const activeTenant = tenants.find(t => t.id === activeTenantId);
  const activeTenantName = activeTenant?.name || 'Unknown Organization';  // Fallback

  useEffect(() => {
    if (token) {
      fetchConversations(token);
    }
  }, [token, fetchConversations]);

  return (
    <>
      <div className="w-full flex flex-col">
        {/* New Conversation Button */}
        <div className="px-4 py-3 flex-shrink-0">
          <div className="flex items-center justify-between gap-2 mb-2">
            <span className="text-xs text-gray-500">{activeTenantName}</span>
          </div>
          <button
            onClick={() => setShowNewConversationModal(true)}
            className="w-full px-3 py-2 bg-brand-600 hover:bg-brand-700 text-white rounded-md text-sm font-medium transition-colors flex items-center justify-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            New Conversation
          </button>
        </div>
        {/* Conversations List */}
        <div className="flex-1 overflow-y-auto px-2 pb-2 space-y-1 min-h-0" style={{ maxHeight: '400px' }}>
          {isLoading ? (
            <div className="text-center text-gray-500 py-4 text-sm">Loading conversations...</div>
          ) : conversations.length === 0 ? (
            <div className="text-center text-gray-500 py-4 text-sm">No conversations yet</div>
          ) : (
            conversations.map((conv) => (
              <ConversationItem key={conv.id} conversation={conv} />
            ))
          )}
        </div>
      </div>
      
      <NewConversationModal
        isOpen={showNewConversationModal}
        onClose={() => setShowNewConversationModal(false)}
      />
    </>
  );
}

