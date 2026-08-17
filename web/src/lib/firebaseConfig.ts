import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import {
  initializeFirestore,
  persistentLocalCache,
  persistentSingleTabManager,
} from "firebase/firestore";
import { getFunctions } from "firebase/functions";

// TODO: replace with your real Firebase config.
// Get this from Firebase Console -> Project settings -> General -> Your apps -> Web app.
// These placeholder values will not connect to a real project.
const firebaseConfig = {
  apiKey: "REPLACE_ME",
  authDomain: "REPLACE_ME.firebaseapp.com",
  projectId: "REPLACE_ME",
  storageBucket: "REPLACE_ME.appspot.com",
  messagingSenderId: "REPLACE_ME",
  appId: "REPLACE_ME",
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);

// Offline-first: cache reads/writes locally and sync when connectivity allows.
// Single-tab manager is enough for v1 -- this is a personal app, not one built
// to have several browser tabs of the same account fighting over one cache.
export const db = initializeFirestore(app, {
  localCache: persistentLocalCache({
    tabManager: persistentSingleTabManager(undefined),
  }),
});

export const functions = getFunctions(app);
