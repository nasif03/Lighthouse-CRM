"""Stream Chat API routes"""
from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, ValidationError
from typing import Optional, List
from bson import ObjectId
from api.dependencies import get_current_user
from config.database import users_collection
from services.stream_chat_service import (
    create_or_update_user,
    generate_user_token,
    get_or_create_direct_channel,
    get_user_channels,
    send_message,
    get_channel_messages,
)

router = APIRouter(prefix="/api/chat", tags=["chat"])

class CreateUserRequest(BaseModel):
    name: str
    email: str
    image: Optional[str] = None

class CreateChannelRequest(BaseModel):
    user_id: str  # Other user ID to create DM with

class SendMessageRequest(BaseModel):
    channel_type: str = "messaging"
    channel_id: str
    text: str

class StreamTokenResponse(BaseModel):
    token: str
    user_id: str

class ChatUserResponse(BaseModel):
    id: str
    name: str
    email: str
    picture: Optional[str] = None

@router.get("/users", response_model=List[ChatUserResponse])
async def get_chat_users(
    current_user: dict = Depends(get_current_user)
):
    """Get all users in the current user's organization for chat - available to all authenticated users"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        current_user_id = str(user_doc["_id"])
        user_org_id = user_doc.get("orgId")
        
        if not user_org_id:
            raise HTTPException(status_code=400, detail="User is not part of any organization")
        
        # Handle both string and array formats for orgId
        if isinstance(user_org_id, str):
            org_ids = [user_org_id]
        elif isinstance(user_org_id, list):
            org_ids = user_org_id
        else:
            raise HTTPException(status_code=400, detail="Invalid organization ID format")
        
        # Get all users in the same organization(s)
        users_cursor = users_collection.find({
            "$or": [
                {"orgId": {"$in": org_ids}},  # Array format
                {"orgId": {"$in": [org_id for org_id in org_ids]}}  # String format
            ]
        })
        
        # Filter to ensure we only get users that actually have this org_id
        # and exclude the current user
        users = []
        for user in users_cursor:
            user_id = str(user["_id"])
            if user_id == current_user_id:
                continue
            
            user_org_ids = user.get("orgId", [])
            if isinstance(user_org_ids, str):
                user_org_ids = [user_org_ids]
            elif user_org_ids is None:
                continue
            
            # Check if user shares at least one organization with current user
            if any(org_id in user_org_ids for org_id in org_ids):
                users.append(ChatUserResponse(
                    id=user_id,
                    name=user.get("name", "User"),
                    email=user.get("email", ""),
                    picture=user.get("picture")
                ))
        
        return users
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error getting chat users: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to get users: {str(e)}")

@router.post("/users", response_model=dict)
async def create_chat_user(
    request: CreateUserRequest,
    current_user: dict = Depends(get_current_user)
):
    """Create or update Stream Chat user"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        user_id = str(user_doc["_id"])
        
        # Create/update user in Stream Chat
        result = create_or_update_user(
            user_id=user_id,
            name=request.name,
            email=request.email,
            image=request.image
        )
        
        return {"success": True, "user": result}
    except Exception as e:
        print(f"Error creating Stream Chat user: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to create user: {str(e)}")

