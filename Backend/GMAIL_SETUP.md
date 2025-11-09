# Gmail Integration Setup Guide

This guide explains how to set up Gmail OAuth integration for the Lighthouse CRM.

## Prerequisites

1. Google Cloud Console project (lighthousecrm-6caf2)
2. OAuth 2.0 credentials configured
3. Gmail API enabled

## Google Cloud Console Configuration

### Step 1: Enable Gmail API

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project: `lighthousecrm-6caf2`
3. Navigate to **APIs & Services** > **Library**
4. Search for "Gmail API"
5. Click **Enable**

### Step 2: Configure OAuth Consent Screen

1. Go to **APIs & Services** > **OAuth consent screen**
2. Configure the consent screen:
   - User Type: External (for testing) or Internal (for organization)
   - App name: Lighthouse CRM
   - User support email: Your email
   - Developer contact information: Your email
3. Add scopes:
   - `https://www.googleapis.com/auth/gmail.readonly`
   - `https://www.googleapis.com/auth/gmail.send`
   - `https://www.googleapis.com/auth/gmail.modify`
4. Add test users (if using External user type)
5. Save and continue

### Step 3: Configure OAuth 2.0 Credentials

1. Go to **APIs & Services** > **Credentials**
2. Find your OAuth 2.0 Client ID (or create a new one)
3. Click **Edit**
4. Under **Authorized redirect URIs**, add:
   - `http://localhost:5173` (for frontend development)
   - `http://localhost:3000` (for backend if needed)
   - `http://localhost` (for installed app type)
5. Save the changes

### Step 4: Update client_secret.json

The `client_secret.json` file should have both `installed` and `web` configurations with the correct redirect URIs.

## Backend Setup

1. Ensure the `token` directory exists in the backend:
   ```bash
   mkdir -p Backend/token
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

3. The Gmail service will automatically:
   - Store user tokens in `Backend/token/{user_email}_token.json`
   - Refresh tokens when they expire
   - Handle OAuth flow

## Frontend Setup

1. The Gmail panel is automatically integrated into the app layout
2. After Firebase authentication, Gmail auth status is checked
3. Users can connect their Gmail account by clicking "Connect Gmail"
4. OAuth flow will redirect to Google, then back to the app

## How It Works

1. **User logs in** with Firebase/Google Auth
2. **Gmail auth status is checked** automatically
3. If not authenticated, user sees "Connect Gmail" button
4. User clicks button → redirected to Google OAuth
5. User grants permissions → redirected back to app
6. Backend exchanges code for token → saves token
7. User can now view messages and send emails

## Token Storage

- Tokens are stored in `Backend/token/{user_email}_token.json`
- Each user has their own token file
- Tokens are automatically refreshed when expired
- Tokens include refresh tokens for long-term access

## Troubleshooting

### "Redirect URI mismatch" error
- Ensure `http://localhost:5173` is added to authorized redirect URIs in Google Cloud Console
- Check that `client_secret.json` has the correct redirect URIs

### "Invalid client" error
- Verify `client_secret.json` is in the `Backend` directory
- Check that the client ID and secret are correct

### "Access denied" error
- Ensure Gmail API is enabled in Google Cloud Console
- Check that the correct scopes are requested
- Verify the OAuth consent screen is configured

### Token not refreshing
- Check that the refresh token is included in the token file
- Verify the token file has proper permissions
- Ensure the client secret is correct

## Security Notes

- Never commit `client_secret.json` or token files to version control
- Use environment variables for sensitive data in production
- Implement proper token rotation and revocation
- Use HTTPS in production

## Production Deployment

For production:
1. Update redirect URIs to your production domain
2. Use environment variables for client credentials
3. Implement proper token storage (database instead of files)
4. Use HTTPS for all OAuth redirects
5. Configure CORS properly
6. Implement rate limiting

