import { useEffect, useRef, useState } from 'react';
import { clsx } from 'clsx';

import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import { useAuthStore } from '../store/authStore';
import { apiPost, apiGet } from '../utils/api';

type SupportChatMessage = {
  id: string;
  role: 'user' | 'assistant';
  content: string;
};

export default function SupportAI() {
  const { token } = useAuthStore();

  const [messages, setMessages] = useState<SupportChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [isLoadingHistory, setIsLoadingHistory] = useState(true);

  const chatEndRef = useRef<HTMLDivElement | null>(null);

  // Load conversation history on mount
  useEffect(() => {
    if (!token) return;
    
    const loadHistory = async () => {
      try {
        const response = await apiGet<{ conversationId: string; messages: SupportChatMessage[] }>(
          '/api/support-chat/history',
          token
        );
        
        if (response.messages && response.messages.length > 0) {
          setMessages(response.messages);
          setConversationId(response.conversationId);
        } else {
          // No history - show welcome message
          setMessages([
            {
              id: 'assistant-welcome',
              role: 'assistant',
              content:
                "Hi! I'm Support AI. Ask anything about Jira/JSM integration, CRM workflows, or Lighthouse MCP setup and I will guide you.",
            },
          ]);
        }
      } catch (err: any) {
        // If history load fails, show welcome message
        setMessages([
          {
            id: 'assistant-welcome',
            role: 'assistant',
            content:
              "Hi! I'm Support AI. Ask anything about Jira/JSM integration, CRM workflows, or Lighthouse MCP setup and I will guide you.",
          },
        ]);
      } finally {
        setIsLoadingHistory(false);
      }
    };
    
    loadHistory();
  }, [token]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async () => {
    if (!token || !input.trim() || isSending) {
      return;
    }

    const messageText = input.trim();
    const userMessage: SupportChatMessage = {
      id: `user-${Date.now()}-${Math.random()}`,
      role: 'user',
      content: messageText,
    };

    const nextMessages = [...messages, userMessage];
    setMessages(nextMessages);
    setInput('');
    setIsSending(true);
    setError(null);

    try {
      // Backend loads history from DB, so we don't need to send it
      const response = await apiPost<{ reply: string; conversationId: string }>(
        '/api/support-chat',
        token,
        {
          message: messageText,
          conversationId,
        },
      );

      setConversationId(response.conversationId);
      
      // Add assistant reply to messages
      setMessages((prev) => [
        ...prev,
        {
          id: `assistant-${Date.now()}-${Math.random()}`,
          role: 'assistant',
          content: response.reply,
        },
      ]);
    } catch (err: any) {
      const fallback = err?.message || 'Unable to reach Support AI right now. Please try again.';
      setError(fallback);
      setMessages((prev) => [
        ...prev,
        {
          id: `assistant-error-${Date.now()}-${Math.random()}`,
          role: 'assistant',
          content: 'Sorry, I could not respond at the moment. Please try again shortly.',
        },
      ]);
    } finally {
      setIsSending(false);
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  };

  const canSend = input.trim().length > 0 && !isSending;

  return (
    <div className="h-full flex flex-col">
      <Card className="flex-1 flex flex-col min-h-0">
        <CardHeader>
          <div>
            <h1 className="text-lg font-semibold">Support AI</h1>
            <p className="text-sm text-gray-500 mt-1">
              Get instant insights about Lighthouse CRM, Jira/JSM workflows, and MCP tools.
            </p>
          </div>
        </CardHeader>
        <CardContent className="flex flex-col flex-1 min-h-0">
          {error && (
            <div className="mb-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
              {error}
            </div>
          )}
          <div className="flex-1 overflow-y-auto space-y-3 pr-1 min-h-0">
            {isLoadingHistory ? (
              <div className="text-center text-gray-500 py-8 text-sm">Loading conversation...</div>
            ) : (
              messages.map((message) => (
              <div
                key={message.id}
                className={clsx('flex', message.role === 'user' ? 'justify-end' : 'justify-start')}
              >
                <div
                  className={clsx(
                    'rounded-lg px-3 py-2 text-sm shadow-sm max-w-[70%] whitespace-pre-wrap break-words',
                    message.role === 'user'
                      ? 'bg-brand-600 text-white'
                      : 'bg-gray-100 text-gray-800',
                  )}
                >
                  {message.content}
                </div>
              </div>
              ))
            )}
            <div ref={chatEndRef} />
          </div>
          {isSending && (
            <div className="mt-2 text-center text-xs text-gray-500">
              Support AI is thinking...
            </div>
          )}
          <div className="mt-4 space-y-2">
            <div className="space-y-1">
              <textarea
                value={input}
                onChange={(e) => {
                  const value = e.target.value;
                  if (value.length <= 2000) {
                    setInput(value);
                  }
                }}
                onKeyDown={handleKeyDown}
                rows={4}
                placeholder="Type your question and press Enter to send"
                className="w-full rounded-md border border-gray-200 p-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-200 disabled:bg-gray-50"
                disabled={isSending}
                maxLength={2000}
              />
              <div className="text-xs text-gray-400 text-right">
                {input.length}/2000 characters
              </div>
            </div>
            <Button onClick={sendMessage} disabled={!canSend} className="w-full">
              {isSending ? 'Sending...' : 'Send'}
            </Button>
            <p className="text-xs text-gray-500">
              Support AI can help with CRM configuration, Jira/JSM ticket flows, MCP commands, and troubleshooting
              steps. Anything sensitive or account-specific will be escalated to human support.
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}


