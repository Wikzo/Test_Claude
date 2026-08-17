package com.wikzo.todo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wikzo.todo.ui.pairing.PairDeviceScreen
import com.wikzo.todo.ui.pairing.PairingScreen
import com.wikzo.todo.ui.taskdetail.AddEditTaskScreen
import com.wikzo.todo.ui.taskdetail.AddEditTaskViewModel.Companion.ARG_TASK_ID
import com.wikzo.todo.ui.taskdetail.AddEditTaskViewModel.Companion.NEW_TASK_ID
import com.wikzo.todo.ui.tasklist.TaskListScreen

private const val ROUTE_TASK_LIST = "taskList"
private const val ROUTE_TASK_DETAIL = "taskDetail"
private const val ROUTE_PAIRING = "pairing"
private const val ROUTE_PAIR_DEVICE = "pairDevice"

/** Root composable: task list <-> add/edit <-> the two pairing screens. */
@Composable
fun TodoApp() {
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = ROUTE_TASK_LIST) {
            composable(ROUTE_TASK_LIST) {
                TaskListScreen(
                    onAddTask = {
                        navController.navigate("$ROUTE_TASK_DETAIL/$NEW_TASK_ID")
                    },
                    onEditTask = { task ->
                        navController.navigate("$ROUTE_TASK_DETAIL/${task.id}")
                    },
                    onShowMyCode = {
                        navController.navigate(ROUTE_PAIRING)
                    },
                    onEnterCode = {
                        navController.navigate(ROUTE_PAIR_DEVICE)
                    },
                )
            }
            composable(
                route = "$ROUTE_TASK_DETAIL/{$ARG_TASK_ID}",
                arguments = listOf(navArgument(ARG_TASK_ID) { type = NavType.StringType }),
            ) {
                AddEditTaskScreen(
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(ROUTE_PAIRING) {
                PairingScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ROUTE_PAIR_DEVICE) {
                PairDeviceScreen(
                    onBack = { navController.popBackStack() },
                    onDeviceLinked = {
                        // Drop the pairing screens from the back stack too -- once
                        // linked, "back" from the task list shouldn't return to a
                        // stale "enter a code" screen.
                        navController.popBackStack(ROUTE_TASK_LIST, inclusive = false)
                    },
                )
            }
        }
    }
}
