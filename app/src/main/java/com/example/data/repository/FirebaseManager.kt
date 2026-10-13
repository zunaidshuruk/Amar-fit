package com.example.data.repository

import android.util.Log
import com.example.data.local.*
import com.example.data.sync.CollectionReconciler
import com.example.data.sync.MetricMerger
import com.example.data.sync.SyncIds
import com.example.data.sync.ProfileMerger
import kotlinx.coroutines.flow.firstOrNull
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class DeleteAccountResult {
    data object Success : DeleteAccountResult()
    data object NeedsReauth : DeleteAccountResult()
    data class Error(val message: String) : DeleteAccountResult()
}

object FirebaseManager {
    suspend fun deleteAccount(): DeleteAccountResult {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                val uid = user.uid
                val metricsSnap = db.collection("users").document(uid).collection("health_metrics").get().await()
                for (doc in metricsSnap.documents) {
                    db.collection("users").document(uid).collection("health_metrics").document(doc.id).delete().await()
                }
                val dietLogsSnap = db.collection("users").document(uid).collection("diet_logs").get().await()
                for (doc in dietLogsSnap.documents) {
                    db.collection("users").document(uid).collection("diet_logs").document(doc.id).delete().await()
                }
                
                val dietChartsSnap = db.collection("users").document(uid).collection("saved_diet_charts").get().await()
                for (doc in dietChartsSnap.documents) {
                    db.collection("users").document(uid).collection("saved_diet_charts").document(doc.id).delete().await()
                }

                val workoutsSnap = db.collection("users").document(uid).collection("saved_workouts").get().await()
                for (doc in workoutsSnap.documents) {
                    db.collection("users").document(uid).collection("saved_workouts").document(doc.id).delete().await()
                }

                val programsSnap = db.collection("users").document(uid).collection("workout_programs").get().await()
                for (doc in programsSnap.documents) {
                    db.collection("users").document(uid).collection("workout_programs").document(doc.id).delete().await()
                }

                val chatsSnap = db.collection("users").document(uid).collection("saved_chats").get().await()
                for (doc in chatsSnap.documents) {
                    db.collection("users").document(uid).collection("saved_chats").document(doc.id).delete().await()
                }

                val medicalRecordsSnap = db.collection("users").document(uid).collection("medical_records").get().await()
                for (doc in medicalRecordsSnap.documents) {
                    db.collection("users").document(uid).collection("medical_records").document(doc.id).delete().await()
                }

                val activitySnap = db.collection("users").document(uid).collection("activity_events").get().await()
                for (doc in activitySnap.documents) {
                    doc.reference.delete().await()
                }

                val assistantChatsSnap = db.collection("users").document(uid).collection("assistant_chats").get().await()
                for (doc in assistantChatsSnap.documents) {
                    val msgs = doc.reference.collection("messages").get().await()
                    for (m in msgs.documents) {
                        m.reference.delete().await()
                    }
                    doc.reference.delete().await()
                }

                deleteSocialData(db, uid)

                db.collection("users").document(uid).delete().await()
                user.delete().await()
                DeleteAccountResult.Success
            } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                DeleteAccountResult.NeedsReauth
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: ""
                if (msg.contains("requires recent authentication") || msg.contains("ERROR_REQUIRES_RECENT_LOGIN")) {
                    DeleteAccountResult.NeedsReauth
                } else {
                    DeleteAccountResult.Error(msg.ifBlank { "Unknown error during account deletion" })
                }
            }
        }
        return DeleteAccountResult.Error("No user logged in")
    }

    private suspend fun deleteSocialData(db: FirebaseFirestore, uid: String) {
        // a) Own messages: query friendships where uid1 == uid and where uid2 == uid (two queries),
        // keep only documents whose status == "accepted"; for each, query dm_threads/{doc.id}/messages
        // where senderUid == uid and delete every result document.
        try {
            val asUid1 = db.collection("friendships").whereEqualTo("uid1", uid).get().await()
            val asUid2 = db.collection("friendships").whereEqualTo("uid2", uid).get().await()
            val acceptedFriendships = (asUid1.documents + asUid2.documents).filter {
                it.getString("status") == "accepted"
            }
            for (fDoc in acceptedFriendships) {
                try {
                    val messagesSnap = db.collection("dm_threads").document(fDoc.id)
                        .collection("messages")
                        .whereEqualTo("senderUid", uid)
                        .get().await()
                    for (msgDoc in messagesSnap.documents) {
                        msgDoc.reference.delete().await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // b) Activity events: query activity_events where uid == uid and delete every result.
        try {
            val eventsSnap = db.collection("activity_events").whereEqualTo("uid", uid).get().await()
            for (doc in eventsSnap.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // c) Friend code: read public_profiles/{uid}; if it has a non-blank friendCode, delete friend_codes/{friendCode}.
        try {
            val profileSnap = db.collection("public_profiles").document(uid).get().await()
            val friendCode = profileSnap.getString("friendCode")
            if (!friendCode.isNullOrBlank()) {
                db.collection("friend_codes").document(friendCode).delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // d) Delete friend_stats/{uid}, then delete public_profiles/{uid}.
        try {
            db.collection("friend_stats").document(uid).delete().await()
            db.collection("public_profiles").document(uid).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // e) Friendships: query friendships where uid1 == uid and where uid2 == uid and delete every result (any status).
        try {
            val asUid1 = db.collection("friendships").whereEqualTo("uid1", uid).get().await()
            for (doc in asUid1.documents) {
                doc.reference.delete().await()
            }
            val asUid2 = db.collection("friendships").whereEqualTo("uid2", uid).get().await()
            for (doc in asUid2.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Writes a day's metrics with a read-merge-write transaction, so another phone's data is never
     * overwritten: counters keep the higher value, readings the newest non-empty one.
     * Returns the merged row that is now in the cloud, or null when signed out, offline or on failure.
     */
    suspend fun syncMetricMerged(local: DailyMetric): DailyMetric? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(user.uid)
            .collection("health_metrics").document(local.date)
        return try {
            db.runTransaction<DailyMetric> { tx ->
                val remote = tx.get(ref).toObject(DailyMetric::class.java)
                val merged = if (remote == null) local else MetricMerger.merge(local, remote)
                if (remote == null || merged != remote) {
                    tx.set(ref, merged)
                }
                merged
            }.await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "syncMetricMerged failed for ${local.date}", e)
            null
        }
    }

    /** Reads the cloud's daily rows from [startDate] (inclusive, "yyyy-MM-dd") or all of them; null on failure. */
    suspend fun fetchMetrics(startDate: String?): List<DailyMetric>? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            val col = FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .collection("health_metrics")
            val snap = (if (startDate == null) col.get() else col.whereGreaterThanOrEqualTo("date", startDate).get()).await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(DailyMetric::class.java)?.let { if (it.date.isBlank()) it.copy(date = doc.id) else it }
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "fetchMetrics failed", e)
            null
        }
    }

    /** Live feed of the cloud's daily rows from [startDate] on, so a second phone's changes arrive within seconds. */
    fun observeMetrics(startDate: String): Flow<List<DailyMetric>> = callbackFlow {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            close()
            return@callbackFlow
        }
        val registration = FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .collection("health_metrics")
            .whereGreaterThanOrEqualTo("date", startDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseManager", "observeMetrics failed", error)
                    return@addSnapshotListener
                }
                val rows = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DailyMetric::class.java)?.let { if (it.date.isBlank()) it.copy(date = doc.id) else it }
                } ?: emptyList()
                trySend(rows)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Writes the profile with a read-merge-write transaction: the most recently saved profile wins, points and
     * badges only grow. Returns the merged profile now in the cloud, or null when signed out / offline / failed.
     */
    suspend fun syncProfileMerged(local: UserProfile): UserProfile? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(user.uid)
        return try {
            db.runTransaction<UserProfile> { tx ->
                val remote = tx.get(ref).toObject(UserProfile::class.java)
                val merged = if (remote == null) local else ProfileMerger.merge(local, remote)
                if (remote == null || merged != remote) {
                    tx.set(ref, merged, SetOptions.merge())
                }
                merged
            }.await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "syncProfileMerged failed", e)
            null
        }
    }

    suspend fun fetchProfile(): UserProfile? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            FirebaseFirestore.getInstance().collection("users").document(user.uid).get().await()
                .toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "fetchProfile failed", e)
            null
        }
    }

    suspend fun claimFriendCode(uid: String): String? {
        val safeChars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789" // excludes ambiguous 0/O/1/I/L
        val db = FirebaseFirestore.getInstance()
        repeat(5) {
            val candidate = (1..5).map { safeChars.random() }.joinToString("")
            try {
                db.runTransaction { transaction ->
                    val codeRef = db.collection("friend_codes").document(candidate)
                    val snapshot = transaction.get(codeRef)
                    if (snapshot.exists()) {
                        throw FirebaseFirestoreException("Code already claimed", FirebaseFirestoreException.Code.ALREADY_EXISTS)
                    }
                    transaction.set(codeRef, mapOf("uid" to uid))
                }.await()
                return candidate
            } catch (e: Exception) {
                // This candidate was taken (or a transient error) -- try another.
            }
        }
        return null
    }

    fun syncPublicProfile(name: String, currentStreak: Int, points: Int, badges: String, friendCode: String) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "name" to name,
                "currentStreak" to currentStreak,
                "points" to points,
                "badges" to badges,
                "friendCode" to friendCode
            )
            db.collection("public_profiles").document(user.uid).set(data, SetOptions.merge())
        }
    }

    fun updateFcmToken(token: String) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null && token.isNotBlank()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("public_profiles").document(user.uid).set(
                mapOf(
                    "fcmToken" to com.google.firebase.firestore.FieldValue.delete(),
                    "fcmTokenUpdatedAt" to com.google.firebase.firestore.FieldValue.delete()
                ),
                SetOptions.merge()
            )
            db.collection("users").document(user.uid).set(
                mapOf("fcmToken" to token),
                SetOptions.merge()
            )
        }
    }

    fun syncFriendStats(metric: DailyMetric, profile: UserProfile) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "date" to metric.date,
                "steps" to metric.steps,
                "stepGoal" to profile.stepGoal,
                "waterLiters" to metric.waterLiters,
                "waterGoal" to profile.dailyWaterLimitLiters,
                "sleepHours" to metric.sleepHours,
                "sleepGoal" to profile.sleepGoalHours,
                "caloriesConsumed" to metric.caloriesConsumed,
                "calorieGoal" to profile.dailyCalorieLimit
            )
            db.collection("friend_stats").document(user.uid).set(data, SetOptions.merge())
        }
    }

    data class FriendRequestInfo(
        val pairId: String,
        val otherUid: String,
        val otherName: String
    )

    suspend fun resolveFriendCode(code: String): String? {
        val db = FirebaseFirestore.getInstance()
        return try {
            val snap = db.collection("friend_codes").document(code.uppercase()).get().await()
            snap.getString("uid")
        } catch (e: Exception) {
            null
        }
    }

    private fun pairIdFor(uidA: String, uidB: String): String =
        if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"

    suspend fun sendFriendRequest(targetUid: String): String {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return "Not signed in."
        if (user.uid == targetUid) return "You can't add yourself."
        val db = FirebaseFirestore.getInstance()
        val pairId = pairIdFor(user.uid, targetUid)
        return try {
            val existing = db.collection("friendships").document(pairId).get().await()
            if (existing.exists()) {
                val status = existing.getString("status")
                return if (status == "accepted") "You're already friends." else "A request is already pending with this user."
            }
            val data = mapOf(
                "uid1" to (if (user.uid < targetUid) user.uid else targetUid),
                "uid2" to (if (user.uid < targetUid) targetUid else user.uid),
                "requestedBy" to user.uid,
                "status" to "pending",
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            db.collection("friendships").document(pairId).set(data).await()
            "Friend request sent."
        } catch (e: Exception) {
            "Couldn't send request: ${e.message}"
        }
    }

    suspend fun getFriendRequests(): Pair<List<FriendRequestInfo>, List<FriendRequestInfo>> {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return Pair(emptyList(), emptyList())
        val db = FirebaseFirestore.getInstance()
        return try {
            val asUid1 = db.collection("friendships").whereEqualTo("uid1", user.uid).whereEqualTo("status", "pending").get().await()
            val asUid2 = db.collection("friendships").whereEqualTo("uid2", user.uid).whereEqualTo("status", "pending").get().await()
            val incoming = mutableListOf<FriendRequestInfo>()
            val outgoing = mutableListOf<FriendRequestInfo>()
            for (doc in (asUid1.documents + asUid2.documents)) {
                val uid1 = doc.getString("uid1") ?: continue
                val uid2 = doc.getString("uid2") ?: continue
                val requestedBy = doc.getString("requestedBy") ?: continue
                val otherUid = if (uid1 == user.uid) uid2 else uid1
                val otherName = try {
                    db.collection("public_profiles").document(otherUid).get().await().getString("name") ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
                val info = FriendRequestInfo(pairId = doc.id, otherUid = otherUid, otherName = otherName)
                if (requestedBy == user.uid) outgoing.add(info) else incoming.add(info)
            }
            Pair(incoming, outgoing)
        } catch (e: Exception) {
            Pair(emptyList(), emptyList())
        }
    }

    suspend fun acceptFriendRequest(pairId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("friendships").document(pairId)
                .update(mapOf("status" to "accepted", "acceptedAt" to com.google.firebase.Timestamp.now()))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFriendRequest(pairId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("friendships").document(pairId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    data class LeaderboardEntry(
        val uid: String = "",
        val name: String = "",
        val points: Int = 0,
        val currentStreak: Int = 0
    )

    data class FriendInfo(
        val uid: String = "",
        val name: String = "",
        val currentStreak: Int = 0,
        val points: Int = 0,
        val badgeCount: Int = 0,
        val pairId: String = ""
    )

    data class FriendStatsInfo(
        val date: String = "",
        val steps: Int = 0,
        val stepGoal: Int = 0,
        val waterLiters: Float = 0f,
        val waterGoal: Float = 0f,
        val sleepHours: Float = 0f,
        val sleepGoal: Float = 0f,
        val caloriesConsumed: Int = 0,
        val calorieGoal: Int = 0
    )

    suspend fun getLeaderboard(limit: Long = 50): List<LeaderboardEntry> {
        val db = FirebaseFirestore.getInstance()
        return try {
            val snap = db.collection("public_profiles")
                .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            snap.documents.map { doc ->
                LeaderboardEntry(
                    uid = doc.id,
                    name = doc.getString("name") ?: "Unknown",
                    points = (doc.getLong("points") ?: 0L).toInt(),
                    currentStreak = (doc.getLong("currentStreak") ?: 0L).toInt()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAcceptedFriends(): List<FriendInfo> {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return emptyList()
        val db = FirebaseFirestore.getInstance()
        return try {
            val asUid1 = db.collection("friendships").whereEqualTo("uid1", user.uid).whereEqualTo("status", "accepted").get().await()
            val asUid2 = db.collection("friendships").whereEqualTo("uid2", user.uid).whereEqualTo("status", "accepted").get().await()
            val friends = mutableListOf<FriendInfo>()
            for (doc in (asUid1.documents + asUid2.documents)) {
                val uid1 = doc.getString("uid1") ?: continue
                val uid2 = doc.getString("uid2") ?: continue
                val otherUid = if (uid1 == user.uid) uid2 else uid1
                try {
                    val profileSnap = db.collection("public_profiles").document(otherUid).get().await()
                    val badgesStr = profileSnap.getString("badges") ?: ""
                    friends.add(
                        FriendInfo(
                            uid = otherUid,
                            name = profileSnap.getString("name") ?: "Unknown",
                            currentStreak = (profileSnap.getLong("currentStreak") ?: 0L).toInt(),
                            points = (profileSnap.getLong("points") ?: 0L).toInt(),
                            badgeCount = badgesStr.split(",").count { it.isNotBlank() },
                            pairId = doc.id
                        )
                    )
                } catch (e: Exception) {
                    // Skip a friend whose public profile couldn't be read.
                }
            }
            friends
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFriendStats(uid: String): FriendStatsInfo? {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("friend_stats").document(uid).get().await().toObject(FriendStatsInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendKudos(toUid: String): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        return try {
            val today = java.time.LocalDate.now().toString()
            val kudosId = "${user.uid}_${toUid}_$today"
            db.collection("kudos").document(kudosId).set(
                mapOf(
                    "fromUid" to user.uid,
                    "toUid" to toUid,
                    "date" to today,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            postActivityEvent("kudos_sent", "Sent Kudos to a friend")
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasSentKudosToday(toUid: String): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        val today = java.time.LocalDate.now().toString()
        val kudosId = "${user.uid}_${toUid}_$today"
        return try {
            db.collection("kudos").document(kudosId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeFriend(pairId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("friendships").document(pairId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    data class ActivityFeedEntry(
        val eventId: String = "",
        val uid: String = "",
        val name: String = "",
        val type: String = "",
        val message: String = "",
        val createdAt: com.google.firebase.Timestamp? = null
    )

    fun postActivityEvent(type: String, message: String) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("activity_events").document().set(
                mapOf(
                    "uid" to user.uid,
                    "type" to type,
                    "message" to message,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
            )
        }
    }

    suspend fun getActivityFeed(): List<ActivityFeedEntry> {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return emptyList()
        val db = FirebaseFirestore.getInstance()
        return try {
            val friends = getAcceptedFriends()
            val relevantUids = (friends.map { it.uid } + user.uid).distinct().take(30)
            if (relevantUids.isEmpty()) return emptyList()
            val snap = db.collection("activity_events")
                .whereIn("uid", relevantUids)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(30)
                .get().await()
            snap.documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: return@mapNotNull null
                val name = if (uid == user.uid) "You" else (friends.find { it.uid == uid }?.name ?: "Unknown")
                ActivityFeedEntry(
                    eventId = doc.id,
                    uid = uid,
                    name = name,
                    type = doc.getString("type") ?: "",
                    message = doc.getString("message") ?: "",
                    createdAt = doc.getTimestamp("createdAt")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    data class DirectMessage(
        val messageId: String = "",
        val senderUid: String = "",
        val text: String = "",
        val createdAt: com.google.firebase.Timestamp? = null
    )

    suspend fun sendDirectMessage(pairId: String, text: String): String? {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return "Not signed in."
        if (text.isBlank()) return null
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("dm_threads").document(pairId).collection("messages").document().set(
                mapOf(
                    "senderUid" to user.uid,
                    "text" to text.trim(),
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "notified" to false
                )
            ).await()
            null
        } catch (e: Exception) {
            "Couldn't send message: ${e.message}"
        }
    }

    fun observeMessages(pairId: String): Flow<List<DirectMessage>> = callbackFlow {
        val db = FirebaseFirestore.getInstance()
        val registration = db.collection("dm_threads").document(pairId).collection("messages")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("DirectMessages", "observeMessages failed for pairId=$pairId", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    DirectMessage(
                        messageId = doc.id,
                        senderUid = doc.getString("senderUid") ?: "",
                        text = doc.getString("text") ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    data class ChallengeInfo(
        val challengeId: String = "",
        val otherUid: String = "",
        val otherName: String = "",
        val startDate: String = "",
        val endDate: String = "",
        val status: String = "",
        val myProgress: Long = 0L,
        val otherProgress: Long = 0L,
        val winnerUid: String? = null
    )

    suspend fun createChallenge(targetUid: String): String? {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return null
        val db = FirebaseFirestore.getInstance()
        val startDate = java.time.LocalDate.now()
        val endDate = startDate.plusDays(7)
        val data = mapOf(
            "uid1" to user.uid,
            "uid2" to targetUid,
            "createdBy" to user.uid,
            "startDate" to startDate.toString(),
            "endDate" to endDate.toString(),
            "status" to "active",
            "progress" to mapOf(user.uid to 0L, targetUid to 0L),
            "winnerUid" to null,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "notifiedCreation" to false,
            "notifiedCompletion" to false
        )
        return try {
            val ref = db.collection("challenges").document()
            ref.set(data).await()
            ref.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMyChallenges(): List<ChallengeInfo> {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return emptyList()
        val db = FirebaseFirestore.getInstance()
        return try {
            val asUid1 = db.collection("challenges").whereEqualTo("uid1", user.uid).get().await()
            val asUid2 = db.collection("challenges").whereEqualTo("uid2", user.uid).get().await()
            (asUid1.documents + asUid2.documents).mapNotNull { doc ->
                val uid1 = doc.getString("uid1") ?: return@mapNotNull null
                val uid2 = doc.getString("uid2") ?: return@mapNotNull null
                val otherUid = if (uid1 == user.uid) uid2 else uid1
                val progressMap = doc.get("progress") as? Map<*, *> ?: emptyMap<String, Long>()
                val otherName = try {
                    db.collection("public_profiles").document(otherUid).get().await().getString("name") ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
                ChallengeInfo(
                    challengeId = doc.id,
                    otherUid = otherUid,
                    otherName = otherName,
                    startDate = doc.getString("startDate") ?: "",
                    endDate = doc.getString("endDate") ?: "",
                    status = doc.getString("status") ?: "active",
                    myProgress = (progressMap[user.uid] as? Number)?.toLong() ?: 0L,
                    otherProgress = (progressMap[otherUid] as? Number)?.toLong() ?: 0L,
                    winnerUid = doc.getString("winnerUid")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateMyChallengeProgress(challengeId: String, mySteps: Long): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("challenges").document(challengeId)
                .update("progress.${user.uid}", mySteps)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun completeChallengeIfDue(challengeId: String, endDate: String, myProgress: Long, otherProgress: Long, myUid: String, otherUid: String): String? {
        val today = java.time.LocalDate.now()
        val end = try { java.time.LocalDate.parse(endDate) } catch (e: Exception) { return null }
        if (today.isBefore(end)) return null
        val winnerUid = when {
            myProgress > otherProgress -> myUid
            otherProgress > myProgress -> otherUid
            else -> null
        }
        val db = FirebaseFirestore.getInstance()
        return try {
            db.collection("challenges").document(challengeId)
                .update(mapOf("status" to "completed", "winnerUid" to winnerUid))
                .await()
            if (winnerUid != null) {
                postActivityEvent("challenge_won", "Won a 7-day steps challenge")
            }
            winnerUid
        } catch (e: Exception) {
            null
        }
    }

    fun syncFoodLog(log: FoodLog) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .collection("diet_logs").document(log.cloudId)
                .set(log, SetOptions.merge())
        }
    }

    fun deleteFoodLog(log: FoodLog) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // soft delete: other phones see the tombstone and remove their copy
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .collection("diet_logs").document(log.cloudId)
                .set(mapOf("deleted" to true, "deletedAt" to System.currentTimeMillis(), "date" to log.date))
        }
    }

    suspend fun syncSavedDietChart(chart: SavedDietChart): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .collection("saved_diet_charts").document(chart.cloudId)
                    .set(chart, SetOptions.merge()).await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    suspend fun deleteSavedDietChart(chart: SavedDietChart): Boolean = softDelete("saved_diet_charts", chart.cloudId)

    suspend fun syncSavedWorkout(workout: SavedWorkout): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .collection("saved_workouts").document(workout.cloudId)
                    .set(workout, SetOptions.merge()).await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    suspend fun deleteSavedWorkout(workout: SavedWorkout): Boolean = softDelete("saved_workouts", workout.cloudId)

    suspend fun syncWorkoutProgram(program: com.example.data.local.WorkoutProgram): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .collection("workout_programs").document(program.cloudId)
                    .set(program, SetOptions.merge()).await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    suspend fun deleteWorkoutProgram(cloudId: String): Boolean = softDelete("workout_programs", cloudId)

    suspend fun syncSavedChat(chat: SavedChat): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .collection("saved_chats").document(chat.cloudId)
                    .set(chat, SetOptions.merge()).await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    suspend fun deleteSavedChat(chat: SavedChat): Boolean = softDelete("saved_chats", chat.cloudId)

    suspend fun syncMedicalRecord(record: com.example.data.local.MedicalRecord): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .collection("medical_records").document(record.cloudId)
                    .set(record, SetOptions.merge()).await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    suspend fun deleteMedicalRecordRemote(cloudId: String): Boolean = softDelete("medical_records", cloudId)

    /** Marks a document as deleted instead of removing it, so other phones can apply the delete. */
    private suspend fun softDelete(collection: String, id: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        return try {
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .collection(collection).document(id)
                .set(mapOf("deleted" to true, "deletedAt" to System.currentTimeMillis()))
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class RemoteDoc<T>(val id: String, val item: T)
    data class CollectionPull<T>(val live: List<RemoteDoc<T>>, val deletedIds: Set<String>)

    /**
     * Reads one synced collection. Documents marked deleted are returned as ids (and removed from the cloud
     * once their tombstone is older than 30 days). [windowField] / [windowStart] limit the read to recent items.
     * Returns null when signed out, offline or on failure.
     */
    suspend fun <T : Any> fetchCollection(
        name: String,
        clazz: Class<T>,
        windowField: String? = null,
        windowStart: String? = null
    ): CollectionPull<T>? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            val col = FirebaseFirestore.getInstance().collection("users").document(user.uid).collection(name)
            val snap = (if (windowField != null && windowStart != null) {
                col.whereGreaterThanOrEqualTo(windowField, windowStart).get()
            } else {
                col.get()
            }).await()
            val live = ArrayList<RemoteDoc<T>>()
            val deleted = HashSet<String>()
            val now = System.currentTimeMillis()
            for (doc in snap.documents) {
                if (doc.getBoolean("deleted") == true) {
                    deleted.add(doc.id)
                    if (CollectionReconciler.isTombstoneExpired(doc.getLong("deletedAt") ?: 0L, now)) {
                        doc.reference.delete()
                    }
                } else {
                    doc.toObject(clazz)?.let { live.add(RemoteDoc(doc.id, it)) }
                }
            }
            CollectionPull(live, deleted)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "fetchCollection($name) failed", e)
            null
        }
    }

    // ---------------------------------------------------------------- assistant chats and activity events

    private fun userCollection(name: String) =
        FirebaseAuth.getInstance().currentUser?.let {
            FirebaseFirestore.getInstance().collection("users").document(it.uid).collection(name)
        }

    suspend fun syncAssistantSession(session: AssistantChatSession): Boolean {
        val col = userCollection("assistant_chats") ?: return false
        return try {
            col.document(session.id).set(session, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun syncAssistantMessage(sessionId: String, message: AssistantChatMessage): Boolean {
        val col = userCollection("assistant_chats") ?: return false
        return try {
            col.document(sessionId).collection("messages")
                .document(SyncIds.messageId(message.timestamp, message.isUser, message.text))
                .set(
                    mapOf(
                        "sessionId" to sessionId,
                        "text" to message.text,
                        "isUser" to message.isUser,
                        "timestamp" to message.timestamp
                    ),
                    SetOptions.merge()
                ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Deleting a chat leaves a tombstone (with a fresh updatedAt so other phones notice it). */
    suspend fun softDeleteAssistantSession(sessionId: String): Boolean {
        val col = userCollection("assistant_chats") ?: return false
        val now = System.currentTimeMillis()
        return try {
            // the tombstone replaces the chat document (no content kept) and its messages are removed
            col.document(sessionId)
                .set(mapOf("deleted" to true, "deletedAt" to now, "updatedAt" to now))
                .await()
            col.document(sessionId).collection("messages").get().await().documents.forEach { it.reference.delete() }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Chats changed after [sinceUpdatedAt] (0 = all). Deleted chats come back as ids. Null on failure. */
    suspend fun fetchAssistantSessions(sinceUpdatedAt: Long): CollectionPull<AssistantChatSession>? {
        val col = userCollection("assistant_chats") ?: return null
        return try {
            val snap = col.whereGreaterThan("updatedAt", sinceUpdatedAt).get().await()
            val live = ArrayList<RemoteDoc<AssistantChatSession>>()
            val deleted = HashSet<String>()
            val now = System.currentTimeMillis()
            for (doc in snap.documents) {
                if (doc.getBoolean("deleted") == true) {
                    deleted.add(doc.id)
                    if (CollectionReconciler.isTombstoneExpired(doc.getLong("deletedAt") ?: 0L, now)) {
                        doc.reference.delete()
                    }
                } else {
                    doc.toObject(AssistantChatSession::class.java)?.let { live.add(RemoteDoc(doc.id, it)) }
                }
            }
            CollectionPull(live, deleted)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "fetchAssistantSessions failed", e)
            null
        }
    }

    suspend fun fetchAssistantMessages(sessionId: String): List<RemoteDoc<AssistantChatMessage>>? {
        val col = userCollection("assistant_chats") ?: return null
        return try {
            col.document(sessionId).collection("messages").get().await().documents.map { doc ->
                RemoteDoc(
                    doc.id,
                    AssistantChatMessage(
                        id = 0,
                        sessionId = sessionId,
                        text = doc.getString("text") ?: "",
                        isUser = doc.getBoolean("isUser") ?: false,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "fetchAssistantMessages failed", e)
            null
        }
    }

    suspend fun syncActivityEvent(event: ActivityEvent): Boolean {
        val col = userCollection("activity_events") ?: return false
        return try {
            col.document(SyncIds.activityEventId(event.timestamp, event.type, event.description))
                .set(
                    mapOf(
                        "type" to event.type,
                        "description" to event.description,
                        "timestamp" to event.timestamp,
                        "source" to event.source
                    ),
                    SetOptions.merge()
                ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchActivityEvents(sinceTimestamp: Long): List<RemoteDoc<ActivityEvent>>? {
        val col = userCollection("activity_events") ?: return null
        return try {
            col.whereGreaterThanOrEqualTo("timestamp", sinceTimestamp).get().await().documents.map { doc ->
                RemoteDoc(
                    doc.id,
                    ActivityEvent(
                        id = 0,
                        type = doc.getString("type") ?: "",
                        description = doc.getString("description") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        source = doc.getString("source") ?: "app",
                        externalId = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "fetchActivityEvents failed", e)
            null
        }
    }

    suspend fun pullDataOnLogin(
        userDao: UserDao, 
        metricsDao: MetricsDao, 
        savedDietChartDao: SavedDietChartDao? = null,
        savedWorkoutDao: SavedWorkoutDao? = null,
        savedChatDao: SavedChatDao? = null,
        medicalRecordDao: com.example.data.local.MedicalRecordDao? = null,
        workoutProgramDao: com.example.data.local.WorkoutProgramDao? = null
    ) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            
            // Pull Profile
            try {
                val profileSnap = db.collection("users").document(user.uid).get().await()
                val profile = profileSnap.toObject(UserProfile::class.java)
                if (profile != null) {
                    val local = userDao.getUserProfile().firstOrNull()
                    userDao.insertProfile(if (local == null) profile else ProfileMerger.merge(local, profile))
                }
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Error pulling profile on login", e)
                e.printStackTrace()
            }

            // Metrics are merged (not replaced) by AppRepository.reconcileMetrics after login.

            // Food logs, saved charts, workouts, programs, chats and medical records are merged
            // (with deletes) by AppRepository.reconcileCollections after login.
        }
    }
}
