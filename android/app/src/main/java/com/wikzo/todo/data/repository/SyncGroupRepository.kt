package com.wikzo.todo.data.repository

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.wikzo.todo.data.local.DeviceGroupStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A freshly generated pairing code plus its expiry, as returned by `createPairingCode`. */
data class PairingCode(val code: String, val expiresAtMillis: Long)

/**
 * Thrown by [SyncGroupRepository.createPairingCode] / [SyncGroupRepository.claimPairingCode]
 * with a [message] that is already safe to show directly in the UI -- callers never need to
 * inspect a raw [FirebaseFunctionsException] themselves.
 */
class PairingException(message: String) : Exception(message)

/**
 * Bootstraps this device's own solo sync group, and handles pairing -- joining
 * another device's existing group via a short-lived 6-digit code -- by calling the
 * `createPairingCode` / `claimPairingCode` Cloud Functions (Admin SDK on the server
 * side, which is why `pairingCodes/*` is locked down to "server only" in
 * firestore.rules: these two callables are the sole, auditable way to join a group
 * you didn't create yourself).
 */
@Singleton
class SyncGroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
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

    /**
     * Generates a short-lived pairing code for THIS device's group, so another
     * device can join it. Ensures a local group exists first (the caller doesn't
     * need to have called [ensureLocalGroup] itself).
     */
    suspend fun createPairingCode(): PairingCode {
        val groupId = ensureLocalGroup()

        val result = try {
            functions.getHttpsCallable("createPairingCode")
                .call(hashMapOf("groupId" to groupId))
                .await()
        } catch (e: Exception) {
            // Any failure here (network, server error, etc.) collapses to one
            // generic message per the product spec -- there's no actionable
            // distinction for the user to make on this path.
            throw PairingException("Couldn't generate a code. Try again.")
        }

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?>
        val code = data?.get("code") as? String
        val expiresAtMillis = (data?.get("expiresAtMillis") as? Number)?.toLong()
        if (code.isNullOrBlank() || expiresAtMillis == null) {
            throw PairingException("Couldn't generate a code. Try again.")
        }
        return PairingCode(code = code, expiresAtMillis = expiresAtMillis)
    }

    /**
     * Claims [code], joining the group it belongs to. On success, overwrites this
     * device's locally persisted group id with the new one and returns it -- this
     * device's previous solo group (and any tasks in it) is simply abandoned, per
     * product decision; no migration is performed.
     */
    suspend fun claimPairingCode(code: String): String {
        // Guarantees anonymous auth (and a solo group, if this is somehow the very
        // first thing the device does) so the callable always has a signed-in uid
        // to attribute the claim to. ensureLocalGroup() is idempotent -- if this
        // device already has a group (the common case), this is just a local read,
        // and that group is what's about to be overwritten below regardless.
        ensureLocalGroup()

        val result = try {
            functions.getHttpsCallable("claimPairingCode")
                .call(
                    hashMapOf(
                        "code" to code,
                        "platform" to "android",
                        "deviceName" to Build.MODEL,
                    ),
                )
                .await()
        } catch (e: FirebaseFunctionsException) {
            throw PairingException(e.toClaimErrorMessage())
        } catch (e: Exception) {
            throw PairingException("Couldn't link the device. Try again.")
        }

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?>
        val groupId = data?.get("groupId") as? String
        if (groupId.isNullOrBlank()) {
            throw PairingException("Couldn't link the device. Try again.")
        }

        deviceGroupStore.setGroupId(groupId)
        return groupId
    }

    private fun FirebaseFunctionsException.toClaimErrorMessage(): String = when (code) {
        FirebaseFunctionsException.Code.NOT_FOUND ->
            "That code isn't valid. Double-check it and try again."
        FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
            "That code has already been used. Ask the other device for a new one."
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
            "That code has expired. Ask the other device for a new one."
        else ->
            "Couldn't link the device. Try again."
    }
}
