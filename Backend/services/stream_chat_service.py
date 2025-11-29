"""Stream Chat service for real-time messaging"""
from stream_chat import StreamChat
from config.settings import STREAM_CHAT_API_KEY, STREAM_CHAT_API_SECRET, STREAM_CHAT_APP_ID
from typing import Optional, Dict, List
from datetime import datetime
import requests
import time
import jwt

# Initialize Stream Chat client
_stream_client: Optional[StreamChat] = None

def get_stream_client() -> StreamChat:
    """Get or create Stream Chat client instance"""
    global _stream_client
    if _stream_client is None:
        _stream_client = StreamChat(
            api_key=STREAM_CHAT_API_KEY,
            api_secret=STREAM_CHAT_API_SECRET
        )
    return _stream_client

def create_or_update_user(user_id: str, name: str, email: str, image: Optional[str] = None) -> Dict:
    """Create or update a user in Stream Chat"""
    client = get_stream_client()
    
    user_data = {
        "id": user_id,
        "name": name,
        "email": email,
    }
    
    if image:
        user_data["image"] = image
    
    response = client.update_user(user_data)
    return response

def generate_user_token(user_id: str) -> str:
    """Generate a Stream Chat token for a user (for frontend authentication)"""
    client = get_stream_client()
    token = client.create_token(user_id)
    return token

def create_channel(channel_type: str, channel_id: str, members: List[str], created_by: str, name: Optional[str] = None) -> Dict:
    """Create a channel/conversation in Stream Chat"""
    client = get_stream_client()
    
    channel = client.channel(channel_type, channel_id)
    
    channel_data = {
        "members": members,
    }
    
    if name:
        channel_data["name"] = name
    
    # channel.create() only takes the channel_data dictionary
    # Don't pass created_by - Stream Chat will infer it
    channel.create(channel_data)
    
    return channel

def get_or_create_direct_channel(user1_id: str, user2_id: str):
    """Get or create a direct message channel between two users"""
    client = get_stream_client()
    
    # Ensure user IDs are strings
    user1_id = str(user1_id)
    user2_id = str(user2_id)
    
    # Sort user IDs to ensure consistent channel ID
    sorted_ids = sorted([user1_id, user2_id])
    channel_id = f"direct-{sorted_ids[0]}-{sorted_ids[1]}"
    
    # Try to get existing channel first
    try:
        filter_conditions = {"id": channel_id}
        result = client.query_channels(filter_conditions)
        if result.get("channels"):
            channel = result["channels"][0]
            return {
                "id": channel.get("id", channel_id),
                "type": channel.get("type", "messaging"),
                "cid": channel.get("cid", f"messaging:{channel_id}"),
            }
    except Exception as e:
        print(f"Error querying existing channel: {e}")
    
    # Create new channel - pass created_by as object with id as string
    # Error says: "expected object for field data.created_by" 
    # So created_by must be an object, and created_by.id must be a string
    try:
        # Create channel object
        channel = client.channel("messaging", channel_id)
        
        # Ensure user IDs are definitely strings
        user1_id_str = str(user1_id).strip()
        user2_id_str = str(user2_id).strip()
        
        # Pass created_by as object with id as string
        channel_data = {
            "members": [user1_id_str, user2_id_str],
            "created_by": {
                "id": user1_id_str  # Object with id as string
            }
        }
        
        print(f"[DEBUG] Creating channel with data: {channel_data}")
        
        # Use query() method - this is what create() calls internally
        response = channel.query(
            watch=True,
            state=True,
            data=channel_data
        )
        
        # The response should contain channel data
        if isinstance(response, dict):
            # Response might have 'channel' key or be the channel data itself
            channel_data = response.get("channel", response)
            if isinstance(channel_data, dict):
                return {
                    "id": channel_data.get("id", channel_id),
                    "type": channel_data.get("type", "messaging"),
                    "cid": channel_data.get("cid", f"messaging:{channel_id}"),
                }
        
        # Fallback: return basic structure
        return {
            "id": channel_id,
            "type": "messaging",
            "cid": f"messaging:{channel_id}",
        }
    except Exception as e:
        print(f"Error creating channel: {e}")
        print(f"Error type: {type(e)}")
        import traceback
        traceback.print_exc()
        raise

