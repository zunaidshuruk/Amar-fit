import re

with open('app/src/main/java/com/example/data/repository/AppRepository.kt', 'r') as f:
    content = f.read()

# Add syncMetric to saveMetrics
old_save_metrics = """    suspend fun saveMetrics(metric: DailyMetric) {
        metricsDao.insertMetric(metric)
    }"""
new_save_metrics = """    suspend fun saveMetrics(metric: DailyMetric) {
        metricsDao.insertMetric(metric)
        try {
            FirebaseManager.syncMetric(metric)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }"""
content = content.replace(old_save_metrics, new_save_metrics)

# Add syncProfile to saveProfile
old_save_profile = """    suspend fun saveProfile(profile: UserProfile) {
        userDao.insertProfile(profile)
    }"""
new_save_profile = """    suspend fun saveProfile(profile: UserProfile) {
        userDao.insertProfile(profile)
        try {
            FirebaseManager.syncProfile(profile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }"""
content = content.replace(old_save_profile, new_save_profile)

# Add syncFoodLog to insertFoodLog
old_insert_food_log = """    suspend fun insertFoodLog(log: com.example.data.local.FoodLog) {
        metricsDao.insertFoodLog(log)
    }"""
new_insert_food_log = """    suspend fun insertFoodLog(log: com.example.data.local.FoodLog) {
        metricsDao.insertFoodLog(log)
        try {
            FirebaseManager.syncFoodLog(log)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }"""
content = content.replace(old_insert_food_log, new_insert_food_log)

with open('app/src/main/java/com/example/data/repository/AppRepository.kt', 'w') as f:
    f.write(content)
