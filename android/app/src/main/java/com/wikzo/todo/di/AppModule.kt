package com.wikzo.todo.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Firebase SDK singletons the rest of the app depends on.
 *
 * DeviceGroupStore, TaskRepository, and SyncGroupRepository are *not* re-declared
 * here with their own @Provides methods: they already carry `@Inject constructor`
 * + `@Singleton`, which is the more idiomatic way to give a Hilt-owned class a
 * singleton binding, and Hilt wires them into SingletonComponent automatically.
 * They only need to be listed here at all because they depend (transitively) on
 * the bindings this module supplies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance().apply {
            // Explicit for clarity -- offline persistence with an unbounded cache is
            // the default on Android, but the app's whole offline-first design
            // depends on it, so it's spelled out here rather than left implicit.
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings {})
            }
        }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}
