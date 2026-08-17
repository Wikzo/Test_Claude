package com.wikzo.todo.ui.tasklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Timestamp
import com.wikzo.todo.data.model.Priority
import com.wikzo.todo.data.model.Task
import com.wikzo.todo.ui.theme.PriorityHigh
import com.wikzo.todo.ui.theme.PriorityLow
import com.wikzo.todo.ui.theme.PriorityMedium
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TaskListScreen(
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TaskListScreen(
        uiState = uiState,
        onAddTask = onAddTask,
        onEditTask = onEditTask,
        onToggleCompleted = viewModel::toggleCompleted,
        onDeleteTask = viewModel::deleteTask,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListScreen(
    uiState: TaskListUiState,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onToggleCompleted: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Todo") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SyncStatusBanner(isOffline = uiState.isOffline, isSyncing = uiState.isSyncing)

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.tasks.isEmpty() -> {
                        EmptyState(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(uiState.tasks, key = { it.id }) { task ->
                                SwipeToDeleteTaskRow(
                                    task = task,
                                    onClick = { onEditTask(task) },
                                    onToggleCompleted = { onToggleCompleted(task) },
                                    onDelete = { onDeleteTask(task) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A thin status line reflecting Firestore's snapshot metadata: pending local writes
 * take priority over the plain "offline" state, since "syncing" is the more useful
 * thing to tell the user about at that moment (it also covers being online but
 * mid-flush). Shows nothing once everything is caught up and server-confirmed.
 */
@Composable
private fun SyncStatusBanner(isOffline: Boolean, isSyncing: Boolean) {
    val message = when {
        isSyncing -> "Syncing changes…"
        isOffline -> "Offline — changes saved on this device"
        else -> null
    } ?: return

    Text(
        text = message,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.List,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "Nothing to do yet",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Tap + to add your first task.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteTaskRow(
    task: Task,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        },
    ) {
        TaskRow(
            task = task,
            onClick = onClick,
            onToggleCompleted = onToggleCompleted,
        )
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.completed, onCheckedChange = { onToggleCompleted() })

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                color = if (task.completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )

            val dueDateText = formatDueDate(task.dueDate)
            if (dueDateText != null) {
                Text(
                    text = dueDateText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        PriorityDot(priority = task.priority)
    }
}

@Composable
private fun PriorityDot(priority: Priority) {
    val color = when (priority) {
        Priority.HIGH -> PriorityHigh
        Priority.MEDIUM -> PriorityMedium
        Priority.LOW -> PriorityLow
    }
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

private val dueDateFormatter = DateTimeFormatter.ofPattern("MMM d")

private fun formatDueDate(timestamp: Timestamp?): String? {
    val instant = timestamp?.toDate()?.toInstant() ?: return null
    return instant.atZone(ZoneId.systemDefault()).toLocalDate().format(dueDateFormatter)
}
