import React, { createContext, useContext, useEffect, useState } from 'react';
import { auth, googleProvider, signInWithPopup, signOut, onAuthStateChanged, db, doc, getDoc, setDoc } from '../lib/firebase';
import type { FirebaseUser } from '../lib/firebase';
import type { UserRole } from '../types';

interface AuthUser {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  role: UserRole;
}

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  error: string | null;
  signInWithGoogle: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  loading: true,
  error: null,
  signInWithGoogle: async () => {},
  logout: async () => {},
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser: FirebaseUser | null) => {
      if (firebaseUser) {
        // Fetch role from Firestore users collection
        try {
          const userDoc = await getDoc(doc(db, 'users', firebaseUser.uid));
          let role: UserRole = 'user';
          
          if (userDoc.exists()) {
            role = userDoc.data().role || 'user';
          }

          setUser({
            uid: firebaseUser.uid,
            email: firebaseUser.email,
            displayName: firebaseUser.displayName,
            photoURL: firebaseUser.photoURL,
            role,
          });
          setError(null);
        } catch (err) {
          console.error('Error fetching user role:', err);
          setUser({
            uid: firebaseUser.uid,
            email: firebaseUser.email,
            displayName: firebaseUser.displayName,
            photoURL: firebaseUser.photoURL,
            role: 'user',
          });
        }
      } else {
        setUser(null);
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const signInWithGoogle = async () => {
    try {
      setError(null);
      setLoading(true);
      const result = await signInWithPopup(auth, googleProvider);
      const firebaseUser = result.user;

      // Check if user exists in Firestore
      const userDoc = await getDoc(doc(db, 'users', firebaseUser.uid));
      
      if (!userDoc.exists()) {
        // Create user document for first-time login
        await setDoc(doc(db, 'users', firebaseUser.uid), {
          userId: firebaseUser.uid,
          username: firebaseUser.displayName || 'Admin',
          avatarUrl: firebaseUser.photoURL || '',
          email: firebaseUser.email || '',
          phone: '',
          birthdate: '',
          isPrivate: false,
          followers: 0,
          following: 0,
          likes: 0,
          role: 'user', // Default role, admin must upgrade manually
          status: 'active',
          createdAt: Date.now(),
        });
      }

      // Fetch role
      const updatedDoc = await getDoc(doc(db, 'users', firebaseUser.uid));
      const role = updatedDoc.exists() ? (updatedDoc.data().role || 'user') : 'user';

      // Check if user has admin, moderator, or viewer role
      if (role !== 'admin' && role !== 'moderator' && role !== 'viewer') {
        setError('Bạn không có quyền truy cập hệ thống quản trị. Vui lòng liên hệ Admin.');
        await signOut(auth);
        setUser(null);
        setLoading(false);
        return;
      }

      setUser({
        uid: firebaseUser.uid,
        email: firebaseUser.email,
        displayName: firebaseUser.displayName,
        photoURL: firebaseUser.photoURL,
        role,
      });
    } catch (err: any) {
      console.error('Google sign-in error:', err);
      if (err.code === 'auth/popup-closed-by-user') {
        setError(null); // User closed popup, not an error
      } else {
        setError('Đăng nhập thất bại: ' + (err.message || 'Lỗi không xác định'));
      }
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      await signOut(auth);
      setUser(null);
    } catch (err) {
      console.error('Logout error:', err);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, error, signInWithGoogle, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
