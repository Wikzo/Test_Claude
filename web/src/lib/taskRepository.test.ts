import { describe, expect, it } from "vitest";
import { Timestamp } from "firebase/firestore";
import { mapDocToTask } from "./taskRepository";

describe("mapDocToTask", () => {
  it("maps a fully-populated document, matching /docs/data-model.md field-for-field", () => {
    const dueDate = Timestamp.fromDate(new Date("2026-01-01T09:00:00Z"));
    const createdAt = Timestamp.fromDate(new Date("2025-12-01T00:00:00Z"));

    const task = mapDocToTask("task-1", {
      title: "Buy milk",
      notes: "Whole, not skim",
      completed: true,
      completedAt: createdAt,
      dueDate,
      priority: "high",
      order: 2,
      createdAt,
      updatedAt: createdAt,
      createdByUid: "uid-a",
      updatedByUid: "uid-b",
    });

    expect(task).toEqual({
      id: "task-1",
      title: "Buy milk",
      notes: "Whole, not skim",
      completed: true,
      completedAt: createdAt,
      dueDate,
      priority: "high",
      order: 2,
      createdAt,
      updatedAt: createdAt,
      createdByUid: "uid-a",
      updatedByUid: "uid-b",
    });
  });

  it("fills in schema defaults for a minimal/legacy document missing optional fields", () => {
    const task = mapDocToTask("task-2", { title: "Untitled-ish" });

    expect(task).toEqual({
      id: "task-2",
      title: "Untitled-ish",
      notes: null,
      completed: false,
      completedAt: null,
      dueDate: null,
      priority: "medium",
      order: 0,
      createdAt: null,
      updatedAt: null,
      createdByUid: "",
      updatedByUid: "",
    });
  });

  it("preserves an explicit null notes/dueDate rather than coercing to a default", () => {
    const task = mapDocToTask("task-3", {
      title: "No notes, no due date",
      notes: null,
      dueDate: null,
      priority: "low",
    });

    expect(task.notes).toBeNull();
    expect(task.dueDate).toBeNull();
    expect(task.priority).toBe("low");
  });
});
