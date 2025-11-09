import { create } from 'zustand';
import { signInWithPopup, signOut as firebaseSignOut, onAuthStateChanged, User as FirebaseUser } from 'firebase/auth';
import { auth, googleProvider } from '../config/firebase';
import { apiPost } from '../utils/api';

type User = { id: string; name: string; email: string; picture?: string; orgId?: string } | null;

type AuthState = {
  user: User;
  token: string | null;
  isLoading: boolean;
  login: (user: NonNullable<User>, token: string) => void;
  logout: () => Promise<void>;
  signInWithGoogle: () => Promise<void>;
  initializeAuth: () => void;
};

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: null,
  isLoading: true,
  
  login: (user, token) => {
    set({ user, token, isLoading: false });
    // Store token in localStorage
    if (token) {
      localStorage.setItem('auth_token', token);
    }
    // Fetch tenants after login
    if (token && typeof window !== 'undefined') {
      import('./tenantStore').then(({ useTenantStore }) => {
        useTenantStore.getState().fetchTenants();
      });
      // Don't check Gmail auth status automatically - user will click "Connect Gmail" manually
    }
  },
  
  logout: async () => {
    try {
      await firebaseSignOut(auth);
    } catch (error) {
      console.error('Firebase sign out error:', error);
    }
    localStorage.removeItem('auth_token');
    set({ user: null, token: null, isLoading: false });
  },
  
  signInWithGoogle: async () => {
    try {
      set({ isLoading: true });
      // Sign in with Google using Firebase
      const result = await signInWithPopup(auth, googleProvider);
      const idToken = await result.user.getIdToken();
      
      // Get OAuth credential for Gmail access
      const credential = googleProvider.credentialFromResult(result);
      const accessToken = credential?.accessToken;
      const oauthIdToken = credential?.idToken;
      
      // Verify token with backend and get user info
      const data = await apiPost<{ user: NonNullable<User>; token: string }>(
        '/api/auth/verify-token',
        null,
        { id_token: idToken },
        { skipCache: true }
      );
      
      get().login(data.user, data.token);
      
      // Check Gmail auth status after login (will be checked in login function)
      // Gmail connection happens separately via Gmail OAuth flow if needed
    } catch (error: any) {
      console.error('Sign in error:', error);
      set({ isLoading: false });
      
      // Provide more helpful error messages
      if (error?.code === 'auth/configuration-not-found') {
        throw new Error('Firebase Auth is not configured. Please enable Google Sign-In in Firebase Console.');
      } else if (error?.code === 'auth/popup-closed-by-user') {
        throw new Error('Sign-in was cancelled. Please try again.');
      } else if (error?.code === 'auth/popup-blocked') {
        throw new Error('Popup was blocked. Please allow popups for this site.');
      } else if (error?.message) {
        throw new Error(error.message);
      }
      throw error;
    }
  },
  
  initializeAuth: () => {
    set({ isLoading: true });
    
    // Track last verification to avoid excessive API calls
    let lastVerificationTime = 0;
    const VERIFICATION_THROTTLE = 60000; // 1 minute
    
    // Listen for auth state changes
    onAuthStateChanged(auth, async (firebaseUser: FirebaseUser | null) => {
      if (firebaseUser) {
        try {
          const now = Date.now();
          // Throttle token verification to avoid excessive API calls
          if (now - lastVerificationTime < VERIFICATION_THROTTLE && get().user) {
            set({ isLoading: false });
            return;
          }
          
          lastVerificationTime = now;
          const idToken = await firebaseUser.getIdToken();
          
          // Verify token with backend
          // POST requests automatically get unique keys, so no cancellation issues
          const data = await apiPost<{ user: NonNullable<User>; token: string }>(
            '/api/auth/verify-token',
            null,
            { id_token: idToken },
            { 
              skipCache: true
            }
          );
          
          get().login(data.user, data.token);
        } catch (error: any) {
          // Don't log cancellation errors during initialization
          if (!error.message?.includes('Request cancelled')) {
            console.error('Auth initialization error:', error);
          }
          // Only clear auth if it's a real error, not cancellation
          if (error.message && !error.message.includes('Request cancelled')) {
            localStorage.removeItem('auth_token');
            set({ user: null, token: null, isLoading: false });
          } else {
            // For cancellation, just set loading to false - retry will happen
            set({ isLoading: false });
          }
        }
      } else {
        localStorage.removeItem('auth_token');
        set({ user: null, token: null, isLoading: false });
      }
    });
  },
}));


