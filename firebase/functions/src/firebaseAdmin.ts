import { initializeApp, getApps } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

// Initialize the Admin SDK exactly once, no matter how many function files
// import this module (Cloud Functions can share a container/module cache
// across invocations, so guard against double-init during local reloads too).
if (getApps().length === 0) {
  initializeApp();
}

export const db = getFirestore();
