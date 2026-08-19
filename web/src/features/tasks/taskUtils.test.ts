import { describe, expect, it } from "vitest";
import { Timestamp } from "firebase/firestore";
import type { Task } from "../../types/task";
import { groupTasksForDisplay, isOverdue, sortTasks } from "./taskUtils";

let nextId = 0;

function makeTask(overrides: Partial<Task> = {}): Task {
  nextId += 1;
  return {
    id: `task-${nextId}`,
    title: `Task ${nextId}`,
    notes: null,
    completed: false,
    completedAt: null,
    dueDate: null,
    priority: "medium",
    order: 0,
    createdAt: null,
    updatedAt: null,
    createdByUid: "uid",
    updatedByUid: "uid",
    ...overrides,
  };
}

function ts(msFromNow: number): Timestamp {
  return Timestamp.fromMillis(Date.now() + msFromNow);
}

const DAY = 24 * 60 * 60 * 1000;

describe("sortTasks", () => {
  it("puts incomplete tasks before completed ones regardless of priority/due date", () => {
    const done = makeTask({ completed: true, priority: "high" });
    const pending = makeTask({ completed: false, priority: "low" });

    expect(sortTasks([done, pending]).map((t) => t.id)).toEqual([
      pending.id,
      done.id,
    ]);
  });

  it("orders incomplete tasks by priority (high, medium, low)", () => {
    const low = makeTask({ priority: "low" });
    const high = makeTask({ priority: "high" });
    const medium = makeTask({ priority: "medium" });

    expect(sortTasks([low, high, medium]).map((t) => t.id)).toEqual([
      high.id,
      medium.id,
      low.id,
    ]);
  });

  it("within the same priority, sorts by soonest due date first and undated last", () => {
    const noDue = makeTask({ priority: "high", dueDate: null });
    const later = makeTask({ priority: "high", dueDate: ts(2 * DAY) });
    const sooner = makeTask({ priority: "high", dueDate: ts(1 * DAY) });

    expect(sortTasks([noDue, later, sooner]).map((t) => t.id)).toEqual([
      sooner.id,
      later.id,
      noDue.id,
    ]);
  });

  it("orders completed tasks most-recently-completed first", () => {
    const older = makeTask({ completed: true, completedAt: ts(-2 * DAY) });
    const newer = makeTask({ completed: true, completedAt: ts(-1 * DAY) });

    expect(sortTasks([older, newer]).map((t) => t.id)).toEqual([
      newer.id,
      older.id,
    ]);
  });
});

describe("isOverdue", () => {
  it("is true for an incomplete task with a due date in the past", () => {
    expect(isOverdue(makeTask({ dueDate: ts(-DAY) }))).toBe(true);
  });

  it("is false for a completed task even with a past due date", () => {
    expect(isOverdue(makeTask({ completed: true, dueDate: ts(-DAY) }))).toBe(
      false,
    );
  });

  it("is false for an incomplete task with no due date", () => {
    expect(isOverdue(makeTask({ dueDate: null }))).toBe(false);
  });

  it("is false for an incomplete task due in the future", () => {
    expect(isOverdue(makeTask({ dueDate: ts(DAY) }))).toBe(false);
  });
});

describe("groupTasksForDisplay", () => {
  it("buckets tasks into overdue/today/upcoming/no-due-date/completed, omitting empty buckets", () => {
    const overdue = makeTask({ dueDate: ts(-2 * DAY) });
    const today = makeTask({ dueDate: ts(60 * 1000) });
    const upcoming = makeTask({ dueDate: ts(5 * DAY) });
    const noDueDate = makeTask({ dueDate: null });
    const completed = makeTask({ completed: true, dueDate: ts(-DAY) });

    const groups = groupTasksForDisplay([
      completed,
      noDueDate,
      upcoming,
      today,
      overdue,
    ]);

    expect(groups.map((g) => g.key)).toEqual([
      "overdue",
      "today",
      "upcoming",
      "no-due-date",
      "completed",
    ]);
    expect(groups.find((g) => g.key === "overdue")?.tasks.map((t) => t.id)).toEqual([
      overdue.id,
    ]);
    expect(groups.find((g) => g.key === "completed")?.tasks.map((t) => t.id)).toEqual([
      completed.id,
    ]);
  });

  it("omits a bucket entirely when nothing falls into it", () => {
    const onlyUpcoming = makeTask({ dueDate: ts(3 * DAY) });

    const groups = groupTasksForDisplay([onlyUpcoming]);

    expect(groups).toHaveLength(1);
    expect(groups[0].key).toBe("upcoming");
  });
});
