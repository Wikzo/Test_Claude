package com.wikzo.todo.ui.tasklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Timestamp
import com.wikzo.todo.data.model.Priority
import com.wikzo.todo.data.model.Task
import com.wikzo.todo.ui.mascot.MascotMood
import com.wikzo.todo.ui.mascot.MascotView
import com.wikzo.todo.ui.theme.PriorityHigh
import com.wikzo.todo.ui.theme.PriorityLow
import com.wikzo.todo.ui.theme.PriorityMedium
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

/** How long the confetti burst + mascot's CELEBRATING mood stay on screen. */
private const val CELEBRATION_DURATION_MILLIS = 2800L

@Composable
fun TaskListScreen(
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onShowMyCode: () -> Unit,
    onEnterCode: () -> Unit,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Theme-derived so the burst matches the app's own palette in both light and
    // dark mode, rather than a hardcoded set of party colors.
    val colorScheme = MaterialTheme.colorScheme
    val confettiColors = remember(colorScheme) {
        listOf(colorScheme.primary, colorScheme.secondary, PriorityHigh, PriorityMedium, PriorityLow)
            .map { it.toArgb() }
    }

    var isCelebrating by remember { mutableStateOf(false) }
    var celebrationStreak by remember { mutableStateOf(0) }
    var confettiParties by remember { mutableStateOf(emptyList<Party>()) }
    // KonfettiView reads its `parties` list exactly once, inside a
    // LaunchedEffect(Unit) -- it does NOT restart on a `parties` change. So a
    // second celebration in the same screen session needs a brand new KonfettiView
    // instance, not the same instance handed a new list; bumping this id and
    // key()-ing the call below (further down in the stateless overload) is what
    // forces that.
    var celebrationBurstId by remember { mutableStateOf(0) }

    LaunchedEffect(viewModel, confettiColors) {
        viewModel.celebrationEvents.collect { event ->
            celebrationStreak = event.streak
            confettiParties = celebrationParties(confettiColors)
            celebrationBurstId++
            isCelebrating = true
            delay(CELEBRATION_DURATION_MILLIS)
            isCelebrating = false
        }
    }

    TaskListScreen(
        uiState = uiState,
        isCelebrating = isCelebrating,
        celebrationStreak = celebrationStreak,
        celebrationBurstId = celebrationBurstId,
        confettiParties = confettiParties,
        onAddTask = onAddTask,
        onEditTask = onEditTask,
        onShowMyCode = onShowMyCode,
        onEnterCode = onEnterCode,
        onToggleCompleted = viewModel::toggleCompleted,
        onDeleteTask = viewModel::deleteTask,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListScreen(
    uiState: TaskListUiState,
    isCelebrating: Boolean,
    celebrationStreak: Int,
    celebrationBurstId: Int,
    confettiParties: List<Party>,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onShowMyCode: () -> Unit,
    onEnterCode: () -> Unit,
    onToggleCompleted: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
) {
    // HAPPY once something's been checked off but the list isn't fully cleared;
    // CELEBRATING takes over for a few seconds right after it is (see
    // TaskListViewModel's >0-to-0 transition detection); IDLE otherwise.
    val mascotMood = when {
        isCelebrating -> MascotMood.CELEBRATING
        uiState.tasks.isNotEmpty() && uiState.incompleteCount in 1 until uiState.tasks.size -> MascotMood.HAPPY
        else -> MascotMood.IDLE
    }
    val celebrationMessage = when {
        !isCelebrating -> null
        celebrationStreak >= 2 -> "$celebrationStreak-day streak!"
        else -> "All done!"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todo") },
                actions = {
                    LinkDeviceAction(onShowMyCode = onShowMyCode, onEnterCode = onEnterCode)
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MascotHeader(mood = mascotMood, celebrationMessage = celebrationMessage)

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

            // Only composed while celebrating, keyed on celebrationBurstId so each
            // new celebration gets a fresh KonfettiView instance -- see the comment
            // on celebrationBurstId in the stateful overload above for why that
            // matters (KonfettiView only reads `parties` on its own first
            // composition, not on every recomposition).
            if (isCelebrating) {
                key(celebrationBurstId) {
                    KonfettiView(modifier = Modifier.fillMaxSize(), parties = confettiParties)
                }
            }
        }
    }
}

/**
 * A brief, centered burst -- "brief" per the product spec, so a single short-lived
 * emitter rather than a sustained rain of confetti.
 */
private fun celebrationParties(colors: List<Int>): List<Party> = listOf(
    Party(
        speed = 10f,
        maxSpeed = 35f,
        damping = 0.9f,
        spread = 360,
        colors = colors,
        // Positional args here (rather than named) since Konfetti's public API
        // doesn't guarantee the constructor's parameter names, only their order.
        emitter = Emitter(150L, TimeUnit.MILLISECONDS).max(90),
        position = Position.Relative(0.5, 0.25),
    ),
)

/**
 * The mascot, plus a short-lived celebration caption next to it. Lives just under
 * the top bar rather than floating over the list, so it never covers a task row --
 * small enough to stay out of the way of the minimalist layout.
 */
@Composable
private fun MascotHeader(mood: MascotMood, celebrationMessage: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MascotView(mood = mood, size = 48.dp)
        AnimatedVisibility(visible = celebrationMessage != null) {
            Text(
                text = celebrationMessage.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 12.dp),
            )
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

/**
 * A single top-bar entry point for pairing, covering both directions ("show my
 * code" to invite another device, "enter a code" to join one) behind one small
 * menu rather than two separate top-bar icons.
 */
@Composable
private fun LinkDeviceAction(onShowMyCode: () -> Unit, onEnterCode: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.Devices, contentDescription = "Link a device")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Show my code") },
            onClick = {
                expanded = false
                onShowMyCode()
            },
        )
        DropdownMenuItem(
            text = { Text("Enter a code") },
            onClick = {
                expanded = false
                onEnterCode()
            },
        )
    }
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
    val scope = rememberCoroutineScope()
    // Animatable rather than animateFloatAsState -- a toggle should always pop up
    // then settle, even if it's tapped again mid-animation, which is naturally
    // expressed as "launch a new two-step animation" rather than as a function of
    // a single target value.
    val checkboxScale = remember { Animatable(1f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.completed,
            onCheckedChange = {
                onToggleCompleted()
                scope.launch {
                    checkboxScale.snapTo(1f)
                    checkboxScale.animateTo(
                        targetValue = 1.3f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh,
                        ),
                    )
                    checkboxScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
            },
            modifier = Modifier.scale(checkboxScale.value),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            AnimatedTaskTitle(title = task.title, completed = task.completed)

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

/**
 * The task title, animating into its completed look (dimmed color + a line drawn
 * through it) rather than snapping there instantly. `TextDecoration.LineThrough`
 * itself can't be animated -- it's a discrete enum, not an interpolable value --
 * so the strike-through is hand-drawn as a line whose width animates in
 * ([strikeProgress] 0f -> 1f) over [Modifier.drawWithContent], while the text
 * color cross-fades separately over the same short (quarter-second) window.
 */
@Composable
private fun AnimatedTaskTitle(title: String, completed: Boolean) {
    val strikeProgress by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "title-strike-through",
    )
    val textColor by animateColorAsState(
        targetValue = if (completed) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 260),
        label = "title-color",
    )

    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = textColor,
        modifier = Modifier.drawWithContent {
            drawContent()
            if (strikeProgress > 0f) {
                val y = size.height / 2f
                drawLine(
                    color = textColor,
                    start = Offset(0f, y),
                    end = Offset(size.width * strikeProgress, y),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        },
    )
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
