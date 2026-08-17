import {
  doc,
  onSnapshot,
  runTransaction,
  serverTimestamp,
  Timestamp,
  type Unsubscribe,
} from "firebase/firestore";
import { db } from "./firebaseConfig";
import type { StreakSummary } from "../types/streak";

function streakDoc(groupId: string) {
  return doc(db, "syncGroups", groupId, "streaks", "summary");
}

/**
 * Subscribes to live updates of the group's streak summary. The doc may not
 * exist yet (no task list has ever been fully cleared) -- callback receives
 * the zero-value default in that case rather than null, so callers don't
 * each need to repeat that fallback.
 */
export function subscribeToStreak(
  groupId: string,
  callback: (summary: StreakSummary) => void,
): Unsubscribe {
  return onSnapshot(streakDoc(groupId), (snap) => {
    const data = snap.data() as Partial<StreakSummary> | undefined;
    callback({
      currentStreak: data?.currentStreak ?? 0,
      lastCompletedAllAt: (data?.lastCompletedAllAt as Timestamp | null | undefined) ?? null,
    });
  });
}

/** Local (browser-timezone) calendar date, stripped of time-of-day. */
function localCalendarDate(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function sameCalendarDate(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

/**
 * Records that every task in the group was just marked complete, updating the
 * daily streak. Meant to be called exactly once per "list just cleared"
 * transition (incomplete count going from >0 to 0) -- see useTasks.ts.
 *
 * Uses the LOCAL calendar day deliberately (not UTC): this is a personal
 * daily-habit streak for a single user/device pair, not something that needs
 * cross-timezone consistency.
 *
 * Runs in a Firestore transaction so concurrent completions from paired
 * devices can't double-increment the streak. Returns the resulting streak
 * count (unchanged if today was already recorded).
 */
export async function recordAllTasksCleared(groupId: string): Promise<number> {
  const today = localCalendarDate(new Date());
  const ref = streakDoc(groupId);

  return runTransaction(db, async (transaction) => {
    const snap = await transaction.get(ref);
    const data = snap.data() as Partial<StreakSummary> | undefined;

    const currentStreak = data?.currentStreak ?? 0;
    const lastCompletedAllAt = (data?.lastCompletedAllAt as Timestamp | null | undefined) ?? null;
    const lastDate = lastCompletedAllAt ? localCalendarDate(lastCompletedAllAt.toDate()) : null;

    if (lastDate && sameCalendarDate(lastDate, today)) {
      // Already recorded today -- avoid double-counting if the user
      // un-completes and re-completes the last task the same day.
      return currentStreak;
    }

    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    const newStreak =
      lastDate && sameCalendarDate(lastDate, yesterday) ? currentStreak + 1 : 1;

    transaction.set(
      ref,
      { currentStreak: newStreak, lastCompletedAllAt: serverTimestamp() },
      { merge: true },
    );

    return newStreak;
  });
}
