import type { Timestamp } from "firebase/firestore";
import type { Priority, Task } from "../../types/task";

const PRIORITY_RANK: Record<Priority, number> = {
  high: 0,
  medium: 1,
  low: 2,
};

/**
 * Sorts tasks for display: incomplete tasks first (by priority, then by
 * due date, soonest first, with no-due-date tasks last), followed by
 * completed tasks (most recently completed first).
 */
export function sortTasks(tasks: Task[]): Task[] {
  return [...tasks].sort((a, b) => {
    if (a.completed !== b.completed) {
      return a.completed ? 1 : -1;
    }
    if (a.completed && b.completed) {
      const aTime = a.completedAt?.toMillis() ?? 0;
      const bTime = b.completedAt?.toMillis() ?? 0;
      return bTime - aTime;
    }

    const priorityDiff = PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority];
    if (priorityDiff !== 0) return priorityDiff;

    const aDue = a.dueDate?.toMillis() ?? Number.POSITIVE_INFINITY;
    const bDue = b.dueDate?.toMillis() ?? Number.POSITIVE_INFINITY;
    if (aDue !== bDue) return aDue - bDue;

    return a.order - b.order;
  });
}

/** Formats a due-date Timestamp as "Today", "Tomorrow", or e.g. "Aug 20". */
export function formatDueDate(dueDate: Timestamp | null): string | null {
  if (!dueDate) return null;

  const date = dueDate.toDate();
  const now = new Date();

  const startOfDay = (d: Date) =>
    new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();

  const diffDays = Math.round(
    (startOfDay(date) - startOfDay(now)) / (1000 * 60 * 60 * 24),
  );

  const time = date.toLocaleTimeString(undefined, {
    hour: "numeric",
    minute: "2-digit",
  });

  if (diffDays === 0) return `Today, ${time}`;
  if (diffDays === 1) return `Tomorrow, ${time}`;
  if (diffDays === -1) return `Yesterday, ${time}`;

  const dateLabel = date.toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: date.getFullYear() !== now.getFullYear() ? "numeric" : undefined,
  });
  return `${dateLabel}, ${time}`;
}

export const PRIORITY_LABEL: Record<Priority, string> = {
  high: "High",
  medium: "Medium",
  low: "Low",
};

export const PRIORITY_DOT_CLASS: Record<Priority, string> = {
  high: "bg-rose-500",
  medium: "bg-amber-500",
  low: "bg-emerald-500",
};

export const PRIORITY_BADGE_CLASS: Record<Priority, string> = {
  high: "bg-rose-50 text-rose-700 ring-rose-200",
  medium: "bg-amber-50 text-amber-700 ring-amber-200",
  low: "bg-emerald-50 text-emerald-700 ring-emerald-200",
};
