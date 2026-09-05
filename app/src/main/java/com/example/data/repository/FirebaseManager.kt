package com.example.data.repository

import com.example.data.local.DailyMetric
import com.example.data.local.UserProfile
import com.example.data.local.FoodLog
import com.example.data.local.MetricsDao
import com.example.data.local.UserDao
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
                .collection("diet_logs").document(log.id.toString())
                .set(log, SetOptions.merge())
        }
    }

    fun deleteFoodLog(log: FoodLog) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .collection("diet_logs").document(log.id.toString())
                .delete()
        }
    }

    suspend fun pullDataOnLogin(userDao: UserDao, metricsDao: MetricsDao) {
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
                        metricsDao.insertFoodLog(log)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
