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

/** True for an incomplete task whose due date has passed. */
export function isOverdue(task: Task): boolean {
  if (task.completed || !task.dueDate) return false;
  return task.dueDate.toMillis() < Date.now();
}

export interface TaskGroup {
  key: string;
  label: string;
  tasks: Task[];
}

/**
 * Buckets tasks into date-aware sections for display -- this is the web
 * client's stand-in for push notifications (out of scope for v1; Android
 * owns actual reminders): due-date urgency is surfaced visually instead.
 * Each bucket is internally sorted the same way as the flat list
 * (priority, then due date). Empty buckets are omitted.
 */
export function groupTasksForDisplay(tasks: Task[]): TaskGroup[] {
  const completed = tasks.filter((t) => t.completed);
  const incomplete = tasks.filter((t) => !t.completed);

  const now = Date.now();
  const startOfToday = new Date();
  startOfToday.setHours(0, 0, 0, 0);
  const startOfTomorrow = startOfToday.getTime() + 24 * 60 * 60 * 1000;

  const overdue: Task[] = [];
  const today: Task[] = [];
  const upcoming: Task[] = [];
  const noDueDate: Task[] = [];

  for (const task of incomplete) {
    const due = task.dueDate?.toMillis();
    if (due == null) {
      noDueDate.push(task);
    } else if (due < now) {
      overdue.push(task);
    } else if (due < startOfTomorrow) {
      today.push(task);
    } else {
      upcoming.push(task);
    }
  }

  const groups: TaskGroup[] = [];
  if (overdue.length) groups.push({ key: "overdue", label: "Overdue", tasks: sortTasks(overdue) });
  if (today.length) groups.push({ key: "today", label: "Today", tasks: sortTasks(today) });
  if (upcoming.length) groups.push({ key: "upcoming", label: "Upcoming", tasks: sortTasks(upcoming) });
  if (noDueDate.length) groups.push({ key: "no-due-date", label: "No due date", tasks: sortTasks(noDueDate) });
  if (completed.length) groups.push({ key: "completed", label: "Completed", tasks: sortTasks(completed) });

  return groups;
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
