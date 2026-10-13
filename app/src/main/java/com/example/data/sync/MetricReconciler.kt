package com.example.data.sync

import com.example.data.local.DailyMetric

/** What a reconcile pass found: remote rows that change the phone, and dates the cloud is missing data for. */
data class ReconcilePlan(
    val applyFromRemote: List<DailyMetric>,
    val pushDates: List<String>
)

/** Result shown to the user / logs. */
data class ReconcileResult(val pulled: Int, val pushed: Int, val ok: Boolean)

/**
 * Pure planning step of the two-phone sync: compares the phone's rows with the cloud's rows
 * using [MetricMerger] and says what has to move in which direction. No I/O here, so it is unit tested.
 */
object MetricReconciler {

    fun plan(locals: List<DailyMetric>, remotes: List<DailyMetric>): ReconcilePlan {
        val localByDate = locals.filter { it.date.isNotBlank() }.associateBy { it.date }
        val remoteByDate = remotes.filter { it.date.isNotBlank() }.associateBy { it.date }

        val apply = mutableListOf<DailyMetric>()
        val mergedLocalByDate = HashMap<String, DailyMetric>()
        for ((date, remote) in remoteByDate) {
            val local = localByDate[date]
            val merged = if (local == null) remote else MetricMerger.merge(local, remote)
            if (local == null) {
                if (!isEmpty(remote)) {
                    apply.add(remote)
                    mergedLocalByDate[date] = remote
                }
            } else if (merged != local) {
                apply.add(remote)
                mergedLocalByDate[date] = merged
            }
        }

        val push = mutableListOf<String>()
        for ((date, local) in localByDate) {
            val effectiveLocal = mergedLocalByDate[date] ?: local
            val remote = remoteByDate[date]
            if (remote == null) {
                if (!isEmpty(effectiveLocal)) push.add(date)
            } else if (MetricMerger.merge(remote, effectiveLocal) != remote) {
                push.add(date)
            }
        }
        return ReconcilePlan(apply, push.sorted())
    }

    /** A row with no data at all (only the date): never worth moving. */
    fun isEmpty(metric: DailyMetric): Boolean {
        return metric.copy(updatedAt = 0L) == DailyMetric(date = metric.date)
    }
}
