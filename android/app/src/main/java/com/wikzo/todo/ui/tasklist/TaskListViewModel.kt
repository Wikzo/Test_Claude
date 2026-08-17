package com.wikzo.todo.ui.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wikzo.todo.data.local.DeviceGroupStore
import com.wikzo.todo.data.model.Task
import com.wikzo.todo.data.repository.SyncGroupRepository
import com.wikzo.todo.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val isLoading: Boolean = true,
    val tasks: List<Task> = emptyList(),
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val isSyncing: Boolean = false,
)

/**
 * Incomplete tasks first, then by priority (HIGH, MEDIUM, LOW -- the enum's
 * declaration order), then by due date (soonest first, undated tasks last).
 * Completed tasks always sort after incomplete ones regardless of the above.
 */
private val taskComparator = compareBy<Task>(
    { it.completed },
    { it.priority.ordinal },
    { it.dueDate?.seconds ?: Long.MAX_VALUE },
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val syncGroupRepository: SyncGroupRepository,
    private val deviceGroupStore: DeviceGroupStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    private var groupId: String? = null

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
                        taskRepository.observeTasks(id)
                    }
                    .collect { snapshot ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                tasks = snapshot.tasks.sortedWith(taskComparator),
                                isOffline = snapshot.isFromCache,
                                isSyncing = snapshot.hasPendingWrites,
                            )
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

    fun deleteTask(task: Task) {
        val id = groupId ?: return
        viewModelScope.launch {
            taskRepository.deleteTask(id, task.id)
        }
    }
}
