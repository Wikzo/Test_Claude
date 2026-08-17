package com.wikzo.todo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.wikzo.todo.notifications.NotificationHelper
import com.wikzo.todo.notifications.ReminderReconcileWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * [Configuration.Provider] supplies [HiltWorkerFactory] so that
 * [ReminderReconcileWorker] (a `@HiltWorker`) gets its [com.wikzo.todo.data.repository.TaskRepository]
 * / [com.wikzo.todo.data.local.DeviceGroupStore] dependencies injected by Hilt
 * instead of needing a hand-rolled `WorkerFactory` + `EntryPoint`. This requires
 * disabling WorkManager's default (Hilt-unaware) auto-initialization in the
 * manifest -- see the `androidx.startup.InitializationProvider` override in
 * AndroidManifest.xml -- otherwise WorkManager would initialize itself with the
 * default factory before this class's `workManagerConfiguration` is ever consulted.
 */
@HiltAndroidApp
class TodoApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        ReminderReconcileWorker.enqueuePeriodic(this)
    }
}
