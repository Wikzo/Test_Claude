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
