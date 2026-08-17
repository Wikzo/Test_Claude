import type { Timestamp } from "firebase/firestore";

/**
 * Mirrors the TypeScript representation in /docs/data-model.md exactly.
 * Do not diverge from that document without updating it first — it is the
 * canonical schema shared with the Android client.
 */
export interface StreakSummary {
  currentStreak: number;
  lastCompletedAllAt: Timestamp | null;
}
