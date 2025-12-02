"""Telegram bot integration for Lighthouse CRM Support AI"""
import os
from typing import Optional
from fastapi import Request
from telegram import Update, Bot
from telegram.ext import Application, CommandHandler, MessageHandler, ContextTypes, filters

from services.support_ai import process_support_chat, get_or_create_conversation_id
from config.settings import SUPER_ADMIN_EMAIL
from utils.permissions import is_super_admin

# Read Telegram token from environment
TELEGRAM_TOKEN = os.getenv("TELEGRAM_TOKEN")

# Global bot and application instances
bot: Optional[Bot] = None
application: Optional[Application] = None


async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle /start command"""
    if not update.message:
        return
    
    welcome_message = (
        "Hi! I'm Lighthouse Support AI on Telegram.\n\n"
        "I can help you with:\n"
        "• CRM operations (leads, deals, contacts, accounts)\n"
        "• Jira/JSM ticket management\n"
        "• Calendar and meeting scheduling\n"
        "• Organization and employee management\n"
        "• General questions about the system\n\n"
        "Just send me a message and I'll assist you!"
    )
    
    await update.message.reply_text(welcome_message)


async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handle incoming text messages and process through Support AI"""
    if not update.message or not update.message.text:
        return
    
    user_message = update.message.text.strip()
    
    if not user_message:
        await update.message.reply_text("Please send a message with text.")
        return
    
    try:
        # Get Telegram user ID
        telegram_user_id = str(update.effective_user.id) if update.effective_user else "unknown"
        user_id = f"telegram:{telegram_user_id}"
        org_id = "telegram"
        
        # Create synthetic user_doc for Telegram bot (super admin)
        bot_user_doc = {
            "email": SUPER_ADMIN_EMAIL,
            "_id": "telegram_bot_user",
            "orgId": [org_id],
            "activeOrgId": org_id,
        }
        
        # Get or create conversation ID
        conversation_id = get_or_create_conversation_id(user_id, org_id)
        
        # Process message through Support AI (same logic as web/Android)
        reply_text, conversation_id = await process_support_chat(
            user_message=user_message,
            conversation_id=conversation_id,
            user_id=user_id,
            org_id=org_id,
            user_doc=bot_user_doc,
            auth_token="",  # Empty token - MCP tools will use CRM_API_TOKEN fallback
        )
        
        # Safeguard: Strip any JSON tool call format that might have leaked through
        if reply_text:
            import re
            stripped = reply_text.strip()
            # More aggressive detection - check for JSON anywhere in response
            has_json = (
                '{"tool_call"' in stripped or 
                ('{"name"' in stripped and '"args"' in stripped) or
                ('"tool_call"' in stripped and '"name"' in stripped) or
                (stripped.startswith('{') and '"name"' in stripped and '"args"' in stripped) or
                # Check for JSON in the middle of text (like "text... {name: get_leads, args: {}}")
                ('tool_call' in stripped.lower() and '{' in stripped and '"name"' in stripped) or
                # Check for JSON blocks with newlines
                ('\n{' in stripped and '"name"' in stripped and '"args"' in stripped)
            )
            
            if has_json:
                # Try to extract non-JSON text, or replace entirely
                # Remove JSON patterns
                cleaned = re.sub(r'\{[^}]*"tool_call"[^}]*\}|"tool_call"[^}]*\}', '', stripped, flags=re.DOTALL)
                cleaned = re.sub(r'\{[^}]*"name"[^}]*"args"[^}]*\}', '', cleaned, flags=re.DOTALL)
                # Also remove standalone "tool_call" text
                cleaned = re.sub(r'tool_call\s*\n?\s*\{', '', cleaned, flags=re.IGNORECASE)
                cleaned = cleaned.strip()
                
                if cleaned and len(cleaned) > 20:  # If there's substantial non-JSON text, use it
                    reply_text = cleaned
                else:
                    reply_text = "I processed your request, but I'm having trouble formatting the response. Please try asking again or rephrasing your question."
        
        # Send reply back to Telegram
        # Telegram has a 4096 character limit, so truncate if needed
        if len(reply_text) > 4096:
            reply_text = reply_text[:4090] + "\n\n(Message truncated due to length limit)"
        
        await update.message.reply_text(reply_text)
        
    except Exception as e:
        error_message = f"I encountered an error: {str(e)}. Please try again or contact support."
        print(f"[Telegram Bot] Error processing message: {str(e)}")
        await update.message.reply_text(error_message)


async def init_telegram_bot() -> None:
    """Initialize Telegram bot on startup"""
    global bot, application
    
    if not TELEGRAM_TOKEN:
        print("[Telegram Bot] TELEGRAM_TOKEN not set; Telegram bot will not be initialized.")
        return
    
    try:
        bot = Bot(token=TELEGRAM_TOKEN)
        application = Application.builder().token(TELEGRAM_TOKEN).build()
        
        # Register handlers
        application.add_handler(CommandHandler("start", start_command))
        application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))
        
        # Initialize bot and application
        await bot.initialize()
        await application.initialize()
        await application.start()
        
        print("[Telegram Bot] Telegram bot initialized successfully")
        
    except Exception as e:
        print(f"[Telegram Bot] Failed to initialize: {str(e)}")
        bot = None
        application = None


async def shutdown_telegram_bot() -> None:
    """Shutdown Telegram bot on app shutdown"""
    global application
    
    if application is None:
        return
    
    try:
        await application.stop()
        await application.shutdown()
        print("[Telegram Bot] Telegram bot shut down successfully")
    except Exception as e:
        print(f"[Telegram Bot] Error during shutdown: {str(e)}")


async def telegram_webhook(request: Request) -> dict:
    """
    FastAPI-compatible webhook handler for Telegram updates.
    To be mounted at /telegram_webhook in main.py
    """
    global bot, application
    
    if bot is None or application is None:
        return {"ok": False, "error": "Telegram bot not initialized"}
    
    try:
        data = await request.json()
        update = Update.de_json(data, bot)
        await application.process_update(update)
        return {"ok": True}
    except Exception as e:
        print(f"[Telegram Bot] Webhook error: {str(e)}")
        return {"ok": False, "error": str(e)}

