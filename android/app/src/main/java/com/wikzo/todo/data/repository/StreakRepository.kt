package com.wikzo.todo.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private const val FIELD_CURRENT_STREAK = "currentStreak"
private const val FIELD_LAST_COMPLETED_ALL_AT = "lastCompletedAllAt"

/**
 * Maintains `syncGroups/{groupId}/streaks/summary`, per /docs/data-model.md.
 *
 * The only mutation this repository performs is [recordAllTasksCleared], called
 * once per "the incomplete-task count just dropped from >0 to 0" moment -- the same
 * event that triggers the confetti celebration in TaskListViewModel. Everything
 * else about a task's lifecycle (add/edit/complete/delete) stays in [TaskRepository];
 * this is a separate, narrower doc so the two concerns don't get tangled in one
 * class.
 */
@Singleton
class StreakRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private fun summaryDoc(groupId: String) =
        firestore.collection("syncGroups").document(groupId)
            .collection("streaks").document("summary")

    /**
     * Records that every task in [groupId] just became complete, and returns the
     * resulting streak count.
     *
     * Runs as a Firestore transaction (read-then-conditionally-write) so two
     * near-simultaneous clears -- e.g. this device and a paired device racing to
     * complete the last task -- can't corrupt the count. Algorithm, using the
     * *device's local calendar day* (deliberately not UTC -- this is a personal
     * daily-habit streak, not something needing cross-timezone consistency):
     *
     * 1. If the doc's `lastCompletedAllAt` is already today, this is a no-op --
     *    the user un-completed and re-completed the last task the same day, and
     *    the existing streak is returned unchanged rather than bumped again.
     * 2. Otherwise, if `lastCompletedAllAt` was yesterday, the streak continues
     *    (`currentStreak + 1`); any other gap (or no prior doc) resets it to 1.
     * 3. The doc is written with `SetOptions.merge()`, not `set()`/`update()`, so
     *    it doesn't clobber any other field that might be added to this doc later.
     */
    suspend fun recordAllTasksCleared(groupId: String): Int {
        val docRef = summaryDoc(groupId)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentStreak = snapshot.getLong(FIELD_CURRENT_STREAK)?.toInt() ?: 0
            val lastDate = snapshot.getTimestamp(FIELD_LAST_COMPLETED_ALL_AT)?.toLocalDate()

            val today = LocalDate.now(ZoneId.systemDefault())
            if (lastDate == today) {
                return@runTransaction currentStreak
            }

            val yesterday = today.minusDays(1)
            val newStreak = if (lastDate == yesterday) currentStreak + 1 else 1

            transaction.set(
                docRef,
                mapOf(
                    FIELD_CURRENT_STREAK to newStreak,
                    FIELD_LAST_COMPLETED_ALL_AT to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            newStreak
        }.await()
    }

    private fun Timestamp.toLocalDate(): LocalDate =
        toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}
