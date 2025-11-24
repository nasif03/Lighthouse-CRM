declare module 'firebase/auth' {
  export interface User {
    uid: string;
    email: string | null;
    displayName: string | null;
    photoURL: string | null;
    getIdToken(forceRefresh?: boolean): Promise<string>;
  }

  export interface Auth {}

  export class GoogleAuthProvider {
    addScope(scope: string): void;
    setCustomParameters(params: Record<string, string>): void;
    static credentialFromResult(result: any): {
      accessToken?: string;
      idToken?: string;
      [key: string]: any;
    } | null;
  }

  export function getAuth(app?: any): Auth;
  export function signInWithPopup(auth: Auth, provider: GoogleAuthProvider): Promise<{
    user: User;
    credential?: any;
  }>;
  export function signOut(auth: Auth): Promise<void>;
  export function onAuthStateChanged(auth: Auth, callback: (user: User | null) => void): () => void;
}

