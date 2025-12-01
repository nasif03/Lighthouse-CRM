import { useInboxStore } from '../store/inboxStore';
import InboxSidebar from '../components/inbox/InboxSidebar';
import StreamConversationView from '../components/inbox/StreamConversationView';
import { Chat } from 'stream-chat-react';
import { useStreamChat } from '../hooks/useStreamChat';
import StreamAudioCallManager from '../components/inbox/calls/StreamAudioCallManager';
import StreamAudioCallButton from '../components/inbox/calls/StreamAudioCallButton';

export default function Inbox() {
  const { activeConversationId, setActiveConversation, conversations } = useInboxStore();
  const { client, isLoading } = useStreamChat();

  const handleBack = () => {
    setActiveConversation(null);
  };

  // Get conversation info for chat header
  const conversation = activeConversationId 
    ? conversations.find((c) => 
        c.id === activeConversationId || 
        c.cid === activeConversationId || 
        c.channelId === activeConversationId
      )
    : null;
  const otherMember = conversation?.participantName || 'Unknown';

  return (
    <>
      <div className="h-full flex gap-4">
        {/* Inbox Sidebar on the left */}
        <div className="w-[300px] flex-shrink-0 h-full">
          <InboxSidebar />
        </div>

        {/* Conversation View on the right taking all remaining space */}
        <div className="flex-1 min-w-0 h-full flex flex-col bg-white border border-gray-200 rounded-lg overflow-hidden">
          {activeConversationId ? (
            <>
              {/* Chat Header - showing who you're chatting with + back button */}
              <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between flex-shrink-0 bg-white">
                <div className="flex items-center gap-3">
                  <button
                    onClick={handleBack}
                    className="w-8 h-8 rounded-md hover:bg-gray-100 flex items-center justify-center text-gray-600 hover:text-gray-900 transition-colors"
                    title="Back to inbox"
                  >
                    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                    </svg>
                  </button>
                  <div className="w-10 h-10 rounded-full bg-brand-600 text-white flex items-center justify-center font-semibold text-sm overflow-hidden flex-shrink-0">
                    {conversation?.participantAvatar && conversation.participantAvatar.startsWith('http') ? (
                      <img 
                        src={conversation.participantAvatar} 
                        alt={conversation.participantName}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <span>{otherMember[0]?.toUpperCase() || '?'}</span>
                    )}
                  </div>
                  <div>
                    <span className="text-sm font-semibold text-gray-900">{otherMember}</span>
                  </div>
                </div>
                {conversation && (
                  <StreamAudioCallButton
                    participantId={conversation.participantId}
                    participantName={conversation.participantName}
                    conversationId={conversation.channelId || conversation.id || ''}
                  />
                )}
              </div>

              {/* Messages Area - Scrollable */}
              <div className="flex-1 min-h-0 overflow-hidden flex flex-col">
                {client ? (
                  <Chat client={client}>
                    <div className="h-full flex flex-col min-h-0">
                      <StreamConversationView />
                    </div>
                  </Chat>
                ) : isLoading ? (
                  <div className="flex-1 flex items-center justify-center h-full">
                    <div className="text-gray-500">Loading chat...</div>
                  </div>
                ) : (
                  <div className="flex-1 flex items-center justify-center h-full p-4">
                    <div className="text-sm text-yellow-600 bg-yellow-50 border border-yellow-200 rounded p-4">
                      Chat client not ready. Please wait a moment and try again.
                    </div>
                  </div>
                )}
              </div>
            </>
          ) : (
            /* Empty state when no conversation is selected */
            <div className="flex-1 flex items-center justify-center h-full">
              <div className="text-center text-gray-500">
                <svg className="w-16 h-16 mx-auto mb-4 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
                <p className="text-lg font-medium text-gray-600">Select a conversation</p>
                <p className="text-sm text-gray-400 mt-1">Choose a conversation from the list to view messages</p>
              </div>
            </div>
          )}
        </div>
      </div>
      <StreamAudioCallManager />
    </>
  );
}

