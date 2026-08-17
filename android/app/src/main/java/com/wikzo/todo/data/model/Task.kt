package com.wikzo.todo.data.model

import com.google.firebase.Timestamp

/**
 * Mirrors `syncGroups/{groupId}/tasks/{taskId}` as documented in /docs/data-model.md.
 * Keep this class in lockstep with that doc's "Kotlin representation" section --
 * Firestore has no server-side schema enforcement, so this is where drift would show up.
 */
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

/**
 * Firestore stores priority as a closed lowercase string enum ("high" | "medium" | "low"),
 * never a number, so the wire format stays self-describing. [firestoreValue] /
 * [fromFirestoreValue] are the single place that mapping happens.
 *
 * Declaration order (HIGH, MEDIUM, LOW) doubles as sort order -- see
 * TaskListViewModel's comparator, which sorts on `priority.ordinal`.
 */
enum class Priority {
    HIGH,
    MEDIUM,
    LOW;

    val firestoreValue: String
        get() = when (this) {
            HIGH -> "high"
            MEDIUM -> "medium"
            LOW -> "low"
        }

    companion object {
        fun fromFirestoreValue(value: String?): Priority = when (value) {
            "high" -> HIGH
            "medium" -> MEDIUM
            "low" -> LOW
            else -> MEDIUM
        }
    }
}
