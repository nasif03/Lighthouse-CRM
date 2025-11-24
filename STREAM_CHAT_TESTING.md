# Stream Chat Integration - Testing Guide

## Setup Complete ✅

The Stream Chat integration has been implemented with the following components:

### Backend:
- **Stream Chat Service** (`Backend/services/stream_chat_service.py`): Handles user creation, channel management, and messaging
- **Chat API Routes** (`Backend/api/routes/chat.py`): REST endpoints for chat operations
- **Configuration**: Stream Chat credentials added to `Backend/config/settings.py`

### Frontend:
- **Stream Chat Hook** (`Frontend/src/hooks/useStreamChat.ts`): Initializes Stream Chat client
- **Stream Conversation View** (`Frontend/src/components/inbox/StreamConversationView.tsx`): Real-time chat interface
- **Updated Inbox Store**: Fetches real conversations from Stream Chat
- **Updated Inbox Sidebar**: Displays real conversations

## How to Test

### Prerequisites:
1. **Backend running** on `http://localhost:3000`
2. **Frontend running** on `http://localhost:5173`
3. **Two user accounts** logged in (in different browsers or incognito windows)

### Step 1: Install Dependencies

```bash
# Backend
cd Backend
pip install stream-chat==7.0.0

# Frontend (if not already installed)
cd Frontend
npm install stream-chat@8.15.0 stream-chat-react@11.7.0
```

### Step 2: Start the Application

```bash
# Terminal 1 - Backend
cd Backend
python -m uvicorn main:app --reload

# Terminal 2 - Frontend
cd Frontend
npm run dev
```

### Step 3: Test Chat Functionality

#### Test 1: Initial Setup
1. **User A**: Log in to the CRM
2. **User A**: Navigate to the Inbox page
3. **User A**: You should see "No conversations yet" (if this is the first time)
4. **User A**: The Stream Chat client should initialize automatically

#### Test 2: Create a Conversation
**Note**: Currently, conversations are created automatically when two users interact. To test manually:

1. **User A**: Note your User ID (check browser console or backend logs)
2. **User B**: Log in with a different account
3. **User B**: Note your User ID

**Option A - Using Backend API directly:**
```bash
# As User A, create a direct channel with User B
curl -X POST http://localhost:3000/api/chat/channels/direct \
  -H "Authorization: Bearer <USER_A_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"user_id": "<USER_B_ID>"}'
```

**Option B - Using Frontend (if you add a "New Chat" button):**
- This would require adding UI to select a user and create a channel

#### Test 3: Send Messages
1. **User A**: Click on a conversation (or create one)
2. **User A**: Type a message and press Enter or click Send
3. **User B**: Open the same conversation
4. **User B**: You should see User A's message in real-time
5. **User B**: Reply with a message
6. **User A**: You should see User B's reply in real-time

#### Test 4: Real-time Updates
1. **User A**: Send a message
2. **User B**: The message should appear immediately without refreshing
3. **User B**: Send a reply
4. **User A**: The reply should appear immediately

#### Test 5: Multiple Conversations
1. **User A**: Create conversations with multiple users
2. **User A**: Check the inbox sidebar - all conversations should appear
3. **User A**: Click between different conversations
4. **User A**: Each conversation should load its own messages

### Step 4: Verify Data Persistence

1. **User A**: Send a message
2. **User A**: Refresh the page
3. **User A**: The message should still be there (stored in Stream Chat)

### Step 5: Test Error Handling

1. **Disconnect internet**: Try to send a message - should show an error
2. **Invalid channel**: Try to access a non-existent channel - should show appropriate error
3. **Unauthorized**: Try to access chat without token - should redirect to login

## Troubleshooting

### Issue: "Loading chat..." forever
- **Check**: Backend is running and accessible
- **Check**: Stream Chat credentials are correct in `Backend/config/settings.py`
- **Check**: Browser console for errors
- **Solution**: Verify `/api/chat/token` endpoint returns a valid token

### Issue: "No conversations yet" even after creating one
- **Check**: Both users are created in Stream Chat
- **Check**: Channel was created successfully (check backend logs)
- **Solution**: Try creating the channel again via API

### Issue: Messages not appearing in real-time
- **Check**: Stream Chat client is connected (check browser console)
- **Check**: Channel is being watched (check Network tab)
- **Solution**: Refresh the page and try again

### Issue: "Failed to initialize chat"
- **Check**: User token is valid
- **Check**: Stream Chat API key is correct
- **Check**: Backend `/api/chat/token` endpoint is working
- **Solution**: Check backend logs for detailed error messages

## API Endpoints

### Get Chat Token
```
GET /api/chat/token
Authorization: Bearer <token>
```

### Get All Channels
```
GET /api/chat/channels
Authorization: Bearer <token>
```

### Create Direct Channel
```
POST /api/chat/channels/direct
Authorization: Bearer <token>
Body: { "user_id": "<other_user_id>" }
```

### Send Message
```
POST /api/chat/messages
Authorization: Bearer <token>
Body: {
  "channel_type": "messaging",
  "channel_id": "<channel_id>",
  "text": "Hello!"
}
```

### Get Channel Messages
```
GET /api/chat/channels/{channel_type}/{channel_id}/messages
Authorization: Bearer <token>
```

## Next Steps (Optional Enhancements)

1. **Add "New Chat" UI**: Button to start a conversation with a user
2. **User Search**: Search for users to start conversations with
3. **Unread Counts**: Show unread message counts in sidebar
4. **Typing Indicators**: Show when someone is typing
5. **Message Status**: Show sent/delivered/read status
6. **File Attachments**: Support for sending files/images
7. **Notifications**: Browser notifications for new messages

