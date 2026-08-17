import type { Timestamp } from "firebase/firestore";

/**
 * Mirrors the TypeScript representation in /docs/data-model.md exactly.
 * Do not diverge from that document without updating it first — it is the
 * canonical schema shared with the Android client.
 */
export type Priority = "high" | "medium" | "low";

export interface Task {
  id: string;
  title: string;
  notes: string | null;
  completed: boolean;
  completedAt: Timestamp | null;
  dueDate: Timestamp | null;
  priority: Priority;
  order: number;
  createdAt: Timestamp | null;
  updatedAt: Timestamp | null;
  createdByUid: string;
  updatedByUid: string;
}

/**
 * Shape of a new task as created from the UI, before Firestore assigns
 * server-generated fields (id, createdAt, updatedAt, createdByUid, updatedByUid).
 */
export interface NewTaskInput {
  title: string;
  notes: string | null;
  dueDate: Date | null;
  priority: Priority;
  order: number;
}

/**
 * Partial patch applied to an existing task. updatedAt/updatedByUid are
 * always stamped by the repository layer, not the caller.
 */
export interface TaskPatch {
  title?: string;
  notes?: string | null;
  dueDate?: Date | null;
  priority?: Priority;
  completed?: boolean;
}
