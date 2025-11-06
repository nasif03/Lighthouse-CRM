# Firebase Authentication Setup Guide

## Error: `auth/configuration-not-found`

This error occurs when Google Sign-In is not enabled in your Firebase Console. Follow these steps to fix it:

### Steps to Enable Google Sign-In:

1. **Go to Firebase Console**
   - Visit: https://console.firebase.google.com/
   - Select your project: `lighthousecrm-6caf2`

2. **Navigate to Authentication**
   - Click on "Authentication" in the left sidebar
   - If you see "Get started", click it to enable Authentication

3. **Enable Google Sign-In Provider**
   - Click on the "Sign-in method" tab
   - Find "Google" in the list of providers
   - Click on "Google"
   - Toggle "Enable" to ON
   - Enter your **Support email** (required)
   - Click "Save"

4. **Configure OAuth Consent Screen (if needed)**
   - If prompted, you may need to configure the OAuth consent screen in Google Cloud Console
   - The project ID is: `lighthousecrm-6caf2`
   - Make sure the authorized domains include your domain

5. **Authorized Domains**
   - In Firebase Console > Authentication > Settings
   - Under "Authorized domains", make sure `localhost` is included
   - For production, add your production domain

### After Enabling:

1. **Refresh your application**
   - The error should be resolved after enabling Google Sign-In
   - Try signing in again

2. **Verify Configuration**
   - Make sure the Firebase config in `src/config/firebase.ts` matches your Firebase project
   - The `authDomain` should match: `lighthousecrm-6caf2.firebaseapp.com`

### Common Issues:

- **"Popup blocked"**: Allow popups for localhost in your browser settings
- **"Configuration not found"**: Google Sign-In provider is not enabled (see steps above)
- **"Invalid API key"**: Check that your Firebase config is correct

### Testing:

After enabling Google Sign-In, you should be able to:
1. Click "Continue with Google" on the login page
2. See the Google sign-in popup
3. Select your Google account
4. Be redirected to the dashboard

If you continue to see errors, check the browser console for more details.

