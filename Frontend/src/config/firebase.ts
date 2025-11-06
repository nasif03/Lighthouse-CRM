import { initializeApp, getApps, FirebaseApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, Auth } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyB_nk1k3gqAr1KCEljJdEtAcGIlGRArrSw",
  authDomain: "lighthousecrm-6caf2.firebaseapp.com",
  projectId: "lighthousecrm-6caf2",
  storageBucket: "lighthousecrm-6caf2.firebasestorage.app",
  messagingSenderId: "1081029911047",
  appId: "1:1081029911047:web:5e9d1f555f66dc190140a2",
  measurementId: "G-8W57J785KD"
};

// Initialize Firebase - check if already initialized
let app: FirebaseApp;
if (getApps().length === 0) {
  app = initializeApp(firebaseConfig);
} else {
  app = getApps()[0];
}

// Initialize Firebase Auth and get a reference to the service
export const auth: Auth = getAuth(app);

// Initialize Google Auth Provider
export const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({
  prompt: 'select_account'
});

export default app;

