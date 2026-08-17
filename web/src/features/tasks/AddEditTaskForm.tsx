import { useState, type FormEvent } from "react";
import { addTask, updateTask } from "../../lib/taskRepository";
import type { Priority, Task } from "../../types/task";
import { PRIORITY_LABEL } from "./taskUtils";

interface AddEditTaskFormProps {
  groupId: string;
  task?: Task;
  onDone: () => void;
  onCancel: () => void;
}

function toDatetimeLocalValue(date: Date | null): string {
  if (!date) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

const PRIORITIES: Priority[] = ["high", "medium", "low"];

export function AddEditTaskForm({
  groupId,
  task,
  onDone,
  onCancel,
}: AddEditTaskFormProps) {
  const isEditing = Boolean(task);

  const [title, setTitle] = useState(task?.title ?? "");
  const [notes, setNotes] = useState(task?.notes ?? "");
  const [dueDateValue, setDueDateValue] = useState(
    toDatetimeLocalValue(task?.dueDate?.toDate() ?? null),
  );
  const [priority, setPriority] = useState<Priority>(task?.priority ?? "medium");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const trimmedTitle = title.trim();
    if (!trimmedTitle) {
      setError("Title is required.");
      return;
    }

    setSubmitting(true);
    setError(null);

    const dueDate = dueDateValue ? new Date(dueDateValue) : null;
    const trimmedNotes = notes.trim() || null;

    try {
      if (isEditing && task) {
        await updateTask(groupId, task.id, {
          title: trimmedTitle,
          notes: trimmedNotes,
          dueDate,
          priority,
        });
      } else {
        await addTask(groupId, {
          title: trimmedTitle,
          notes: trimmedNotes,
          dueDate,
          priority,
          order: Date.now(),
        });
      }
      onDone();
    } catch (err) {
      console.error("Failed to save task", err);
      setError("Something went wrong saving this task. Try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
    >
      <div>
        <label
          htmlFor="task-title"
          className="mb-1 block text-xs font-medium text-slate-600"
        >
          Title
        </label>
        <input
          id="task-title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="What needs doing?"
          autoFocus
          className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 outline-none focus:border-accent-500 focus:ring-2 focus:ring-accent-100"
        />
      </div>

      <div>
        <label
          htmlFor="task-notes"
          className="mb-1 block text-xs font-medium text-slate-600"
        >
          Notes
        </label>
        <textarea
          id="task-notes"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Optional details..."
          rows={2}
          className="w-full resize-none rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 outline-none focus:border-accent-500 focus:ring-2 focus:ring-accent-100"
        />
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
        <div className="flex-1">
          <label
            htmlFor="task-due"
            className="mb-1 block text-xs font-medium text-slate-600"
          >
            Due date
          </label>
          <input
            id="task-due"
            type="datetime-local"
            value={dueDateValue}
            onChange={(e) => setDueDateValue(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 outline-none focus:border-accent-500 focus:ring-2 focus:ring-accent-100"
          />
        </div>

        <div className="flex-1">
          <span className="mb-1 block text-xs font-medium text-slate-600">
            Priority
          </span>
          <div className="flex overflow-hidden rounded-lg border border-slate-300">
            {PRIORITIES.map((p) => (
              <button
                key={p}
                type="button"
                onClick={() => setPriority(p)}
                className={`flex-1 px-3 py-2 text-sm font-medium transition ${
                  priority === p
                    ? "bg-accent-500 text-white"
                    : "bg-white text-slate-600 hover:bg-slate-50"
                } ${p !== "low" ? "border-r border-slate-300" : ""}`}
              >
                {PRIORITY_LABEL[p]}
              </button>
            ))}
          </div>
        </div>
      </div>

      {error && <p className="text-sm text-rose-600">{error}</p>}

      <div className="flex justify-end gap-2 pt-1">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={submitting}
          className="rounded-lg bg-accent-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-accent-700 disabled:opacity-60"
        >
          {isEditing ? "Save changes" : "Add task"}
        </button>
      </div>
    </form>
  );
}
