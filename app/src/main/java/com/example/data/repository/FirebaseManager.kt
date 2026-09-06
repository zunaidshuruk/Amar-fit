package com.example.data.repository

import com.example.data.local.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    suspend fun pullDataOnLogin(
        userDao: UserDao, 
        metricsDao: MetricsDao, 
        savedDietChartDao: SavedDietChartDao? = null,
        savedWorkoutDao: SavedWorkoutDao? = null,
        savedChatDao: SavedChatDao? = null
    ) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            
            try {
                // Pull Profile
                val profileSnap = db.collection("users").document(user.uid).get().await()
                val profile = profileSnap.toObject(UserProfile::class.java)
                if (profile != null) {
                    userDao.insertProfile(profile)
                }

                // Pull Metrics
                val metricsSnap = db.collection("users").document(user.uid).collection("health_metrics").get().await()
                for (doc in metricsSnap.documents) {
                    val metric = doc.toObject(DailyMetric::class.java)
                    if (metric != null) {
                        metricsDao.insertMetrics(metric)
                    }
                }

                // Pull Food Logs
                val foodLogsSnap = db.collection("users").document(user.uid).collection("diet_logs").get().await()
                for (doc in foodLogsSnap.documents) {
                    val log = doc.toObject(FoodLog::class.java)
                    if (log != null) {
                        val resolvedLog = if (log.cloudId.isEmpty()) log.copy(cloudId = doc.id) else log
                        metricsDao.insertFoodLog(resolvedLog)
                    }
                }

                // Pull Saved Diet Charts
                if (savedDietChartDao != null) {
                    val chartsSnap = db.collection("users").document(user.uid).collection("saved_diet_charts").get().await()
                    for (doc in chartsSnap.documents) {
                        val chart = doc.toObject(SavedDietChart::class.java)
                        if (chart != null) {
                            savedDietChartDao.insertChart(chart)
                        }
                    }
                }

                // Pull Saved Workouts
                if (savedWorkoutDao != null) {
                    val workoutsSnap = db.collection("users").document(user.uid).collection("saved_workouts").get().await()
                    for (doc in workoutsSnap.documents) {
                        val workout = doc.toObject(SavedWorkout::class.java)
                        if (workout != null) {
                            savedWorkoutDao.insertWorkout(workout)
                        }
                    }
                }

                // Pull Saved Chats
                if (savedChatDao != null) {
                    val chatsSnap = db.collection("users").document(user.uid).collection("saved_chats").get().await()
                    for (doc in chatsSnap.documents) {
                        val chat = doc.toObject(SavedChat::class.java)
                        if (chat != null) {
                            savedChatDao.insertChat(chat)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
