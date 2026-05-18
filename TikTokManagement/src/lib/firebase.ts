import { initializeApp } from 'firebase/app';
import { getFirestore, collection, onSnapshot, doc, updateDoc, addDoc, query, orderBy, limit, getDocs, where, getCountFromServer, Timestamp, getDoc, setDoc } from 'firebase/firestore';
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut, onAuthStateChanged, createUserWithEmailAndPassword } from 'firebase/auth';
import type { User as FirebaseUser } from 'firebase/auth';

export const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID
};

export const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();

// Re-export commonly used Firestore functions
export {
  collection,
  onSnapshot,
  doc,
  updateDoc,
  addDoc,
  query,
  orderBy,
  limit,
  getDocs,
  getDoc,
  setDoc,
  where,
  getCountFromServer,
  Timestamp
};

// Re-export Auth functions
export {
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  createUserWithEmailAndPassword
};

export type { FirebaseUser };
