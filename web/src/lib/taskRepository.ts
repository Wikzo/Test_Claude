import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  Timestamp,
  updateDoc,
  type Unsubscribe,
} from "firebase/firestore";
import { auth, db } from "./firebaseConfig";
import type { NewTaskInput, Task, TaskPatch } from "../types/task";

function tasksCollection(groupId: string) {
  return collection(db, "syncGroups", groupId, "tasks");
}

function taskDoc(groupId: string, taskId: string) {
  return doc(db, "syncGroups", groupId, "tasks", taskId);
}

export interface TasksSnapshot {
  tasks: Task[];
  isFromCache: boolean;
  hasPendingWrites: boolean;
}

/**
 * Subscribes to live updates of every task in the group, including sync
 * metadata (so the UI can show an "offline" / "syncing" indicator rather than
 * offline-first behavior being an invisible cache). Calls `callback` on every
 * change, including local writes not yet confirmed by the server. Returns an
 * Unsubscribe function — call it on cleanup (e.g. in a useEffect teardown).
 */
export function subscribeToTasks(
  groupId: string,
  callback: (snapshot: TasksSnapshot) => void,
): Unsubscribe {
  const q = query(tasksCollection(groupId), orderBy("createdAt", "asc"));
  return onSnapshot(q, { includeMetadataChanges: true }, (snapshot) => {
    const tasks: Task[] = snapshot.docs.map((docSnap) => {
      const data = docSnap.data();
      return {
        id: docSnap.id,
        title: data.title,
        notes: data.notes ?? null,
        completed: data.completed ?? false,
        completedAt: data.completedAt ?? null,
        dueDate: data.dueDate ?? null,
        priority: data.priority ?? "medium",
        order: data.order ?? 0,
        createdAt: data.createdAt ?? null,
        updatedAt: data.updatedAt ?? null,
        createdByUid: data.createdByUid ?? "",
        updatedByUid: data.updatedByUid ?? "",
      };
    });
    callback({
      tasks,
      isFromCache: snapshot.metadata.fromCache,
      hasPendingWrites: snapshot.metadata.hasPendingWrites,
    });
  });
}

/** Creates a new task in the group. Returns the new task's id. */
export async function addTask(
  groupId: string,
  input: NewTaskInput,
): Promise<string> {
  const uid = auth.currentUser?.uid ?? "";
  const docRef = await addDoc(tasksCollection(groupId), {
    title: input.title,
    notes: input.notes,
    completed: false,
    completedAt: null,
    dueDate: input.dueDate ? Timestamp.fromDate(input.dueDate) : null,
    priority: input.priority,
    order: input.order,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    createdByUid: uid,
    updatedByUid: uid,
  });
  return docRef.id;
}

/** Applies a partial update to an existing task, stamping updatedAt/updatedByUid. */
export async function updateTask(
  groupId: string,
  taskId: string,
  patch: TaskPatch,
): Promise<void> {
  const uid = auth.currentUser?.uid ?? "";
  const update: Record<string, unknown> = {
    updatedAt: serverTimestamp(),
    updatedByUid: uid,
  };

  if (patch.title !== undefined) update.title = patch.title;
  if (patch.notes !== undefined) update.notes = patch.notes;
  if (patch.priority !== undefined) update.priority = patch.priority;
  if (patch.dueDate !== undefined) {
    update.dueDate = patch.dueDate ? Timestamp.fromDate(patch.dueDate) : null;
  }
  if (patch.completed !== undefined) {
    update.completed = patch.completed;
    update.completedAt = patch.completed ? serverTimestamp() : null;
  }

  await updateDoc(taskDoc(groupId, taskId), update);
}

/** Deletes a task permanently. */
export async function deleteTask(
  groupId: string,
  taskId: string,
): Promise<void> {
  await deleteDoc(taskDoc(groupId, taskId));
}

/** Convenience wrapper for toggling completion, stamping completedAt appropriately. */
export async function setCompleted(
  groupId: string,
  taskId: string,
  completed: boolean,
): Promise<void> {
  await updateTask(groupId, taskId, { completed });
}
