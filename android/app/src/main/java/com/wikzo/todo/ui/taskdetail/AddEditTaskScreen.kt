package com.wikzo.todo.ui.taskdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Timestamp
import com.wikzo.todo.data.model.Priority
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

@Composable
fun AddEditTaskScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onDone()
        }
    }

    AddEditTaskScreen(
        uiState = uiState,
        onTitleChange = viewModel::onTitleChange,
        onNotesChange = viewModel::onNotesChange,
        onDueDateChange = viewModel::onDueDateChange,
        onPriorityChange = viewModel::onPriorityChange,
        onSave = viewModel::save,
        onCancel = onCancel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditTaskScreen(
    uiState: AddEditTaskUiState,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDueDateChange: (Timestamp?) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "New task" else "Edit task") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = onSave, enabled = !uiState.isSaving && !uiState.isLoading) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                isError = uiState.titleError != null,
                supportingText = {
                    uiState.titleError?.let { Text(it) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            PrioritySelector(
                selected = uiState.priority,
                onSelect = onPriorityChange,
            )

            DueDateField(
                dueDate = uiState.dueDate,
                onDueDateChange = onDueDateChange,
            )

            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                TextButton(onClick = onSave, enabled = !uiState.isSaving && !uiState.isLoading) {
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrioritySelector(
    selected: Priority,
    onSelect: (Priority) -> Unit,
) {
    val options = listOf(Priority.HIGH, Priority.MEDIUM, Priority.LOW)
    Column {
        Text(
            text = "Priority",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            options.forEachIndexed { index, priority ->
                SegmentedButton(
                    selected = selected == priority,
                    onClick = { onSelect(priority) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(priority.label())
                }
            }
        }
    }
}

private fun Priority.label(): String = when (this) {
    Priority.HIGH -> "High"
    Priority.MEDIUM -> "Medium"
    Priority.LOW -> "Low"
}

private val dueDateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDateField(
    dueDate: Timestamp?,
    onDueDateChange: (Timestamp?) -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var pendingDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    Column {
        Text(
            text = "Due date",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = dueDate?.let { formatDueDateTime(it) } ?: "None",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Pick due date")
            }
            if (dueDate != null) {
                IconButton(onClick = { onDueDateChange(null) }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear due date")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.let { it.toDate().time } ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    if (selectedMillis != null) {
                        pendingDateMillis = selectedMillis
                        showTimePicker = true
                    }
                }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val existingLocalTime = dueDate?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: LocalTime.of(9, 0)
        val timePickerState = rememberTimePickerState(
            initialHour = existingLocalTime.hour,
            initialMinute = existingLocalTime.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pendingDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        val combined = combineDateAndTime(date, timePickerState.hour, timePickerState.minute)
                        onDueDateChange(combined)
                    }
                    showTimePicker = false
                }) {
                    Text("Set time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }
}

private fun combineDateAndTime(date: LocalDate, hour: Int, minute: Int): Timestamp {
    val localDateTime = date.atTime(hour, minute)
    val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
    return Timestamp(Date.from(instant))
}

private fun formatDueDateTime(timestamp: Timestamp): String {
    val instant = timestamp.toDate().toInstant()
    return instant.atZone(ZoneId.systemDefault()).format(dueDateTimeFormatter)
}
