import type { Task } from "../../types/task";
import {
  PRIORITY_BADGE_CLASS,
  PRIORITY_LABEL,
  formatDueDate,
} from "./taskUtils";

interface TaskItemProps {
  task: Task;
  onToggleCompleted: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (taskId: string) => void;
}

export function TaskItem({
  task,
  onToggleCompleted,
  onEdit,
  onDelete,
}: TaskItemProps) {
  const dueLabel = formatDueDate(task.dueDate);

  return (
    <li className="group flex items-start gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm transition hover:border-slate-300 hover:shadow">
      <button
        type="button"
        role="checkbox"
        aria-checked={task.completed}
        onClick={() => onToggleCompleted(task)}
        className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2 transition ${
          task.completed
            ? "border-accent-500 bg-accent-500 text-white"
            : "border-slate-300 hover:border-accent-400"
        }`}
      >
        {task.completed && (
          <svg
            viewBox="0 0 12 12"
            className="h-3 w-3"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path d="M2 6l2.5 2.5L10 3" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        )}
      </button>

      <button
        type="button"
        onClick={() => onEdit(task)}
        className="min-w-0 flex-1 text-left"
      >
        <p
          className={`truncate text-sm font-medium ${
            task.completed ? "text-slate-400 line-through" : "text-slate-800"
          }`}
        >
          {task.title}
        </p>
        {task.notes && (
          <p className="mt-0.5 truncate text-xs text-slate-500">{task.notes}</p>
        )}
        <div className="mt-1.5 flex flex-wrap items-center gap-2">
          <span
            className={`inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium ring-1 ring-inset ${PRIORITY_BADGE_CLASS[task.priority]}`}
          >
            {PRIORITY_LABEL[task.priority]}
          </span>
          {dueLabel && (
            <span className="inline-flex items-center gap-1 text-xs text-slate-500">
              <svg
                viewBox="0 0 20 20"
                className="h-3.5 w-3.5"
                fill="none"
                stroke="currentColor"
                strokeWidth={1.5}
              >
                <rect x="3" y="4" width="14" height="13" rx="2" />
                <path d="M3 8h14M7 2v4M13 2v4" strokeLinecap="round" />
              </svg>
              {dueLabel}
            </span>
          )}
        </div>
      </button>

      <button
        type="button"
        onClick={() => onDelete(task.id)}
        aria-label={`Delete ${task.title}`}
        className="shrink-0 rounded-lg p-1.5 text-slate-300 opacity-0 transition hover:bg-rose-50 hover:text-rose-500 group-hover:opacity-100"
      >
        <svg
          viewBox="0 0 20 20"
          className="h-4 w-4"
          fill="none"
          stroke="currentColor"
          strokeWidth={1.6}
        >
          <path d="M4 6h12M8 6V4h4v2m-7 0 .7 10.1A2 2 0 0 0 7.7 18h4.6a2 2 0 0 0 2-1.9L15 6" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
    </li>
  );
}
