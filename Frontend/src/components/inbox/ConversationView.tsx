import { useState, useRef, useEffect } from 'react';
import { useInboxStore } from '../../store/inboxStore';
import Button from '../ui/Button';
import Input from '../ui/Input';

function CallButton({ participantName }: { participantName: string }) {
  const handleCall = () => {
    // VoIP integration will go here
    console.log(`Calling ${participantName}...`);
    alert(`Call feature will integrate with open-source VoIP. Calling ${participantName}...`);
  };

  return (
    <button
      onClick={handleCall}
      className="w-9 h-9 rounded-md bg-green-500 hover:bg-green-600 text-white flex items-center justify-center transition-colors"
      title="Call"
    >
      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
      </svg>
    </button>
  );
}

function MessageBubble({ message, isOwn }: { message: any; isOwn: boolean }) {
  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'} mb-3`}>
      <div
        className={`max-w-[70%] rounded-lg px-3 py-2 ${
          isOwn
            ? 'bg-brand-600 text-white'
            : 'bg-gray-100 text-gray-900'
        }`}
      >
        <p className="text-sm">{message.content}</p>
        <p className={`text-xs mt-1 ${isOwn ? 'text-brand-100' : 'text-gray-500'}`}>
          {new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </p>
      </div>
    </div>
  );
}

export default function ConversationView() {
  const { conversations, activeConversationId, addMessage, markAsRead, setActiveConversation } = useInboxStore();
  const [inputValue, setInputValue] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const lastMarkedRef = useRef<string | null>(null);

  // Get the active conversation
  const conversation = conversations.find((c) => c.id === activeConversationId);

  // Mark as read only when activeConversationId changes (not when conversation object changes)
  useEffect(() => {
    if (activeConversationId && activeConversationId !== lastMarkedRef.current) {
      lastMarkedRef.current = activeConversationId;
      markAsRead(activeConversationId);
      // Scroll after a brief delay to ensure DOM is updated
      setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    }
  }, [activeConversationId, markAsRead]);

  // Scroll to bottom when messages change
  useEffect(() => {
    if (conversation?.messages.length) {
      setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    }
  }, [conversation?.messages.length]);

  const handleSend = () => {
    if (!inputValue.trim() || !conversation) return;

    const newMessage = {
      id: `msg-${Date.now()}`,
      senderId: 'me',
      senderName: 'You',
      content: inputValue,
      timestamp: new Date(),
      read: true,
    };

    addMessage(conversation.id, newMessage);
    setInputValue('');
    setTimeout(() => {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, 100);
  };

  // This should never happen now, but keep as safety check
  if (!conversation) {
    return null;
  }

  const handleBack = () => {
    setActiveConversation(null);
  };

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div className="flex items-center gap-3">
          {/* Back Button */}
          <button
            onClick={handleBack}
            className="w-8 h-8 rounded-md hover:bg-gray-100 flex items-center justify-center text-gray-600 hover:text-gray-900 transition-colors"
            title="Back to inbox"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          
          {/* Avatar */}
          <div className="w-10 h-10 rounded-full bg-brand-600 text-white flex items-center justify-center font-semibold">
            {conversation.participantAvatar || conversation.participantName[0]}
          </div>
          
          {/* Name and Status */}
          <div>
            <h3 className="font-semibold text-gray-900">{conversation.participantName}</h3>
            <p className="text-xs text-gray-500">Online</p>
          </div>
        </div>
        <CallButton participantName={conversation.participantName} />
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4">
        {conversation.messages.map((message) => (
          <MessageBubble
            key={message.id}
            message={message}
            isOwn={message.senderId === 'me'}
          />
        ))}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
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
    </div>
  );
}

