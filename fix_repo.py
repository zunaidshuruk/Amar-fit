import re

with open('app/src/main/java/com/example/data/repository/AppRepository.kt', 'r') as f:
    content = f.read()

sync_code = """
    suspend fun syncDataOnLogin() {
        FirebaseManager.pullDataOnLogin(userDao, metricsDao)
    }
"""

if "suspend fun syncDataOnLogin()" not in content:
    content = content.replace("class AppRepository(\n    private val userDao: UserDao,\n    private val metricsDao: MetricsDao,\n    private val savedDietChartDao: com.example.data.local.SavedDietChartDao\n) {", "class AppRepository(\n    private val userDao: UserDao,\n    private val metricsDao: MetricsDao,\n    private val savedDietChartDao: com.example.data.local.SavedDietChartDao\n) {\n" + sync_code)
    with open('app/src/main/java/com/example/data/repository/AppRepository.kt', 'w') as f:
        f.write(content)
