import type { Task } from "../../types/task";
import { TaskItem } from "./TaskItem";
import { groupTasksForDisplay } from "./taskUtils";

interface TaskListProps {
  tasks: Task[];
  onToggleCompleted: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (taskId: string) => void;
}

const GROUP_LABEL_CLASS: Record<string, string> = {
  overdue: "text-rose-600",
};

export function TaskList({
  tasks,
  onToggleCompleted,
  onEdit,
  onDelete,
}: TaskListProps) {
  if (tasks.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 bg-white/60 px-6 py-12 text-center">
        <p className="text-sm text-slate-500">
          No tasks yet. Add your first one to get started.
        </p>
      </div>
    );
  }

  const groups = groupTasksForDisplay(tasks);

  return (
    <div className="flex flex-col gap-5">
      {groups.map((group) => (
        <section key={group.key} className="flex flex-col gap-2">
          <h2
            className={`px-1 text-xs font-semibold uppercase tracking-wide ${
              GROUP_LABEL_CLASS[group.key] ?? "text-slate-400"
            }`}
          >
            {group.label}
          </h2>
          <ul className="flex flex-col gap-2">
            {group.tasks.map((task) => (
              <TaskItem
                key={task.id}
                task={task}
                onToggleCompleted={onToggleCompleted}
                onEdit={onEdit}
                onDelete={onDelete}
              />
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