@router.get("/token", response_model=StreamTokenResponse)
async def get_chat_token(
    current_user: dict = Depends(get_current_user)
):
    """Get Stream Chat token for frontend authentication"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        user_id = str(user_doc["_id"])
        
        # Ensure user exists in Stream Chat
        create_or_update_user(
            user_id=user_id,
            name=user_doc.get("name", "User"),
            email=user_doc.get("email", ""),
            image=user_doc.get("picture")
        )
        
        # Generate token
        token = generate_user_token(user_id)
        
        return StreamTokenResponse(token=token, user_id=user_id)
    except Exception as e:
        print(f"Error generating Stream Chat token: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to generate token: {str(e)}")

@router.post("/channels/direct")
async def create_direct_channel(
    request: CreateChannelRequest,
    current_user: dict = Depends(get_current_user)
):
    """Create or get a direct message channel with another user"""
    try:
        from config.database import users_collection
        from bson import ObjectId
        
        # Debug: Log the request
        print(f"[CHAT] Create channel request received")
        print(f"[CHAT] user_id={request.user_id}, type={type(request.user_id)}")
        print(f"[CHAT] current_user={current_user.get('user_doc', {}).get('_id')}")
        
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        user_id = str(user_doc["_id"])
        other_user_id = request.user_id
        
        # Get other user document
        try:
            other_user_doc = users_collection.find_one({"_id": ObjectId(other_user_id)})
            if not other_user_doc:
                raise HTTPException(status_code=404, detail="Other user not found")
        except:
            raise HTTPException(status_code=404, detail="Invalid user ID")
        
        # Ensure both users exist in Stream Chat
        create_or_update_user(
            user_id=user_id,
            name=user_doc.get("name", "User"),
            email=user_doc.get("email", ""),
            image=user_doc.get("picture")
        )
        
        create_or_update_user(
            user_id=other_user_id,
            name=other_user_doc.get("name", "User"),
            email=other_user_doc.get("email", ""),
            image=other_user_doc.get("picture")
        )
        
        # Get or create channel
        channel_data = get_or_create_direct_channel(user_id, other_user_id)
        
        # Fetch full channel state to get member information
        from services.stream_chat_service import get_stream_client
        client = get_stream_client()
        channel_id = channel_data.get("id")
        if channel_id:
            try:
                channel = client.channel("messaging", channel_id)
                full_state = channel.query(watch=False, state=True)
                if full_state and isinstance(full_state, dict):
                    channel_state = full_state.get("channel", {})
                    if channel_state:
                        # Update channel_data with full state info
                        channel_data.update({
                            "cid": channel_state.get("cid", channel_data.get("cid")),
                            "members": channel_state.get("members", []),
                        })
            except Exception as e:
                print(f"Warning: Could not fetch full channel state: {e}")
        
        return {
            "success": True,
            "channel": channel_data
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error creating direct channel: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to create channel: {str(e)}")

@router.get("/channels", response_model=List[dict])
async def get_channels(
    tenantId: Optional[str] = None,  # Add tenant ID parameter
    current_user: dict = Depends(get_current_user)
):
    """Get all channels for the current user, optionally filtered by tenant"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        user_id = str(user_doc["_id"])
        
        # Use get_user_ids to properly handle orgId (array/string and activeOrgId)
        from utils.query_filters import get_user_ids
        user_ids = get_user_ids(user_doc, active_org_id=tenantId)
        org_id = user_ids["orgId"]
        
        # Get user channels - Stream Chat should filter by org_id if needed
        # Note: Stream Chat channels might need custom filtering based on org_id
        channels = get_user_channels(user_id)
        
        # TODO: Filter channels by org_id if tenant isolation is required at channel level
        # This depends on how Stream Chat stores org_id in channel custom data
        
        # Format response
        from config.database import users_collection
        from bson import ObjectId
        
        result = []
        for channel_item in channels:
            # Stream Chat returns channels in format: {'channel': {...}, 'messages': [], 'members': [], ...}
            # The actual channel data is nested in the 'channel' key
            if not isinstance(channel_item, dict):
                continue
            
            # Extract the actual channel object
            channel = channel_item.get("channel", {})
            if not isinstance(channel, dict):
                print(f"[CHAT] Skipping - channel key not found or not a dict")
                continue
            
            # Extract channel ID, type, and cid from the nested channel object
            channel_id = channel.get("id")
            channel_type = channel.get("type", "messaging")
            channel_cid = channel.get("cid")
            
            # If cid is not set, construct it from type and id
            if not channel_cid and channel_type and channel_id:
                channel_cid = f"{channel_type}:{channel_id}"
            
            print(f"[CHAT] Extracted: id={channel_id}, type={channel_type}, cid={channel_cid}")
            
            # Skip if we still don't have required fields
            if not channel_id:
                print(f"[CHAT] Skipping channel - no ID found")
                continue
            
            # Ensure cid is set
            if not channel_cid:
                channel_cid = f"{channel_type}:{channel_id}"
                print(f"[CHAT] Constructed cid: {channel_cid}")
            
            # Get other participant for direct messages
            # Members are in the top-level channel_item, not in the nested channel
            members = channel_item.get("members", [])
            
            # Use members from channel_item
            all_members = members
            other_member = None
            other_user_id = None
            
            # Handle different member formats
            member_list = []
            if isinstance(all_members, list):
                for m in all_members:
                    if isinstance(m, dict):
                        member_list.append(m)
                    elif isinstance(m, str):
                        member_list.append({"user_id": m})
            
            # Find other member for direct messages (2 members total)
            if len(member_list) == 2:
                for m in member_list:
                    member_id = m.get("user_id") if isinstance(m, dict) else m
                    if member_id and str(member_id) != user_id:
                        other_user_id = str(member_id)
                        # Get user info from our database
                        try:
                            other_user_doc = users_collection.find_one({"_id": ObjectId(other_user_id)})
                            if other_user_doc:
                                other_member = {
                                    "user_id": other_user_id,
                                    "name": other_user_doc.get("name", "User"),
                                    "image": other_user_doc.get("picture"),
                                }
                            else:
                                # Fallback to Stream Chat member data if available
                                other_member = {
                                    "user_id": other_user_id,
                                    "name": m.get("name") if isinstance(m, dict) else "User",
                                    "image": m.get("image") if isinstance(m, dict) else None,
                                }
                        except:
                            # If ObjectId conversion fails, use Stream Chat data
                            other_member = {
                                "user_id": other_user_id,
                                "name": m.get("name") if isinstance(m, dict) else "User",
                                "image": m.get("image") if isinstance(m, dict) else None,
                            }
                        break
            
            # Get last_message_at from channel or from messages
            last_message_at = channel.get("last_message_at")
            if not last_message_at:
                messages = channel_item.get("messages", [])
                if messages and len(messages) > 0:
                    last_message = messages[-1] if isinstance(messages[-1], dict) else messages[0]
                    last_message_at = last_message.get("created_at") if isinstance(last_message, dict) else None
            
            result.append({
                "id": channel_id,
                "type": channel_type,
                "cid": channel_cid,
                "name": channel.get("name"),
                "last_message_at": last_message_at,
                "other_member": other_member,
                "member_count": len(member_list),
            })
            
            print(f"[CHAT] Formatted channel: id={channel_id}, type={channel_type}, cid={channel_cid}, other_member={other_member is not None}")
        
        return result
    except Exception as e:
        print(f"Error getting channels: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to get channels: {str(e)}")

