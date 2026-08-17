package com.wikzo.todo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.wikzo.todo.ui.theme.TodoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Registered unconditionally in onCreate (required before STARTED), only
    // ever launched below when the permission is actually missing.
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* No explicit handling either way -- if denied, reminders silently
           don't show a notification (NotificationHelper checks again before
           each one); there's no separate feature this blocks. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            TodoTheme {
                TodoApp()
            }
        }
    }

    /**
     * Requests POST_NOTIFICATIONS (API 33+ only -- it doesn't exist as a
     * runtime permission below that) up front on launch, rather than lazily
     * the first time a task with a due date is saved. Asking once at launch
     * keeps the ask decoupled from any single screen/ViewModel (which would
     * otherwise need an Activity reference just to prompt), and means a
     * reminder scheduled by the reconcile worker while the app is in the
     * background is already covered instead of depending on the save flow
     * having run at least once.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
