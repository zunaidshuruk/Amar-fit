import re

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'r') as f:
    content = f.read()

# Replace saveProfile
old_save_profile = """
    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            // Initialize today's metrics if not exist
            if (todayMetrics.value == null) {
                repository.saveMetrics(DailyMetric(date = todayDateString))
            }
        }
    }
"""

new_save_profile = """
    suspend fun saveProfile(profile: UserProfile): Boolean {
        return try {
            repository.saveUserProfile(profile)
            // Initialize today's metrics if not exist
            if (todayMetrics.value == null) {
                repository.saveMetrics(DailyMetric(date = todayDateString))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
"""

content = content.replace(old_save_profile.strip(), new_save_profile.strip())

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'w') as f:
    f.write(content)