def get_user_channels(user_id: str, limit: int = 20) -> List[Dict]:
    """Get all channels for a user with full state including member details"""
    client = get_stream_client()
    
    # query_channels requires filter_conditions as first positional argument
    # and other parameters as keyword arguments
    filter_conditions = {"members": {"$in": [user_id]}}
    result = client.query_channels(
        filter_conditions,
        sort=[{"last_message_at": -1}],
        limit=limit,
        state=True,  # Include full channel state with member details
        watch=False,  # Don't watch, just query
    )
    
    print(f"[get_user_channels] Result type: {type(result)}")
    print(f"[get_user_channels] Result keys: {list(result.keys()) if isinstance(result, dict) else 'not dict'}")
    
    channels = result.get("channels", [])
    print(f"[get_user_channels] Found {len(channels)} channels")
    
    # Debug: log first channel structure
    if channels and len(channels) > 0:
        first_channel = channels[0]
        print(f"[get_user_channels] First channel type: {type(first_channel)}")
        if isinstance(first_channel, dict):
            print(f"[get_user_channels] First channel keys: {list(first_channel.keys())}")
            print(f"[get_user_channels] First channel id: {first_channel.get('id')}, cid: {first_channel.get('cid')}")
            if 'state' in first_channel:
                state = first_channel.get('state', {})
                if isinstance(state, dict):
                    print(f"[get_user_channels] First channel state keys: {list(state.keys())}")
                    print(f"[get_user_channels] First channel state id: {state.get('id')}, cid: {state.get('cid')}")
    
    return channels

def send_message(channel_type: str, channel_id: str, user_id: str, text: str) -> Dict:
    """Send a message to a channel"""
    client = get_stream_client()
    
    channel = client.channel(channel_type, channel_id)
    response = channel.send_message({
        "text": text,
    }, user_id)
    
    return response

def get_channel_messages(channel_type: str, channel_id: str, limit: int = 50, offset: int = 0) -> List[Dict]:
    """Get messages from a channel with pagination support"""
    client = get_stream_client()
    
    channel = client.channel(channel_type, channel_id)
    
    # Stream Chat SDK's channel.get_messages() method signature varies by version
    # We'll use a try-except approach to handle different SDK versions
    try:
        # Try using query() method with message pagination
        # This is the recommended way for pagination in Stream Chat
        response = channel.query(
            messages={
                "limit": limit,
                "offset": offset
            },
            watch=False,
            state=True
        )
        
        # Extract messages from response
        if isinstance(response, dict):
            messages = response.get("messages", [])
            # Messages are typically returned in reverse chronological order (newest first)
            # Reverse to get chronological order (oldest first)
            return list(reversed(messages)) if messages else []
        return []
    except TypeError as e:
        # If query() doesn't accept messages parameter, try alternative approach
        print(f"Query with messages parameter failed: {e}, trying alternative method")
        try:
            # Alternative: Use query() to get channel state, then get messages separately
            channel.query(watch=False, state=True)
            # Now try to get messages with limit
            # Some SDK versions support get_messages with limit parameter
            try:
                response = channel.get_messages(limit=limit + offset)
                if isinstance(response, dict):
                    messages = response.get("messages", [])
                    # Apply offset manually
                    if offset > 0 and len(messages) > offset:
                        messages = messages[offset:]
                    # Apply limit
                    if len(messages) > limit:
                        messages = messages[:limit]
                    # Reverse for chronological order
                    return list(reversed(messages)) if messages else []
                return []
            except TypeError:
                # get_messages() doesn't accept limit parameter
                # Get all messages and apply pagination manually
                response = channel.get_messages()
                if isinstance(response, dict):
                    messages = response.get("messages", [])
                    # Apply offset and limit manually
                    if offset > 0 and len(messages) > offset:
                        messages = messages[offset:]
                    if len(messages) > limit:
                        messages = messages[:limit]
                    # Reverse for chronological order
                    return list(reversed(messages)) if messages else []
                return []
        except Exception as e2:
            print(f"Alternative method failed: {e2}")
            # Final fallback: get all messages and paginate manually
            try:
                response = channel.get_messages()
                if isinstance(response, dict):
                    messages = response.get("messages", [])
                    # Apply offset and limit manually
                    if offset > 0 and len(messages) > offset:
                        messages = messages[offset:]
                    if len(messages) > limit:
                        messages = messages[:limit]
                    # Reverse for chronological order
                    return list(reversed(messages)) if messages else []
                return []
            except Exception as e3:
                print(f"Final fallback failed: {e3}")
                import traceback
                traceback.print_exc()
                return []
    except Exception as e:
        print(f"Error querying channel messages: {e}")
        import traceback
        traceback.print_exc()
        # Fallback: try basic get_messages
        try:
            response = channel.get_messages()
            if isinstance(response, dict):
                messages = response.get("messages", [])
                # Apply offset and limit manually
                if offset > 0 and len(messages) > offset:
                    messages = messages[offset:]
                if len(messages) > limit:
                    messages = messages[:limit]
                # Reverse for chronological order
                return list(reversed(messages)) if messages else []
            return []
        except Exception as e2:
            print(f"Error in final fallback: {e2}")
            return []

