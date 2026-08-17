import { useCallback, useEffect, useRef, useState } from "react";
import { ensureLocalGroup } from "../../lib/syncGroup";
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
}

export function useTasks(): UseTasksResult {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [groupId, setGroupId] = useState<string | null>(null);
  const [isOffline, setIsOffline] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const mounted = useRef(true);
  // Bumped whenever the app needs to re-resolve/re-subscribe to a group (e.g.
  // right after this device pairs into a different group). `overrideGroupId`
  // lets that call skip the localStorage/ensureLocalGroup round-trip, since
  // the caller already knows the new id.
  const [refreshToken, setRefreshToken] = useState(0);
  const overrideGroupId = useRef<string | undefined>(undefined);

  const refreshGroup = useCallback((newGroupId?: string) => {
    overrideGroupId.current = newGroupId;
    setRefreshToken((token) => token + 1);
  }, []);

  useEffect(() => {
    mounted.current = true;
    setLoading(true);
    let unsubscribe: (() => void) | undefined;

    const resolveGroupId = overrideGroupId.current
      ? Promise.resolve(overrideGroupId.current)
      : ensureLocalGroup();

    resolveGroupId
      .then((id) => {
        if (!mounted.current) return;
        setGroupId(id);
        unsubscribe = subscribeToTasks(id, (snapshot) => {
          if (!mounted.current) return;
          setTasks(snapshot.tasks);
          setIsOffline(snapshot.isFromCache);
          setIsSyncing(snapshot.hasPendingWrites);
          setLoading(false);
        });
      })
      .catch((error) => {
        console.error("Failed to set up sync group", error);
        if (mounted.current) setLoading(false);
      });

    return () => {
      mounted.current = false;
      unsubscribe?.();
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

  return {
    tasks,
    loading,
    groupId,
    isOffline,
    isSyncing,
    toggleCompleted,
    deleteTask,
    refreshGroup,
  };
}
