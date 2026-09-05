import re

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'r') as f:
    content = f.read()

sleep_func = """    fun setSleep(hours: Float) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(sleepHours = hours)
            repository.saveMetrics(updated)
        }
    }
"""

if "fun setSleep(" not in content:
    content = content.replace('fun addWater(amountLiters: Float) {', sleep_func + '\n    fun addWater(amountLiters: Float) {')

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'w') as f:
    f.write(content)
