package com.wikzo.todo.data.model

import com.google.firebase.Timestamp

/**
 * Mirrors `syncGroups/{groupId}/streaks/summary` as documented in /docs/data-model.md.
 * Keep this class in lockstep with that doc's "Kotlin representation" section, same
 * discipline as [Task].
 *
 * This is a daily-habit streak: [currentStreak] counts consecutive local-calendar
 * days on which every task in the group was completed at least once, and
 * [lastCompletedAllAt] is the server timestamp of the most recent such clear. See
 * [com.wikzo.todo.data.repository.StreakRepository] for the update algorithm.
 */
data class StreakSummary(
    val currentStreak: Int = 0,
    val lastCompletedAllAt: Timestamp? = null,
)
