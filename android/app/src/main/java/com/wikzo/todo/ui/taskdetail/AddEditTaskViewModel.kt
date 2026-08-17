package com.wikzo.todo.ui.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.wikzo.todo.data.model.Priority
import com.wikzo.todo.data.model.Task
import com.wikzo.todo.data.repository.SyncGroupRepository
import com.wikzo.todo.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditTaskUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val title: String = "",
    val notes: String = "",
    val dueDate: Timestamp? = null,
    val priority: Priority = Priority.MEDIUM,
    val titleError: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Backs both "add new task" and "edit existing task" -- the same screen and
 * ViewModel are reused for both, distinguished by whether a real [taskId] nav
 * arg was supplied (see [NEW_TASK_ID]).
 */
@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val syncGroupRepository: SyncGroupRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val taskId: String? = savedStateHandle.get<String>(ARG_TASK_ID)
        ?.takeIf { it.isNotBlank() && it != NEW_TASK_ID }

    private val _uiState = MutableStateFlow(AddEditTaskUiState(isNew = taskId == null))
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    private var groupId: String? = null
    private var existingTask: Task? = null

    init {
        val idToLoad = taskId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resolvedGroupId = syncGroupRepository.ensureLocalGroup()
                groupId = resolvedGroupId

                if (idToLoad != null) {
                    val task = taskRepository.getTask(resolvedGroupId, idToLoad)
                    existingTask = task
                    _uiState.update { state ->
                        if (task != null) {
                            state.copy(
                                isLoading = false,
                                title = task.title,
                                notes = task.notes.orEmpty(),
                                dueDate = task.dueDate,
                                priority = task.priority,
                            )
                        } else {
                            state.copy(isLoading = false, errorMessage = "Task not found")
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
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

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onDueDateChange(value: Timestamp?) {
        _uiState.update { it.copy(dueDate = value) }
    }

    fun onPriorityChange(value: Priority) {
        _uiState.update { it.copy(priority = value) }
    }

    fun save() {
        val state = _uiState.value
        val trimmedTitle = state.title.trim()
        if (trimmedTitle.isEmpty()) {
            _uiState.update { it.copy(titleError = "Title is required") }
            return
        }
        val resolvedGroupId = groupId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val notes = state.notes.trim().ifEmpty { null }
                if (state.isNew) {
                    val newTask = Task(
                        title = trimmedTitle,
                        notes = notes,
                        dueDate = state.dueDate,
                        priority = state.priority,
                    )
                    taskRepository.addTask(resolvedGroupId, newTask)
                } else {
                    val base = existingTask
                    if (base == null) {
                        _uiState.update { it.copy(isSaving = false, errorMessage = "Task not found") }
                        return@launch
                    }
                    val updated = base.copy(
                        title = trimmedTitle,
                        notes = notes,
                        dueDate = state.dueDate,
                        priority = state.priority,
                    )
                    taskRepository.updateTask(resolvedGroupId, updated)
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Couldn't save task")
                }
            }
        }
    }

    companion object {
        const val ARG_TASK_ID = "taskId"

        /** Sentinel nav-arg value meaning "no task id" -- i.e. the add-new flow. */
        const val NEW_TASK_ID = "new"
    }
}
