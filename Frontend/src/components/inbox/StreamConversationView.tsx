import { useState, useEffect, useRef } from 'react';
import { useStreamChat } from '../../hooks/useStreamChat';
import { useInboxStore } from '../../store/inboxStore';
import { useAuthStore } from '../../store/authStore';
import { Channel, MessageInput, Window, useChannelStateContext, useChannelActionContext } from 'stream-chat-react';
import { useJitsiStore } from '../../store/jitsiStore';
import JitsiCall from '../jitsi/JitsiCall';
import Input from '../ui/Input';
import Button from '../ui/Button';

// Custom message component to match CRM design
function CustomMessage({ message }: { message: any }) {
  const { user } = useAuthStore();
  const isOwn = message.user?.id === user?.id;

  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} mb-3`}>
      <div
        className={`max-w-[70%] rounded-lg px-3 py-2 ${
          isOwn
            ? 'bg-brand-600 text-white'
            : 'bg-gray-100 text-gray-900'
        }`}
      >
        <p className="text-sm">{message.text}</p>
        <p className={`text-xs mt-1 ${isOwn ? 'text-brand-100' : 'text-gray-500'}`}>
          {new Date(message.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </p>
      </div>
    </div>
  );
}

// Custom message list
function CustomMessageList() {
  const { messages } = useChannelStateContext();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className="flex-1 overflow-y-auto p-4">
      {messages.map((message: any) => (
        <CustomMessage key={message.id} message={message} />
      ))}
      <div ref={messagesEndRef} />
    </div>
  );
}

// Custom message input
function CustomMessageInput() {
  const [inputValue, setInputValue] = useState('');
  const { sendMessage } = useChannelActionContext();

  const handleSend = async () => {
    if (!inputValue.trim()) return;

    try {
      await sendMessage({
        text: inputValue.trim(),
      });
      setInputValue('');
    } catch (error) {
      console.error('Failed to send message:', error);
    }
  };

  return (
    <div className="px-4 py-3 border-t border-gray-200">
      <div className="flex items-center gap-2">
        <Input
          placeholder="Type a message..."
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSend();
            }
          }}
          className="flex-1"
        />
        <Button onClick={handleSend} disabled={!inputValue.trim()}>
          Send
        </Button>
      </div>
    </div>
  );
}

function JitsiCallButton({ 
  participantId, 
  participantName 
}: { 
  participantId: string; 
  participantName: string;
}) {
  const { token, user } = useAuthStore();
  const { startCall, endCall, isCallActive, currentRoom } = useJitsiStore();
  const [showCall, setShowCall] = useState(false);

  const handleStartCall = () => {
    if (!token || !user?.id) {
      alert('Please login to make calls');
      return;
    }

    const roomName = startCall(user.id, participantId);
    setShowCall(true);
  };

  const handleEndCall = () => {
    endCall();
    setShowCall(false);
  };

  return (
    <>
      <button
        onClick={handleStartCall}
        disabled={isCallActive || !token}
        className="w-9 h-9 rounded-md bg-green-500 hover:bg-green-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white flex items-center justify-center transition-colors"
        title={`Call ${participantName}`}
      >
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
        </svg>
      </button>
      
      {showCall && currentRoom && (
        <JitsiCall
          roomName={currentRoom}
          userName={user?.name || 'You'}
          onEndCall={handleEndCall}
        />
      )}
    </>
  );
}

export default function StreamConversationView() {
  // ALL HOOKS MUST BE CALLED AT THE TOP - BEFORE ANY CONDITIONAL RETURNS
  const { client, isLoading: chatLoading } = useStreamChat();
  const { activeConversationId, setActiveConversation } = useInboxStore();
  const { conversations } = useInboxStore();
  const [channel, setChannel] = useState<any>(null);
  const [channelError, setChannelError] = useState<string | null>(null);
  
  // Try multiple ways to find the conversation
  const conversation = conversations.find((c) => 
    c.id === activeConversationId || 
    c.cid === activeConversationId || 
    c.channelId === activeConversationId
  );

  console.log('[StreamConversationView]', {
    activeConversationId,
    conversationsCount: conversations.length,
    foundConversation: !!conversation,
    conversation: conversation ? {
      id: conversation.id,
      cid: conversation.cid,
      channelId: conversation.channelId,
      channelType: conversation.channelType
    } : null,
    client: !!client,
    chatLoading
  });

  const hasConversation = !!conversation;
  const hasChannelInfo = !!(conversation?.channelType && conversation?.channelId);

  // Initialize and watch the channel (must stay before any early returns)
  useEffect(() => {
    if (!client || !hasChannelInfo || !conversation) return;

    console.log('[StreamConversationView] Initializing channel:', {
      type: conversation.channelType,
      id: conversation.channelId
    });

    setChannelError(null);
    const newChannel = client.channel(conversation.channelType, conversation.channelId);
    
    // Set a timeout for channel watch
    const timeout = setTimeout(() => {
      setChannelError('Channel loading timeout. Please try again.');
    }, 10000); // 10 second timeout

    newChannel.watch()
      .then(() => {
        clearTimeout(timeout);
        console.log('[StreamConversationView] Channel watched successfully');
        setChannel(newChannel);
      })
      .catch((error: any) => {
        clearTimeout(timeout);
        console.error('[StreamConversationView] Error watching channel:', error);
        setChannelError(error.message || 'Failed to load channel');
      });

    return () => {
      clearTimeout(timeout);
      try {
        newChannel.stopWatching();
      } catch (e) {
        // Ignore errors when stopping watch
      }
    };
  }, [client, hasChannelInfo, conversation?.channelType, conversation?.channelId]);

  const handleBack = () => {
    setActiveConversation(null);
  };

  if (chatLoading || !client) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-gray-500">Loading chat...</div>
      </div>
    );
  }

  if (!hasConversation || !hasChannelInfo) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="text-gray-500">
          {!hasConversation ? 'Conversation not found' : 'Missing channel info'}
          <br />
          <button 
            onClick={handleBack}
            className="mt-2 text-sm text-brand-600 hover:underline"
          >
            Back to inbox
          </button>
        </div>
      </div>
    );
  }
  
  // Get other participant info
  const otherMember = conversation.participantName || 'Unknown';
  const otherMemberAvatar = conversation.participantAvatar || otherMember[0];

  if (!channel) {
    return (
      <div className="h-full flex flex-col items-center justify-center p-4">
        <div className="text-gray-500 mb-2">
          {channelError || 'Loading conversation...'}
        </div>
        {channelError && (
          <button 
            onClick={handleBack}
            className="mt-2 px-4 py-2 bg-brand-600 text-white rounded-md hover:bg-brand-700 text-sm"
          >
            Back to inbox
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
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
          
          <div className="w-10 h-10 rounded-full bg-brand-600 text-white flex items-center justify-center font-semibold overflow-hidden">
            {conversation.participantAvatar && conversation.participantAvatar.startsWith('http') ? (
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
            <h3 className="font-semibold text-gray-900">{otherMember}</h3>
            <p className="text-xs text-gray-500">Online</p>
          </div>
        </div>
        <JitsiCallButton 
          participantId={conversation.participantId}
          participantName={conversation.participantName}
        />
      </div>

      {/* Stream Chat Channel */}
      <Channel channel={channel}>
        <Window>
          <CustomMessageList />
          <CustomMessageInput />
        </Window>
      </Channel>
    </div>
  );
}

