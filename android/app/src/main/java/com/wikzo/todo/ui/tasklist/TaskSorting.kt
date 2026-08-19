package com.wikzo.todo.ui.tasklist

import com.wikzo.todo.data.model.Task

/**
 * Incomplete tasks first, then by priority (HIGH, MEDIUM, LOW -- the enum's
 * declaration order), then by due date (soonest first, undated tasks last).
 * Completed tasks always sort after incomplete ones regardless of the above.
 *
 * Pulled out of TaskListViewModel into its own file (rather than kept
 * file-private there) so it's directly unit-testable -- see
 * TaskSortingTest -- without needing a ViewModel/Hilt/Firestore test setup.
 */
val taskComparator: Comparator<Task> = compareBy(
    { it.completed },
    { it.priority.ordinal },
    { it.dueDate?.seconds ?: Long.MAX_VALUE },
)
