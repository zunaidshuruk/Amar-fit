package com.example.data.sync

/** What to do for one synced collection (food logs, saved charts, workouts, ...). */
data class CollectionPlan(
    val upsertIds: List<String>,
    val deleteLocalIds: List<String>,
    val pushIds: List<String>
)

/**
 * Pure planning step for collections that sync item by item (each item has a stable id).
 * Deleting an item on one phone leaves a "tombstone" (deleted = true) in the cloud; the other phones apply it
 * and tombstones are removed after [TOMBSTONE_DAYS] days.
 */
object CollectionReconciler {
    const val TOMBSTONE_DAYS = 30L

    fun plan(localIds: Set<String>, remoteLiveIds: Set<String>, remoteDeletedIds: Set<String>): CollectionPlan {
        val deleteLocal = localIds.intersect(remoteDeletedIds)
        val push = localIds - remoteLiveIds - remoteDeletedIds
        return CollectionPlan(
            upsertIds = remoteLiveIds.sorted(),
            deleteLocalIds = deleteLocal.sorted(),
            pushIds = push.sorted()
        )
    }

    fun isTombstoneExpired(deletedAt: Long, now: Long): Boolean {
        return deletedAt > 0L && now - deletedAt > TOMBSTONE_DAYS * 24L * 60L * 60L * 1000L
    }
}
