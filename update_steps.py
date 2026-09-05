import re

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'r') as f:
    content = f.read()

set_steps = """    fun setSteps(steps: Int) {
        viewModelScope.launch {
            val current = todayMetrics.value ?: DailyMetric(date = todayDateString)
            val updated = current.copy(steps = steps)
            repository.saveMetrics(updated)
            repository.checkAndAwardBadges(updated)
        }
    }
"""

if "fun setSteps" not in content:
    content = content.replace("fun addSteps(steps: Int) {", set_steps + "\n    fun addSteps(steps: Int) {")

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'w') as f:
    f.write(content)
