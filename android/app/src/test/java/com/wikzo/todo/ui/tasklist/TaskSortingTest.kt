package com.wikzo.todo.ui.tasklist

import com.google.firebase.Timestamp
import com.wikzo.todo.data.model.Priority
import com.wikzo.todo.data.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test

private var nextId = 0

private fun task(
    completed: Boolean = false,
    priority: Priority = Priority.MEDIUM,
    dueDateSeconds: Long? = null,
): Task {
    nextId += 1
    return Task(
        id = "task-$nextId",
        title = "Task $nextId",
        completed = completed,
        priority = priority,
        dueDate = dueDateSeconds?.let { Timestamp(it, 0) },
    )
}

class TaskSortingTest {

    @Test
    fun `incomplete tasks sort before completed ones regardless of priority`() {
        val done = task(completed = true, priority = Priority.HIGH)
        val pending = task(completed = false, priority = Priority.LOW)

        val sorted = listOf(done, pending).sortedWith(taskComparator)

        assertEquals(listOf(pending.id, done.id), sorted.map { it.id })
    }

    @Test
    fun `incomplete tasks sort by priority, high before medium before low`() {
        val low = task(priority = Priority.LOW)
        val high = task(priority = Priority.HIGH)
        val medium = task(priority = Priority.MEDIUM)

        val sorted = listOf(low, high, medium).sortedWith(taskComparator)

        assertEquals(listOf(high.id, medium.id, low.id), sorted.map { it.id })
    }

    @Test
    fun `within the same priority, soonest due date sorts first and undated tasks sort last`() {
        val noDue = task(priority = Priority.HIGH, dueDateSeconds = null)
        val later = task(priority = Priority.HIGH, dueDateSeconds = 2_000_000L)
        val sooner = task(priority = Priority.HIGH, dueDateSeconds = 1_000_000L)

        val sorted = listOf(noDue, later, sooner).sortedWith(taskComparator)

        assertEquals(listOf(sooner.id, later.id, noDue.id), sorted.map { it.id })
    }
}
