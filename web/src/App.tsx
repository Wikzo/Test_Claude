import { useEffect, useRef, useState } from "react";
import confetti from "canvas-confetti";
import { AddEditTaskForm } from "./features/tasks/AddEditTaskForm";
import { TaskList } from "./features/tasks/TaskList";
import { useTasks } from "./features/tasks/useTasks";
import { PairingPanel } from "./features/pairing/PairingPanel";
import { Mascot } from "./features/mascot/Mascot";
import type { Task } from "./types/task";

type FormState = { mode: "closed" } | { mode: "add" } | { mode: "edit"; task: Task };

// How long the mascot's "celebrating" state (and its streak badge) stays up
// after the list is fully cleared, before settling back to idle/happy.
const CELEBRATION_DURATION_MS = 2800;

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
    remaining,
    completedCount,
    streak,
    celebrationToken,
  } = useTasks();
  const [formState, setFormState] = useState<FormState>({ mode: "closed" });
  const [pairingOpen, setPairingOpen] = useState(false);
  const [celebrating, setCelebrating] = useState(false);

  // celebrationToken is a one-shot signal: skip the effect's initial run (it
  // fires once on mount with the token's starting value) so confetti only
  // ever plays for a genuine "just cleared the list" event, never on load.
  const isFirstCelebrationRender = useRef(true);
  useEffect(() => {
    if (isFirstCelebrationRender.current) {
      isFirstCelebrationRender.current = false;
      return;
    }

    confetti({
      particleCount: 90,
      spread: 70,
      startVelocity: 38,
      origin: { y: 0.3 },
      ticks: 180,
    });

    setCelebrating(true);
    const timeout = setTimeout(() => setCelebrating(false), CELEBRATION_DURATION_MS);
    return () => clearTimeout(timeout);
  }, [celebrationToken]);

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
          <div className="flex items-center gap-3">
            <Mascot
              remaining={remaining}
              completedCount={completedCount}
              celebrating={celebrating}
              streak={streak}
            />
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
