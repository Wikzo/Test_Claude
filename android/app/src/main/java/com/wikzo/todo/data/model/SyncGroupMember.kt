package com.wikzo.todo.data.model

import com.google.firebase.Timestamp

/**
 * Mirrors `syncGroups/{groupId}/members/{uid}` as documented in /docs/data-model.md.
 * Membership docs are never written directly by this pass's UI beyond the single
 * solo-group bootstrap in SyncGroupRepository.ensureLocalGroup() -- pairing (which
 * would create members for a second device) is a later phase.
 */
data class SyncGroupMember(
    val uid: String = "",
    val platform: String = "android",
    val deviceName: String = "",
    val joinedAt: Timestamp? = null,
    val lastSeenAt: Timestamp? = null,
)
