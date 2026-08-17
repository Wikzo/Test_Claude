package com.wikzo.todo.data.repository

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.wikzo.todo.data.local.DeviceGroupStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bootstraps this device's own solo sync group. Pairing (joining someone else's
 * group via a pairing code) is a later phase -- for this pass, every device is
 * the sole member of a group it creates for itself on first launch.
 */
@Singleton
class SyncGroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val deviceGroupStore: DeviceGroupStore,
) {

    /**
     * Returns the Firestore `syncGroups/{groupId}` this device belongs to, creating
     * one (plus this device's `members/{uid}` doc) on first call. Safe to call
     * repeatedly -- subsequent calls just return the persisted id.
     */
    suspend fun ensureLocalGroup(): String {
        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        val uid = requireNotNull(user?.uid) { "Anonymous sign-in did not return a uid" }

        deviceGroupStore.groupId.first()?.let { existingGroupId ->
            return existingGroupId
        }

        val groupId = UUID.randomUUID().toString()
        val groupRef = firestore.collection("syncGroups").document(groupId)

        groupRef.set(
            hashMapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "createdByUid" to uid,
            ),
        ).await()

        groupRef.collection("members").document(uid).set(
            hashMapOf(
                "uid" to uid,
                "platform" to "android",
                "deviceName" to Build.MODEL,
                "joinedAt" to FieldValue.serverTimestamp(),
                "lastSeenAt" to FieldValue.serverTimestamp(),
            ),
        ).await()

        deviceGroupStore.setGroupId(groupId)
        return groupId
    }
}
