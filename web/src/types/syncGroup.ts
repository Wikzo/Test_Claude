import type { Timestamp } from "firebase/firestore";

/**
 * Mirrors syncGroups/{groupId} in /docs/data-model.md.
 */
export interface SyncGroup {
  id: string;
  createdAt: Timestamp | null;
  createdByUid: string;
}

/**
 * Mirrors syncGroups/{groupId}/members/{uid} in /docs/data-model.md.
 */
export interface SyncGroupMember {
  uid: string;
  platform: "android" | "web";
  deviceName: string;
  joinedAt: Timestamp | null;
  lastSeenAt: Timestamp | null;
}

/**
 * Result of the `createPairingCode` Cloud Function: a fresh code for this
 * device's group, plus when it stops being claimable.
 */
export interface PairingCode {
  code: string;
  expiresAtMillis: number;
}
