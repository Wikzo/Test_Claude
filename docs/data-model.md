# Data Model (canonical schema)

This is the source of truth for the Firestore schema. Android (Kotlin data classes)
and web (TypeScript interfaces) are implemented by hand against this document —
Firestore has no schema enforcement of its own, so keeping both platforms in sync
here is what prevents drift.

## Collections

```
syncGroups/{groupId}
  createdAt: Timestamp
  createdByUid: string

syncGroups/{groupId}/members/{uid}
  uid: string
  platform: "android" | "web"
  deviceName: string
  joinedAt: Timestamp
  lastSeenAt: Timestamp

syncGroups/{groupId}/tasks/{taskId}
  title: string
  notes: string | null
  completed: boolean
  completedAt: Timestamp | null
  dueDate: Timestamp | null
  priority: "high" | "medium" | "low"
  order: number
  createdAt: Timestamp        // FieldValue.serverTimestamp() on create
  updatedAt: Timestamp        // FieldValue.serverTimestamp() on every write
  createdByUid: string
  updatedByUid: string

syncGroups/{groupId}/streaks/summary        // added in Phase 5
  currentStreak: number
  lastCompletedAllAt: Timestamp | null

pairingCodes/{code}           // top-level collection; doc ID IS the code, e.g. "482913"
  groupId: string
  createdByUid: string
  createdAt: Timestamp
  expiresAt: Timestamp
  used: boolean
  usedByUid: string | null
```

## Notes

- `dueDate` and `completedAt` are nullable — a task need not have a due date, and
  is only stamped with `completedAt` when `completed` flips to `true`.
- `priority` is a closed string enum: `"high" | "medium" | "low"`. No numeric
  encoding — keeps Firestore documents self-describing.
- `order` is a plain number for manual drag-reorder within a priority/section.
  Not required to be contiguous; re-sequence lazily on drag, not on every write.
- Reminders fire exactly at `dueDate` in v1 — no configurable offset. A
  `reminderOffsetMinutes: number | null` field is the natural v1.5 extension point.
- `pairingCodes` documents are ephemeral: `expiresAt` is both checked explicitly by
  the claiming Cloud Function AND covered by a Firestore TTL policy for eventual
  cleanup (TTL deletion is not instant, so the explicit check is required regardless).
- Every task write should set `updatedByUid` and bump `updatedAt` — this is what lets
  a future "who changed this" affordance exist without redesigning the schema later,
  even though v1 doesn't surface it in UI.

## Kotlin representation (Android)

```kotlin
data class Task(
    val id: String = "",
    val title: String = "",
    val notes: String? = null,
    val completed: Boolean = false,
    val completedAt: Timestamp? = null,
    val dueDate: Timestamp? = null,
    val priority: Priority = Priority.MEDIUM,
    val order: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val createdByUid: String = "",
    val updatedByUid: String = "",
)

enum class Priority { HIGH, MEDIUM, LOW }
```

## TypeScript representation (web + functions)

```typescript
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

export interface StreakSummary {
  currentStreak: number;
  lastCompletedAllAt: Timestamp | null;
}
```
