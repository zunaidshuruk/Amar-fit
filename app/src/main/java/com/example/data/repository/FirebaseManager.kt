package com.example.data.repository

import com.example.data.local.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    suspend fun deleteAccount(): Boolean {
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

                db.collection("users").document(uid).delete().await()
                user.delete().await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
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

    suspend fun syncSavedDietChart(chart: SavedDietChart) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            try {
                db.collection("users").document(user.uid)
                    .collection("saved_diet_charts").document(chart.cloudId)
                    .set(chart, SetOptions.merge()).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteSavedDietChart(chart: SavedDietChart) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            try {
                db.collection("users").document(user.uid)
                    .collection("saved_diet_charts").document(chart.cloudId)
                    .delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncSavedWorkout(workout: SavedWorkout) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            try {
                db.collection("users").document(user.uid)
                    .collection("saved_workouts").document(workout.cloudId)
                    .set(workout, SetOptions.merge()).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteSavedWorkout(workout: SavedWorkout) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            try {
                db.collection("users").document(user.uid)
                    .collection("saved_workouts").document(workout.cloudId)
                    .delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun pullDataOnLogin(
        userDao: UserDao, 
        metricsDao: MetricsDao, 
        savedDietChartDao: SavedDietChartDao? = null,
        savedWorkoutDao: SavedWorkoutDao? = null
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

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
