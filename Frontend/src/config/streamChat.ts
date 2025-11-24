import { StreamChat } from 'stream-chat';

// Stream Chat configuration
export const STREAM_CHAT_API_KEY = 'n8nh34grh4b3';

// Stream Chat client instance (will be initialized with user token)
let streamClient: StreamChat | null = null;

export const getStreamClient = (): StreamChat | null => {
  return streamClient;
};

export const initializeStreamClient = async (userToken: string, userId: string): Promise<StreamChat> => {
  console.log('[initializeStreamClient] Initializing with userId:', userId, 'hasToken:', !!userToken);
  
  const client = StreamChat.getInstance(STREAM_CHAT_API_KEY);
  
  console.log('[initializeStreamClient] Got client instance, current userID:', client.userID);
  
  if (!client.userID || client.userID !== userId) {
    console.log('[initializeStreamClient] Connecting user...');
    try {
      await client.connectUser(
        {
          id: userId,
        },
        userToken
      );
      console.log('[initializeStreamClient] User connected successfully, userID:', client.userID);
    } catch (error) {
      console.error('[initializeStreamClient] Error connecting user:', error);
      throw error;
    }
  } else {
    console.log('[initializeStreamClient] User already connected');
  }
  
  streamClient = client;
  return client;
};

export const disconnectStreamClient = async (): Promise<void> => {
  if (streamClient) {
    await streamClient.disconnectUser();
    streamClient = null;
  }
};

