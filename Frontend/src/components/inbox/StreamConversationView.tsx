import { useState, useEffect, useRef } from 'react';
import { useStreamChat } from '../../hooks/useStreamChat';
import { useInboxStore } from '../../store/inboxStore';
import { useAuthStore } from '../../store/authStore';
import { Channel, useChannelStateContext, useChannelActionContext } from 'stream-chat-react';
import Input from '../ui/Input';
import { VALIDATION_LIMITS } from '../../utils/validation';
import Button from '../ui/Button';
import StreamAudioCallButton from './calls/StreamAudioCallButton';

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
export function CustomMessageList() {
  const channelState = useChannelStateContext() as { messages?: any[]; channel?: any };
  const messages = channelState.messages ?? [];
  const channelId = channelState.channel?.id || channelState.channel?.cid || '';
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const hasScrolledRef = useRef<string | null>(null);

  // Sort messages by created_at to ensure chronological order (oldest first)
  const sortedMessages = [...messages].sort((a: any, b: any) => {
    const timeA = a.created_at ? new Date(a.created_at).getTime() : 0;
    const timeB = b.created_at ? new Date(b.created_at).getTime() : 0;
    return timeA - timeB;
  });

  // Reset scroll flag when channel changes
  useEffect(() => {
    if (channelId && hasScrolledRef.current !== channelId) {
      hasScrolledRef.current = null;
    }
  }, [channelId]);

  // Auto-scroll to bottom only the first time messages load for this channel
  useEffect(() => {
    if (sortedMessages.length > 0 && channelId && hasScrolledRef.current !== channelId && messagesEndRef.current) {
      hasScrolledRef.current = channelId;
      setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    }
  }, [sortedMessages.length, channelId]);

  return (
    <div className="p-4">
      {sortedMessages.map((message: any) => (
        <CustomMessage key={message.id} message={message} />
      ))}
      <div ref={messagesEndRef} />
    </div>
  );
}

// Custom message input
export function CustomMessageInput() {
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

  const remainingChars = VALIDATION_LIMITS.CHAT_MESSAGE - inputValue.length;
  const isNearLimit = remainingChars < 100;

  return (
    <div 
      className="chat-input flex-shrink-0 border-t border-gray-200 bg-white"
      style={{
        padding: '12px 16px',
        display: 'flex',
        flexDirection: 'column',
        gap: '8px'
      }}
    >
      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
        <Input
          placeholder="Type a message..."
          value={inputValue}
          onChange={(e) => {
            const value = e.target.value;
            if (value.length <= VALIDATION_LIMITS.CHAT_MESSAGE) {
              setInputValue(value);
            }
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSend();
            }
          }}
          className="flex-1"
          maxLength={VALIDATION_LIMITS.CHAT_MESSAGE}
        />
        <Button onClick={handleSend} disabled={!inputValue.trim()}>
          Send
        </Button>
      </div>
      {inputValue.length > 0 && (
        <div className="flex justify-end">
          <span 
            className={`text-xs ${
              remainingChars < 0 
                ? 'text-red-600 font-semibold' 
                : isNearLimit 
                ? 'text-yellow-600' 
                : 'text-gray-400'
            }`}
          >
            {inputValue.length}/{VALIDATION_LIMITS.CHAT_MESSAGE} characters
            {remainingChars < 0 && ' (over limit)'}
          </span>
        </div>
      )}
    </div>
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
    const newChannel = client.channel(conversation.channelType!, conversation.channelId!);
    
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


  if (chatLoading || !client) {
    return (
      <div 
        className="chat-container"
        style={{
          height: '420px',
          display: 'flex',
          flexDirection: 'column',
          border: '1px solid #ddd',
          borderRadius: '8px',
          overflow: 'hidden',
          alignItems: 'center',
          justifyContent: 'center'
        }}
      >
        <div className="text-gray-500">Loading chat...</div>
      </div>
    );
  }

  if (!hasConversation || !hasChannelInfo) {
    return (
      <div 
        className="chat-container"
        style={{
          height: '420px',
          display: 'flex',
          flexDirection: 'column',
          border: '1px solid #ddd',
          borderRadius: '8px',
          overflow: 'hidden',
          alignItems: 'center',
          justifyContent: 'center'
        }}
      >
        <div className="text-gray-500 text-center">
          {!hasConversation ? 'Conversation not found' : 'Missing channel info'}
        </div>
      </div>
    );
  }
  
  // Get other participant info
  const otherMember = conversation.participantName || 'Unknown';
  const callTargetId = conversation.channelId || conversation.id || '';

  if (!channel) {
    return (
      <div 
        className="chat-container"
        style={{
          height: '420px',
          display: 'flex',
          flexDirection: 'column',
          border: '1px solid #ddd',
          borderRadius: '8px',
          overflow: 'hidden',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px'
        }}
      >
        <div className="text-gray-500 mb-2">
          {channelError || 'Loading conversation...'}
        </div>
      </div>
    );
  }

  return (
    <div 
      className="flex-1 flex flex-col min-h-0 overflow-hidden"
      style={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: 0,
        overflow: 'hidden'
      }}
    >
      {/* Stream Chat Channel - Messages area (scrollable) */}
      <Channel channel={channel}>
        <div   
          className="flex-1 flex flex-col min-h-0 overflow-hidden"
          style={{
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
            overflow: 'hidden'
          }}
        >
          {/* Messages List - Only this area scrolls */}
          <CustomMessageList />
          {/* Input - Fixed at bottom */}
          <CustomMessageInput />
        </div>
      </Channel>
    </div>
  );
}

