import { useState } from "react";
import { AddEditTaskForm } from "./features/tasks/AddEditTaskForm";
import { TaskList } from "./features/tasks/TaskList";
import { useTasks } from "./features/tasks/useTasks";
import { PairingPanel } from "./features/pairing/PairingPanel";
import type { Task } from "./types/task";

type FormState = { mode: "closed" } | { mode: "add" } | { mode: "edit"; task: Task };

function App() {
  const {
    tasks,
    loading,
    groupId,
    isOffline,
    isSyncing,
    toggleCompleted,
    deleteTask,
    refreshGroup,
  } = useTasks();
  const [formState, setFormState] = useState<FormState>({ mode: "closed" });
  const [pairingOpen, setPairingOpen] = useState(false);

  const remaining = tasks.filter((t) => !t.completed).length;

  const closeForm = () => setFormState({ mode: "closed" });

  const handlePaired = () => {
    refreshGroup();
  };

  const syncStatus = isSyncing
    ? "Syncing changes…"
    : isOffline
      ? "Offline — changes saved on this device"
      : null;

  return (
    <div className="min-h-full bg-slate-50">
      <div className="mx-auto flex max-w-xl flex-col gap-6 px-4 py-10 sm:py-14">
        <header className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
              Todo
            </h1>
            <p className="mt-0.5 text-sm text-slate-500">
              {loading
                ? "Loading..."
                : remaining === 0
                  ? "All caught up"
                  : `${remaining} task${remaining === 1 ? "" : "s"} remaining`}
            </p>
          </div>

          {formState.mode === "closed" && !pairingOpen && groupId && (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setPairingOpen(true)}
                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm font-medium text-slate-600 shadow-sm transition hover:bg-slate-50"
              >
                <svg
                  viewBox="0 0 20 20"
                  className="h-4 w-4"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth={1.6}
                >
                  <rect x="3" y="3" width="6" height="6" rx="1" />
                  <rect x="11" y="3" width="6" height="6" rx="1" />
                  <rect x="3" y="11" width="6" height="6" rx="1" />
                  <path d="M13 13h4M15 11v4" strokeLinecap="round" />
                </svg>
                Link a device
              </button>
              <button
                type="button"
                onClick={() => setFormState({ mode: "add" })}
                className="inline-flex items-center gap-1.5 rounded-lg bg-accent-600 px-3.5 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-accent-700"
              >
                <svg
                  viewBox="0 0 20 20"
                  className="h-4 w-4"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth={2}
                >
                  <path d="M10 4v12M4 10h12" strokeLinecap="round" />
                </svg>
                New task
              </button>
            </div>
          )}
        </header>

        {syncStatus && (
          <p className="-mt-2 rounded-md bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-500">
            {syncStatus}
          </p>
        )}

        {pairingOpen && groupId && (
          <PairingPanel
            groupId={groupId}
            onPaired={handlePaired}
            onClose={() => setPairingOpen(false)}
          />
        )}

        {formState.mode === "add" && groupId && (
          <AddEditTaskForm groupId={groupId} onDone={closeForm} onCancel={closeForm} />
        )}
        {formState.mode === "edit" && groupId && (
          <AddEditTaskForm
            groupId={groupId}
            task={formState.task}
            onDone={closeForm}
            onCancel={closeForm}
          />
        )}

        {loading ? (
          <p className="text-sm text-slate-400">Setting up your tasks...</p>
        ) : (
          <TaskList
            tasks={tasks}
            onToggleCompleted={toggleCompleted}
            onEdit={(task) => setFormState({ mode: "edit", task })}
            onDelete={deleteTask}
          />
        )}
      </div>
    </div>
  );
}

export default App;
