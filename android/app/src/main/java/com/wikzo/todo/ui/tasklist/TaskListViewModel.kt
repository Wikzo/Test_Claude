package com.wikzo.todo.ui.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wikzo.todo.data.local.DeviceGroupStore
import com.wikzo.todo.data.model.Task
import com.wikzo.todo.data.repository.StreakRepository
import com.wikzo.todo.data.repository.SyncGroupRepository
import com.wikzo.todo.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val isSyncing: Boolean = false,
    val incompleteCount: Int = 0,
)

/**
 * One-shot "the list was just fully cleared" event -- carries the resulting streak
 * count (see [StreakRepository.recordAllTasksCleared]) so the UI can distinguish a
 * first-time clear from an ongoing streak without a second round-trip. Delivered
 * over a [Channel] rather than folded into [TaskListUiState] so it fires exactly
 * once per clear and never replays on recomposition, process death + state
 * restoration, or a config change re-collecting the state flow.
 */
data class CelebrationEvent(val streak: Int)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val syncGroupRepository: SyncGroupRepository,
    private val streakRepository: StreakRepository,
    private val deviceGroupStore: DeviceGroupStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    // Channel, not a SharedFlow/StateFlow: a celebration is a one-shot event, and
    // Channel.receiveAsFlow() only delivers each element to a single collector once,
    // which is exactly what a "list just cleared" burst should do -- a SharedFlow
    // would replay (or re-deliver to a freshly-recreated collector on rotation)
    // depending on how it's configured, and a plain state flag would fire again on
    // every recomposition that reads it until something clears it back to false.
    private val _celebrationEvents = Channel<CelebrationEvent>(Channel.BUFFERED)
    val celebrationEvents: Flow<CelebrationEvent> = _celebrationEvents.receiveAsFlow()

    private var groupId: String? = null

    // Null until the first real snapshot arrives, so the very first emission --
    // which may itself already be an empty list -- never reads as a completion
    // transition. Only set once we've actually seen at least one snapshot.
    // Total count is tracked alongside incomplete count so that deleting the
    // last remaining task -- which also drives incompleteCount to 0, but
    // isn't a "completed everything" moment -- doesn't celebrate: only a
    // same-total-count transition (a pure completed-flag flip) does.
    private var previousIncompleteCount: Int? = null
    private var previousTotalCount: Int? = null

    init {
        viewModelScope.launch {
            try {
                // Guarantees a group id is persisted before observing below --
                // otherwise a brand-new install would have nothing to collect yet.
                syncGroupRepository.ensureLocalGroup()

                // Observed reactively (rather than a one-shot ensureLocalGroup()
                // read) so that pairing -- which overwrites the persisted group id
                // while this screen's ViewModel is still alive in the nav back
                // stack -- makes the task list switch over to the newly-joined
                // group's tasks without needing the screen to be recreated.
                deviceGroupStore.groupId
                    .filterNotNull()
                    .distinctUntilChanged()
                    .flatMapLatest { id ->
                        groupId = id
                        // A newly-observed group (e.g. right after pairing) is a
                        // fresh context for the >0-to-0 transition below -- don't
                        // let a stale count from the previous group carry over.
                        previousIncompleteCount = null
                        previousTotalCount = null
                        taskRepository.observeTasks(id)
                    }
                    .collect { snapshot ->
                        val sortedTasks = snapshot.tasks.sortedWith(taskComparator)
                        val incompleteCount = sortedTasks.count { !it.completed }
                        val totalCount = sortedTasks.size
                        val previousIncomplete = previousIncompleteCount
                        val previousTotal = previousTotalCount
                        previousIncompleteCount = incompleteCount
                        previousTotalCount = totalCount

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                tasks = sortedTasks,
                                isOffline = snapshot.isFromCache,
                                isSyncing = snapshot.hasPendingWrites,
                                incompleteCount = incompleteCount,
                            )
                        }

                        val justCompletedEverything = previousIncomplete != null &&
                            previousTotal != null &&
                            previousIncomplete > 0 &&
                            incompleteCount == 0 &&
                            totalCount == previousTotal
                        if (justCompletedEverything) {
                            onAllTasksCleared()
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Something went wrong")
                }
            }
        }
    }

    fun toggleCompleted(task: Task) {
        val id = groupId ?: return
        viewModelScope.launch {
            taskRepository.setCompleted(id, task, !task.completed)
        }
    }

    /**
     * Fired once per "incomplete count just dropped from >0 to 0" -- the confetti
     * celebration and the streak bump are conceptually the same moment, so they're
     * kicked off from the same call site rather than duplicating the transition
     * detection in two places.
     */
    private fun onAllTasksCleared() {
        val id = groupId ?: return
        viewModelScope.launch {
            val streak = try {
                streakRepository.recordAllTasksCleared(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The confetti is still worth showing even if the streak write
                // failed (e.g. offline) -- 0 reads as "no streak info available"
                // rather than surfacing a write error for what is, after all, a
                // whimsical extra rather than core task data.
                0
            }
            _celebrationEvents.send(CelebrationEvent(streak))
        }
    }

    fun deleteTask(task: Task) {
        val id = groupId ?: return
        viewModelScope.launch {
            taskRepository.deleteTask(id, task.id)
        }
    }
}
