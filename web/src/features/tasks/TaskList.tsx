import type { Task } from "../../types/task";
import { TaskItem } from "./TaskItem";
import { sortTasks } from "./taskUtils";

interface TaskListProps {
  tasks: Task[];
  onToggleCompleted: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (taskId: string) => void;
}

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

  const sorted = sortTasks(tasks);

  return (
    <ul className="flex flex-col gap-2">
      {sorted.map((task) => (
        <TaskItem
          key={task.id}
          task={task}
          onToggleCompleted={onToggleCompleted}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      ))}
    </ul>
  );
}
