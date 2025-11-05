import { create } from 'zustand';

export type Message = {
  id: string;
  senderId: string;
  senderName: string;
  content: string;
  timestamp: Date;
  read: boolean;
};

export type Conversation = {
  id: string;
  participantId: string;
  participantName: string;
  participantAvatar?: string;
  lastMessage: string;
  lastMessageTime: Date;
  unreadCount: number;
  messages: Message[];
};

type InboxState = {
  conversations: Conversation[];
  activeConversationId: string | null;
  setActiveConversation: (id: string | null) => void;
  addMessage: (conversationId: string, message: Message) => void;
  markAsRead: (conversationId: string) => void;
};

// Mock data with Alex and others
const mockConversations: Conversation[] = [
  {
    id: 'conv-1',
    participantId: 'alex',
    participantName: 'Alex Thompson',
    participantAvatar: 'A',
    lastMessage: 'Thanks for the update!',
    lastMessageTime: new Date(Date.now() - 5 * 60000),
    unreadCount: 2,
    messages: [
      {
        id: 'msg-1',
        senderId: 'alex',
        senderName: 'Alex Thompson',
        content: 'Hey, can we schedule a call to discuss the campaign?',
        timestamp: new Date(Date.now() - 30 * 60000),
        read: true,
      },
      {
        id: 'msg-2',
        senderId: 'me',
        senderName: 'You',
        content: 'Sure! When works for you?',
        timestamp: new Date(Date.now() - 25 * 60000),
        read: true,
      },
      {
        id: 'msg-3',
        senderId: 'alex',
        senderName: 'Alex Thompson',
        content: 'How about tomorrow at 2 PM?',
        timestamp: new Date(Date.now() - 20 * 60000),
        read: true,
      },
      {
        id: 'msg-4',
        senderId: 'me',
        senderName: 'You',
        content: 'Perfect! I\'ll send a calendar invite.',
        timestamp: new Date(Date.now() - 15 * 60000),
        read: true,
      },
      {
        id: 'msg-5',
        senderId: 'alex',
        senderName: 'Alex Thompson',
        content: 'Thanks for the update!',
        timestamp: new Date(Date.now() - 5 * 60000),
        read: false,
      },
    ],
  },
  {
    id: 'conv-2',
    participantId: 'sarah',
    participantName: 'Sarah Johnson',
    participantAvatar: 'S',
    lastMessage: 'The analytics look great!',
    lastMessageTime: new Date(Date.now() - 2 * 3600000),
    unreadCount: 0,
    messages: [
      {
        id: 'msg-6',
        senderId: 'sarah',
        senderName: 'Sarah Johnson',
        content: 'Hi! I reviewed the campaign metrics.',
        timestamp: new Date(Date.now() - 3 * 3600000),
        read: true,
      },
      {
        id: 'msg-7',
        senderId: 'me',
        senderName: 'You',
        content: 'Great! What did you think?',
        timestamp: new Date(Date.now() - 2.5 * 3600000),
        read: true,
      },
      {
        id: 'msg-8',
        senderId: 'sarah',
        senderName: 'Sarah Johnson',
        content: 'The analytics look great!',
        timestamp: new Date(Date.now() - 2 * 3600000),
        read: true,
      },
    ],
  },
  {
    id: 'conv-3',
    participantId: 'mike',
    participantName: 'Mike Chen',
    participantAvatar: 'M',
    lastMessage: 'Can we discuss the new leads?',
    lastMessageTime: new Date(Date.now() - 1 * 86400000),
    unreadCount: 1,
    messages: [
      {
        id: 'msg-9',
        senderId: 'mike',
        senderName: 'Mike Chen',
        content: 'Can we discuss the new leads?',
        timestamp: new Date(Date.now() - 1 * 86400000),
        read: false,
      },
    ],
  },
];

export const useInboxStore = create<InboxState>((set) => ({
  conversations: mockConversations,
  activeConversationId: null, // Start with no active conversation (show inbox list)
  setActiveConversation: (id) => set({ activeConversationId: id }),
  addMessage: (conversationId, message) =>
    set((state) => ({
      conversations: state.conversations.map((conv) =>
        conv.id === conversationId
          ? {
              ...conv,
              lastMessage: message.content,
              lastMessageTime: message.timestamp,
              unreadCount: message.senderId !== 'me' ? conv.unreadCount + 1 : conv.unreadCount,
              messages: [...conv.messages, message],
            }
          : conv
      ),
    })),
  markAsRead: (conversationId) =>
    set((state) => ({
      conversations: state.conversations.map((conv) =>
        conv.id === conversationId
          ? {
              ...conv,
              unreadCount: 0,
              messages: conv.messages.map((msg) => ({ ...msg, read: true })),
            }
          : conv
      ),
    })),
}));

