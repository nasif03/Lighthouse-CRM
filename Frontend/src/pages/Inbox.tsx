import { useState, useEffect } from 'react';
import { useInboxStore } from '../store/inboxStore';
import InboxSidebar from '../components/inbox/InboxSidebar';
import { CustomMessageList, CustomMessageInput } from '../components/inbox/StreamConversationView';
import { Chat, Channel } from 'stream-chat-react';
import { useStreamChat } from '../hooks/useStreamChat';
import StreamAudioCallManager from '../components/inbox/calls/StreamAudioCallManager';
import StreamAudioCallButton from '../components/inbox/calls/StreamAudioCallButton';

export default function Inbox() {
  const { activeConversationId, setActiveConversation, conversations } = useInboxStore();
  const { client, isLoading } = useStreamChat();
  const [channel, setChannel] = useState<any>(null);
  const [channelError, setChannelError] = useState<string | null>(null);

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

  const hasChannelInfo = !!(conversation?.channelType && conversation?.channelId);

  // Initialize and watch the channel
  useEffect(() => {
    if (!client || !hasChannelInfo || !conversation) {
      setChannel(null);
      return;
    }

    setChannelError(null);
    const newChannel = client.channel(conversation.channelType!, conversation.channelId!);
    
    const timeout = setTimeout(() => {
      setChannelError('Channel loading timeout. Please try again.');
    }, 10000);

    newChannel.watch()
      .then(() => {
        clearTimeout(timeout);
        setChannel(newChannel);
      })
      .catch((error: any) => {
        clearTimeout(timeout);
        console.error('Error watching channel:', error);
        setChannelError(error.message || 'Failed to load channel');
      });

    return () => {
      clearTimeout(timeout);
      try {
        newChannel.stopWatching();
      } catch (e) {
        // Ignore errors
      }
    };
  }, [client, hasChannelInfo, conversation?.channelType, conversation?.channelId]);

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
              {/* PART 1: Header - Fixed at top */}
              <div className="flex-shrink-0 px-4 py-3 border-b border-gray-200 flex items-center justify-between bg-white">
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

              {/* PART 2: Messages Area - Scrollable, separate container */}
              <div className="flex-1 overflow-y-auto min-h-0">
                {client && channel ? (
                  <Chat client={client}>
                    <Channel channel={channel}>
                      <CustomMessageList />
                    </Channel>
                  </Chat>
                ) : isLoading || !client ? (
                  <div className="h-full flex items-center justify-center">
                    <div className="text-gray-500">Loading chat...</div>
                  </div>
                ) : channelError ? (
                  <div className="h-full flex items-center justify-center p-4">
                    <div className="text-sm text-yellow-600 bg-yellow-50 border border-yellow-200 rounded p-4">
                      {channelError}
                    </div>
                  </div>
                ) : (
                  <div className="h-full flex items-center justify-center p-4">
                    <div className="text-sm text-yellow-600 bg-yellow-50 border border-yellow-200 rounded p-4">
                      Chat client not ready. Please wait a moment and try again.
                    </div>
                  </div>
                )}
              </div>

              {/* PART 3: Input Area - Fixed at bottom, separate container */}
              <div className="flex-shrink-0 border-t border-gray-200 bg-white">
                {client && channel ? (
                  <Chat client={client}>
                    <Channel channel={channel}>
                      <CustomMessageInput />
                    </Channel>
                  </Chat>
                ) : null}
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

