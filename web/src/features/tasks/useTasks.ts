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
  toggleCompleted: (task: Task) => Promise<void>;
  deleteTask: (taskId: string) => Promise<void>;
}

export function useTasks(): UseTasksResult {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [groupId, setGroupId] = useState<string | null>(null);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    let unsubscribe: (() => void) | undefined;

    ensureLocalGroup()
      .then((id) => {
        if (!mounted.current) return;
        setGroupId(id);
        unsubscribe = subscribeToTasks(id, (nextTasks) => {
          if (!mounted.current) return;
          setTasks(nextTasks);
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
  }, []);

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

  return { tasks, loading, groupId, toggleCompleted, deleteTask };
}
