"""Twilio VoIP service"""
import os
from pathlib import Path
from twilio.rest import Client
from twilio.base.exceptions import TwilioRestException
from typing import Optional, Dict

# Path to twil.txt file
TWILIO_CONFIG_PATH = Path(__file__).parent.parent / 'twil.txt'

# Allowed phone number (hardcoded)
ALLOWED_NUMBER = "+8801957128594"  # Also accepts 01957128594 format

def load_twilio_credentials() -> Dict[str, str]:
    """Load Twilio credentials from twil.txt file"""
    credentials = {
        'account_sid': '',
        'auth_token': '',
        'twilio_number': '',
        'allowed_number': ALLOWED_NUMBER
    }
    
    if not TWILIO_CONFIG_PATH.exists():
        raise FileNotFoundError(f"Twilio config file not found at {TWILIO_CONFIG_PATH}")
    
    with open(TWILIO_CONFIG_PATH, 'r') as f:
        for line in f:
            line = line.strip()
            if '=' in line and not line.startswith('#'):
                key, value = line.split('=', 1)
                key = key.strip()
                value = value.strip()
                if key in credentials:
                    credentials[key] = value
    
    # Validate required credentials
    if not credentials['account_sid'] or not credentials['auth_token']:
        raise ValueError("Twilio credentials not found in twil.txt")
    
    if not credentials['twilio_number']:
        raise ValueError("Twilio number not found in twil.txt")
    
    return credentials

def get_twilio_client() -> Client:
    """Get Twilio client instance"""
    creds = load_twilio_credentials()
    return Client(creds['account_sid'], creds['auth_token'])

def normalize_phone_number(phone: str) -> str:
    """Normalize phone number to E.164 format"""
    # Remove all non-digit characters
    cleaned = phone.strip().replace(' ', '').replace('-', '').replace('(', '').replace(')', '')
    
    # Extract just the digits (remove + if present)
    digits = ''.join(filter(str.isdigit, cleaned))
    
    # Handle Bangladesh number format (01957128594 or +8801957128594)
    if digits.startswith('880'):
        # Already has country code (880)
        return '+' + digits
    elif digits.startswith('0'):
        # Remove leading 0 and add +880 (e.g., 01957128594 -> +8801957128594)
        return '+880' + digits[1:]
    elif len(digits) == 10 and digits.startswith('1'):
        # 10 digits starting with 1 (missing country code and leading 0)
        # Assume it's a Bangladesh number
        return '+880' + digits
    elif len(digits) == 11 and digits.startswith('1'):
        # 11 digits starting with 1 (already has leading 0 in digits)
        # This shouldn't happen if we removed the 0, but handle it anyway
        return '+880' + digits
    else:
        # Default: try to add +880 prefix
        if digits:
            return '+880' + digits
        else:
            return phone  # Return original if no digits found

def is_allowed_number(phone: str) -> bool:
    """Check if the phone number is allowed to receive calls"""
    try:
        normalized = normalize_phone_number(phone)
        # The allowed number should normalize to +8801957128594
        # Check if the normalized number ends with 1957128594 (the actual number)
        # This handles: +8801957128594, 8801957128594, 01957128594, 1957128594
        normalized_digits = ''.join(filter(str.isdigit, normalized))
        allowed_digits = '8801957128594'
        
        # Check if it matches the allowed number
        return normalized_digits == allowed_digits or normalized == '+8801957128594'
    except Exception as e:
        print(f"Error checking allowed number: {e}")
        return False

def make_call(to_number: str, message: str = "Hello, this is a call from Lighthouse CRM.") -> Dict:
    """Make a phone call using Twilio"""
    # Check if number is allowed
    if not is_allowed_number(to_number):
        raise ValueError(f"Calling to {to_number} is not allowed. Only {ALLOWED_NUMBER} is allowed.")
    
    try:
        creds = load_twilio_credentials()
        client = get_twilio_client()
        
        # Normalize the phone number
        normalized_to = normalize_phone_number(to_number)
        twilio_number = creds['twilio_number']
        
        # Create TwiML response
        twiml = f'<Response><Say voice="alice">{message}</Say></Response>'
        
        # Make the call
        call = client.calls.create(
            to=normalized_to,
            from_=twilio_number,
            twiml=twiml
        )
        
        return {
            'success': True,
            'call_sid': call.sid,
            'status': call.status,
            'to': normalized_to,
            'from': twilio_number
        }
    except TwilioRestException as e:
        raise Exception(f"Twilio API error: {str(e)}")
    except Exception as e:
        raise Exception(f"Failed to make call: {str(e)}")

def get_call_status(call_sid: str) -> Optional[Dict]:
    """Get the status of a call"""
    try:
        client = get_twilio_client()
        call = client.calls(call_sid).fetch()
        
        return {
            'sid': call.sid,
            'status': call.status,
            'to': call.to,
            'from': call.from_,
            'duration': call.duration,
            'start_time': call.start_time.isoformat() if call.start_time else None,
            'end_time': call.end_time.isoformat() if call.end_time else None,
        }
    except TwilioRestException as e:
        raise Exception(f"Twilio API error: {str(e)}")
    except Exception as e:
        raise Exception(f"Failed to get call status: {str(e)}")

