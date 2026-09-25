package com.example.data.repository

import android.util.Log
import com.example.data.local.*
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

                val chatsSnap = db.collection("users").document(uid).collection("saved_chats").get().await()
                for (doc in chatsSnap.documents) {
                    db.collection("users").document(uid).collection("saved_chats").document(doc.id).delete().await()
                }

                val medicalRecordsSnap = db.collection("users").document(uid).collection("medical_records").get().await()
                for (doc in medicalRecordsSnap.documents) {
                    db.collection("users").document(uid).collection("medical_records").document(doc.id).delete().await()
                }

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

    fun syncMetric(metric: DailyMetric) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .collection("health_metrics").document(metric.date)
                .set(metric, SetOptions.merge())
        }
    }
    
    suspend fun syncProfile(profile: UserProfile): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .set(profile, SetOptions.merge())
                    .await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
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
                mapOf("fcmToken" to token, "fcmTokenUpdatedAt" to com.google.firebase.Timestamp.now()),
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
                    "createdAt" to com.google.firebase.Timestamp.now()
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
            "createdAt" to com.google.firebase.Timestamp.now()
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
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .collection("diet_logs").document(log.cloudId)
                .delete()
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

    suspend fun deleteSavedDietChart(chart: SavedDietChart): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .collection("saved_diet_charts").document(chart.cloudId)
                    .delete().await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

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

    suspend fun deleteSavedWorkout(workout: SavedWorkout): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .collection("saved_workouts").document(workout.cloudId)
                    .delete().await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

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

    suspend fun deleteSavedChat(chat: SavedChat): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .collection("saved_chats").document(chat.cloudId)
                    .delete().await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

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

    suspend fun deleteMedicalRecordRemote(cloudId: String): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            return try {
                db.collection("users").document(user.uid)
                    .collection("medical_records").document(cloudId)
                    .delete().await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    suspend fun pullDataOnLogin(
        userDao: UserDao, 
        metricsDao: MetricsDao, 
        savedDietChartDao: SavedDietChartDao? = null,
        savedWorkoutDao: SavedWorkoutDao? = null,
        savedChatDao: SavedChatDao? = null,
        medicalRecordDao: com.example.data.local.MedicalRecordDao? = null
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
                    userDao.insertProfile(profile)
                }
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Error pulling profile on login", e)
                e.printStackTrace()
            }

            // Pull Metrics
            try {
                val metricsSnap = db.collection("users").document(user.uid).collection("health_metrics").get().await()
                for (doc in metricsSnap.documents) {
                    val metric = doc.toObject(DailyMetric::class.java)
                    if (metric != null) {
                        metricsDao.insertMetrics(metric)
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Error pulling metrics on login", e)
                e.printStackTrace()
            }

            // Pull Food Logs
            try {
                val foodLogsSnap = db.collection("users").document(user.uid).collection("diet_logs").get().await()
                for (doc in foodLogsSnap.documents) {
                    val log = doc.toObject(FoodLog::class.java)
                    if (log != null) {
                        val resolvedLog = if (log.cloudId.isEmpty()) log.copy(cloudId = doc.id) else log
                        metricsDao.insertFoodLog(resolvedLog)
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Error pulling food logs on login", e)
                e.printStackTrace()
            }

            // Pull Saved Diet Charts
            if (savedDietChartDao != null) {
                try {
                    val chartsSnap = db.collection("users").document(user.uid).collection("saved_diet_charts").get().await()
                    for (doc in chartsSnap.documents) {
                        val chart = doc.toObject(SavedDietChart::class.java)
                        if (chart != null) {
                            savedDietChartDao.insertChart(chart)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error pulling saved diet charts on login", e)
                    e.printStackTrace()
                }
            }

            // Pull Saved Workouts
            if (savedWorkoutDao != null) {
                try {
                    val workoutsSnap = db.collection("users").document(user.uid).collection("saved_workouts").get().await()
                    for (doc in workoutsSnap.documents) {
                        val workout = doc.toObject(SavedWorkout::class.java)
                        if (workout != null) {
                            savedWorkoutDao.insertWorkout(workout)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error pulling saved workouts on login", e)
                    e.printStackTrace()
                }
            }

            // Pull Saved Chats
            if (savedChatDao != null) {
                try {
                    val chatsSnap = db.collection("users").document(user.uid).collection("saved_chats").get().await()
                    for (doc in chatsSnap.documents) {
                        val chat = doc.toObject(SavedChat::class.java)
                        if (chat != null) {
                            savedChatDao.insertChat(chat)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error pulling saved chats on login", e)
                    e.printStackTrace()
                }
            }

            // Pull Medical Records
            if (medicalRecordDao != null) {
                try {
                    val recordsSnap = db.collection("users").document(user.uid).collection("medical_records").get().await()
                    for (doc in recordsSnap.documents) {
                        val record = doc.toObject(com.example.data.local.MedicalRecord::class.java)
                        if (record != null) {
                            medicalRecordDao.insertMedicalRecord(record)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error pulling medical records on login", e)
                    e.printStackTrace()
                }
            }
        }
    }
}