@router.get("/channels/{channel_type}/{channel_id}/messages", response_model=List[dict])
async def get_messages(
    channel_type: str,
    channel_id: str,
    limit: Optional[int] = 50,
    offset: Optional[int] = 0,
    current_user: dict = Depends(get_current_user)
):
    """Get messages from a channel with pagination support"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        # Validate and set defaults
        if limit is None or limit <= 0:
            limit = 50
        if limit > 100:  # Cap at 100 for performance
            limit = 100
        if offset is None or offset < 0:
            offset = 0
        
        messages = get_channel_messages(channel_type, channel_id, limit=limit, offset=offset)
        
        # Format messages
        result = []
        for msg in messages:
            result.append({
                "id": msg.get("id"),
                "text": msg.get("text"),
                "user": msg.get("user"),
                "created_at": msg.get("created_at"),
                "updated_at": msg.get("updated_at"),
            })
        
        return result
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error getting messages: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Failed to get messages: {str(e)}")

@router.post("/messages")
async def send_chat_message(
    request: SendMessageRequest,
    current_user: dict = Depends(get_current_user)
):
    """Send a message to a channel"""
    try:
        user_doc = current_user.get("user_doc")
        if not user_doc:
            raise HTTPException(status_code=404, detail="User not found")
        
        user_id = str(user_doc["_id"])
        
        result = send_message(
            channel_type=request.channel_type,
            channel_id=request.channel_id,
            user_id=user_id,
            text=request.text
        )
        
        return {
            "success": True,
            "message": {
                "id": result.get("message", {}).get("id"),
                "text": result.get("message", {}).get("text"),
                "user": result.get("message", {}).get("user"),
                "created_at": result.get("message", {}).get("created_at"),
            }
        }
    except Exception as e:
        print(f"Error sending message: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to send message: {str(e)}")

