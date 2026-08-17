import {
  signInAnonymously,
  onAuthStateChanged,
  type User,
} from "firebase/auth";
import {
  collection,
  doc,
  serverTimestamp,
  setDoc,
} from "firebase/firestore";
import { auth, db } from "./firebaseConfig";

const LOCAL_STORAGE_KEY = "todo:groupId";

/**
 * Best-effort, human-readable device label derived from the UA string, e.g.
 * "Chrome on macOS" or "Safari on iOS". Good enough for a members list; not
 * meant to be a precise UA parse.
 */
export function describeDevice(userAgent: string): string {
  const ua = userAgent;

  let browser = "Browser";
  if (/Edg\//.test(ua)) browser = "Edge";
  else if (/OPR\//.test(ua)) browser = "Opera";
  else if (/Chrome\//.test(ua) && !/Chromium/.test(ua)) browser = "Chrome";
  else if (/CriOS\//.test(ua)) browser = "Chrome";
  else if (/Firefox\//.test(ua)) browser = "Firefox";
  else if (/Safari\//.test(ua) && /Version\//.test(ua)) browser = "Safari";

  let os = "Unknown OS";
  if (/Windows/.test(ua)) os = "Windows";
  else if (/Mac OS X/.test(ua) && !/iPhone|iPad/.test(ua)) os = "macOS";
  else if (/iPhone|iPad|iPod/.test(ua)) os = "iOS";
  else if (/Android/.test(ua)) os = "Android";
  else if (/Linux/.test(ua)) os = "Linux";

  return `${browser} on ${os}`;
}

function waitForAuthUser(): Promise<User> {
  return new Promise((resolve, reject) => {
    if (auth.currentUser) {
      resolve(auth.currentUser);
      return;
    }
    const unsubscribe = onAuthStateChanged(
      auth,
      (user) => {
        if (user) {
          unsubscribe();
          resolve(user);
        }
      },
      (error) => {
        unsubscribe();
        reject(error);
      },
    );
  });
}

/**
 * Ensures the current device belongs to a sync group:
 * - Signs in anonymously if not already signed in.
 * - Reuses the groupId cached in localStorage, if any.
 * - Otherwise creates a brand-new solo sync group (syncGroups/{groupId} +
 *   a members/{uid} doc for this device), caches the groupId, and returns it.
 *
 * No pairing/joining-an-existing-group support yet — that's a later phase.
 */
export async function ensureLocalGroup(): Promise<string> {
  if (!auth.currentUser) {
    await signInAnonymously(auth);
  }
  const user = await waitForAuthUser();

  const cachedGroupId = localStorage.getItem(LOCAL_STORAGE_KEY);
  if (cachedGroupId) {
    return cachedGroupId;
  }

  const groupRef = doc(collection(db, "syncGroups"));
  const groupId = groupRef.id;

  await setDoc(groupRef, {
    createdAt: serverTimestamp(),
    createdByUid: user.uid,
  });

  const memberRef = doc(db, "syncGroups", groupId, "members", user.uid);
  await setDoc(memberRef, {
    uid: user.uid,
    platform: "web",
    deviceName: describeDevice(navigator.userAgent),
    joinedAt: serverTimestamp(),
    lastSeenAt: serverTimestamp(),
  });

  localStorage.setItem(LOCAL_STORAGE_KEY, groupId);
  return groupId;
}
