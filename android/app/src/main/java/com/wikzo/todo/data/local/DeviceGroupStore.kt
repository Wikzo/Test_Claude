package com.wikzo.todo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "device_group_prefs")

/**
 * Persists the Firestore `syncGroups/{groupId}` this device belongs to. There is
 * exactly one group id per install in this pass (no pairing/multi-group support yet).
 */
@Singleton
class DeviceGroupStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val groupIdKey = stringPreferencesKey("group_id")

    val groupId: Flow<String?> = context.dataStore.data.map { prefs -> prefs[groupIdKey] }

    suspend fun setGroupId(id: String) {
        context.dataStore.edit { prefs -> prefs[groupIdKey] = id }
    }
}
