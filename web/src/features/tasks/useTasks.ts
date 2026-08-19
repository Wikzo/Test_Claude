import { useCallback, useEffect, useRef, useState } from "react";
import { ensureLocalGroup } from "../../lib/syncGroup";
import { recordAllTasksCleared, subscribeToStreak } from "../../lib/streakRepository";
import {
  deleteTask as deleteTaskRepo,
  setCompleted,
  subscribeToTasks,
} from "../../lib/taskRepository";
import type { Task } from "../../types/task";

interface UseTasksResult {
  tasks: Task[];
  loading: boolean;
  groupId: string | null;
  isOffline: boolean;
  isSyncing: boolean;
  toggleCompleted: (task: Task) => Promise<void>;
  deleteTask: (taskId: string) => Promise<void>;
  refreshGroup: (newGroupId?: string) => void;
  /** Count of incomplete tasks, derived from `tasks`. */
  remaining: number;
  /** Count of completed tasks, derived from `tasks`. */
  completedCount: number;
  /** Current daily-clear streak, live from syncGroups/{groupId}/streaks/summary. */
  streak: number;
  /**
   * One-shot signal for "the list was just fully cleared": bumped exactly
   * once per genuine >0 -> 0 transition of `remaining` (never on initial
   * load, and never merely because a re-render happened while the list is
   * already empty). Consume it with a useEffect keyed on this value -- do
   * NOT treat it as a boolean, since a boolean derived from `remaining === 0`
   * would stay "true" (and could look like it re-fired) across unrelated
   * re-renders while the list stays empty.
   */
  celebrationToken: number;
}

export function useTasks(): UseTasksResult {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [groupId, setGroupId] = useState<string | null>(null);
  const [isOffline, setIsOffline] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const [streak, setStreak] = useState(0);
  const [celebrationToken, setCelebrationToken] = useState(0);
  const mounted = useRef(true);
  // Bumped whenever the app needs to re-resolve/re-subscribe to a group (e.g.
  // right after this device pairs into a different group). `overrideGroupId`
  // lets that call skip the localStorage/ensureLocalGroup round-trip, since
  // the caller already knows the new id.
  const [refreshToken, setRefreshToken] = useState(0);
  const overrideGroupId = useRef<string | undefined>(undefined);

  // Previous remaining-incomplete count and total task count, used to detect
  // the exact >0 -> 0 *completion* transition. `null` means "unknown yet" --
  // the first snapshot after a (re)subscribe only seeds these values, it
  // never counts as a transition (otherwise an already-empty list would
  // "celebrate" on every load). Total count is tracked alongside remaining
  // so that deleting the last remaining task -- which also drives remaining
  // to 0, but isn't a "completed everything" moment -- doesn't celebrate:
  // only a same-total-count transition (a pure completed-flag flip) does.
  const prevRemaining = useRef<number | null>(null);
  const prevTotal = useRef<number | null>(null);

  const refreshGroup = useCallback((newGroupId?: string) => {
    overrideGroupId.current = newGroupId;
    setRefreshToken((token) => token + 1);
  }, []);

  useEffect(() => {
    mounted.current = true;
    setLoading(true);
    prevRemaining.current = null;
    prevTotal.current = null;
    let unsubscribeTasks: (() => void) | undefined;
    let unsubscribeStreak: (() => void) | undefined;

    const resolveGroupId = overrideGroupId.current
      ? Promise.resolve(overrideGroupId.current)
      : ensureLocalGroup();

    resolveGroupId
      .then((id) => {
        if (!mounted.current) return;
        setGroupId(id);

        unsubscribeStreak = subscribeToStreak(id, (summary) => {
          if (!mounted.current) return;
          setStreak(summary.currentStreak);
        });

        unsubscribeTasks = subscribeToTasks(id, (snapshot) => {
          if (!mounted.current) return;
          setTasks(snapshot.tasks);
          setIsOffline(snapshot.isFromCache);
          setIsSyncing(snapshot.hasPendingWrites);
          setLoading(false);

          const remainingNow = snapshot.tasks.filter((t) => !t.completed).length;
          const totalNow = snapshot.tasks.length;
          const previousRemaining = prevRemaining.current;
          const previousTotal = prevTotal.current;
          prevRemaining.current = remainingNow;
          prevTotal.current = totalNow;

          const justCompletedEverything =
            previousRemaining !== null &&
            previousTotal !== null &&
            previousRemaining > 0 &&
            remainingNow === 0 &&
            totalNow === previousTotal;

          if (justCompletedEverything) {
            setCelebrationToken((token) => token + 1);
            recordAllTasksCleared(id).catch((error) => {
              console.error("Failed to record streak", error);
            });
          }
        });
      })
      .catch((error) => {
        console.error("Failed to set up sync group", error);
        if (mounted.current) setLoading(false);
      });

    return () => {
      mounted.current = false;
      unsubscribeTasks?.();
      unsubscribeStreak?.();
    };
  }, [refreshToken]);

  const toggleCompleted = useCallback(
    async (task: Task) => {
      if (!groupId) return;
      await setCompleted(groupId, task.id, !task.completed);
    },
    [groupId],
  );

  const deleteTask = useCallback(
    async (taskId: string) => {
      if (!groupId) return;
      await deleteTaskRepo(groupId, taskId);
    },
    [groupId],
  );

  const remaining = tasks.filter((t) => !t.completed).length;
  const completedCount = tasks.length - remaining;

  return {
    tasks,
    loading,
    groupId,
    isOffline,
    isSyncing,
    toggleCompleted,
    deleteTask,
    refreshGroup,
    remaining,
    completedCount,
    streak,
    celebrationToken,
  };
}
