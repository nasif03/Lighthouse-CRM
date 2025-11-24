import { create } from 'zustand';
import { apiGet, apiPost } from '../utils/api';

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
  // Stream Chat specific
  channelId?: string;
  channelType?: string;
  cid?: string;
};

type InboxState = {
  conversations: Conversation[];
  activeConversationId: string | null;
  isLoading: boolean;
  setActiveConversation: (id: string | null) => void;
  addMessage: (conversationId: string, message: Message) => void;
  markAsRead: (conversationId: string) => void;
  fetchConversations: (token: string) => Promise<void>;
  createDirectChannel: (token: string, otherUserId: string) => Promise<Conversation | null>;
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

export const useInboxStore = create<InboxState>((set, get) => ({
  conversations: [],
  activeConversationId: null,
  isLoading: false,
  
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
  
  fetchConversations: async (token: string) => {
    set({ isLoading: true });
    try {
      console.log('[fetchConversations] Fetching channels from API...');
      const channels = await apiGet<Array<{
        id: string;
        type: string;
        cid: string;
        name?: string;
        last_message_at?: string;
        other_member?: {
          user_id: string;
          name?: string;
          image?: string;
        };
        member_count: number;
      }>>('/api/chat/channels', token);
      
      console.log('[fetchConversations] Received channels:', channels.length, channels);
      
      // Convert Stream Chat channels to Conversation format
      const conversations: Conversation[] = channels
        .map((channel) => {
          console.log('[fetchConversations] Processing channel:', channel);
          console.log('[fetchConversations] Channel keys:', Object.keys(channel));
          console.log('[fetchConversations] Channel.id:', channel.id, 'channel.cid:', channel.cid, 'channel.type:', channel.type);
          
          const otherMember = channel.other_member;
          const lastMessageTime = channel.last_message_at 
            ? new Date(channel.last_message_at) 
            : new Date();
          
          // Ensure we have valid IDs - use cid as primary, fallback to id
          // Don't create IDs with null values
          if (!channel.id && !channel.cid) {
            console.warn('[fetchConversations] Channel missing both id and cid. Full channel:', JSON.stringify(channel, null, 2));
            return null; // Skip invalid channels
          }
          
          const conversationId = channel.cid || (channel.id ? `messaging:${channel.id}` : null);
          const channelId = channel.id;
          const channelType = channel.type || 'messaging';
          
          if (!conversationId || !channelId) {
            console.warn('[fetchConversations] Invalid channel data:', { conversationId, channelId, channelType, fullChannel: channel });
            return null; // Skip invalid channels
          }
          
          const conversation: Conversation = {
            id: conversationId,
            participantId: otherMember?.user_id || channelId,
            participantName: otherMember?.name || channel.name || 'Unknown',
            participantAvatar: otherMember?.image,
            lastMessage: '', // Will be populated from channel data
            lastMessageTime,
            unreadCount: 0, // Stream Chat handles this
            messages: [],
            channelId: channelId,
            channelType: channelType,
            cid: channel.cid || conversationId,
          };
          
          console.log('[fetchConversations] Created conversation:', conversation);
          return conversation;
        })
        .filter((conv): conv is Conversation => conv !== null); // Remove null entries
      
      set({ conversations, isLoading: false });
    } catch (error: any) {
      console.error('Failed to fetch conversations:', error);
      set({ isLoading: false });
      // Fallback to empty array on error
      set({ conversations: [] });
    }
  },
  
  createDirectChannel: async (token: string, otherUserId: string): Promise<Conversation | null> => {
    try {
      // Validate input
      if (!otherUserId || typeof otherUserId !== 'string') {
        console.error('Invalid user ID:', otherUserId);
        throw new Error('Invalid user ID');
      }
      
      console.log('Creating direct channel with user_id:', otherUserId);
      const requestBody = { user_id: otherUserId };
      console.log('Request body:', requestBody);
      
      const response = await apiPost<{
        success: boolean;
        channel: {
          id: string;
          type: string;
          cid: string;
        };
      }>(
        '/api/chat/channels/direct',
        token,
        requestBody,
        { skipCache: true }
      );
      
      console.log('Create channel response:', response);
      
      if (response.success && response.channel) {
        console.log('[createDirectChannel] Channel created:', response.channel);
        
        // Fetch updated conversations
        console.log('[createDirectChannel] Fetching conversations...');
        await get().fetchConversations(token);
        
        // Wait a bit for the conversations to be fetched
        await new Promise(resolve => setTimeout(resolve, 300));
        
        const allConversations = get().conversations;
        console.log('[createDirectChannel] Conversations after fetch:', allConversations.length);
        console.log('[createDirectChannel] Looking for channel:', {
          cid: response.channel.cid,
          id: response.channel.id,
          type: response.channel.type
        });
        
        // Try to find the new conversation by cid or id
        const newConv = allConversations.find(c => {
          const matches = 
            c.cid === response.channel.cid || 
            c.channelId === response.channel.id ||
            c.id === response.channel.cid ||
            c.id === `messaging:${response.channel.id}` ||
            (c.channelId && c.channelId === response.channel.id);
          if (matches) {
            console.log('[createDirectChannel] Found matching conversation:', c);
          }
          return matches;
        });
        
        if (newConv) {
          console.log('[createDirectChannel] Returning found conversation');
          return newConv;
        }
        
        // If not found, create a temporary conversation object using the channel data
        console.log('[createDirectChannel] Conversation not found, creating temporary object');
        const tempConv: Conversation = {
          id: response.channel.cid || `messaging:${response.channel.id}`,
          participantId: '', // Will be filled on next fetch
          participantName: 'Loading...',
          lastMessage: '',
          lastMessageTime: new Date(),
          unreadCount: 0,
          messages: [],
          channelId: response.channel.id,
          channelType: response.channel.type || 'messaging',
          cid: response.channel.cid || `messaging:${response.channel.id}`,
        };
        
        // Add it to the conversations list temporarily
        set({ conversations: [...allConversations, tempConv] });
        
        return tempConv;
      }
      return null;
    } catch (error: any) {
      console.error('Failed to create direct channel - Full error:', error);
      console.error('Error status:', error.status);
      console.error('Error response:', error.response);
      console.error('Error message:', error.message);
      throw error; // Re-throw so the UI can handle it
    }
  },
}));

