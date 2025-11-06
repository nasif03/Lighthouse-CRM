import { create } from 'zustand';
import { signInWithPopup, signOut as firebaseSignOut, onAuthStateChanged, User as FirebaseUser } from 'firebase/auth';
import { auth, googleProvider } from '../config/firebase';

const API_BASE_URL = 'http://localhost:3000';

type User = { id: string; name: string; email: string; picture?: string } | null;

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
      
      // Verify token with backend and get user info
      const response = await fetch(`${API_BASE_URL}/api/auth/verify-token`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ id_token: idToken }),
      });
      
      if (!response.ok) {
        throw new Error('Failed to verify token');
      }
      
      const data = await response.json();
      get().login(data.user, data.token);
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
    
    // Check for stored token first
    const storedToken = localStorage.getItem('auth_token');
    if (storedToken) {
      // Try to verify stored token
      // For now, we'll rely on Firebase auth state
    }
    
    // Listen for auth state changes
    onAuthStateChanged(auth, async (firebaseUser: FirebaseUser | null) => {
      if (firebaseUser) {
        try {
          const idToken = await firebaseUser.getIdToken();
          
          // Verify token with backend
          const response = await fetch(`${API_BASE_URL}/api/auth/verify-token`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ id_token: idToken }),
          });
          
          if (response.ok) {
            const data = await response.json();
            get().login(data.user, data.token);
          } else {
            // Clear invalid token
            localStorage.removeItem('auth_token');
            set({ user: null, token: null, isLoading: false });
          }
        } catch (error) {
          console.error('Auth initialization error:', error);
          localStorage.removeItem('auth_token');
          set({ user: null, token: null, isLoading: false });
        }
      } else {
        localStorage.removeItem('auth_token');
        set({ user: null, token: null, isLoading: false });
      }
    });
  },
}));


