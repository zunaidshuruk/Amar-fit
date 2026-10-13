package com.example.data.cycle

/**
 * Decides what to copy between this phone and the cloud for items that can be edited on either side
 * (periods, daily cycle logs): the newer `updatedAt` wins. A remote deletion marker beats a live local item.
 */
object LwwSyncPlanner {
    data class Plan<T>(
        /** Cloud items to store on this phone (new here, or newer than the local copy). */
        val pull: List<T>,
        /** Local items to upload (missing in the cloud, or newer than the cloud copy). */
        val push: List<T>,
        /** Local ids the cloud has marked as deleted: mark them removed here. */
        val removeLocal: List<String>
    )

    fun <T> plan(
        locals: List<T>,
        remoteLive: List<T>,
        remoteDeletedIds: Set<String>,
        id: (T) -> String,
        updatedAt: (T) -> Long,
        isRemoved: (T) -> Boolean = { false }
    ): Plan<T> {
        val pull = ArrayList<T>()
        val push = ArrayList<T>()
        val removeLocal = ArrayList<String>()
        val remoteById = remoteLive.associateBy(id)
        val localIds = locals.map(id).toSet()

        for (local in locals) {
            val key = id(local)
            val remote = remoteById[key]
            when {
                remote != null -> when {
                    updatedAt(remote) > updatedAt(local) -> pull.add(remote)
                    updatedAt(local) > updatedAt(remote) -> push.add(local)
                }
                key in remoteDeletedIds -> if (!isRemoved(local)) removeLocal.add(key)
                !isRemoved(local) -> push.add(local)
            }
        }
        for (remote in remoteLive) {
            if (id(remote) !in localIds) pull.add(remote)
        }
        return Plan(pull, push, removeLocal)
    }
}
