package com.example.util.ai

data class BenchmarkResult(
    val title: String,
    val firstTokenMs: Long,
    val totalMs: Long,
    val outputChars: Int,
    val approxTokensPerSecond: Double,
    val output: String
)

object OnDeviceBenchmark {

    val BENCHMARK_PROMPTS: List<Pair<String, String>> = listOf(
        Pair(
            "Heart rate",
            "Explain what a resting heart rate of 62 bpm means for a healthy adult, in two sentences."
        ),
        Pair(
            "Daily tips",
            "I slept 5.5 hours and walked 4,000 steps today. Give me 3 short tips for tomorrow."
        ),
        Pair(
            "Glucose range",
            "What is a healthy blood glucose range two hours after a meal? Give it in both mmol/L and mg/dL."
        ),
        Pair(
            "Bangla",
            "আমি ওজন কমাতে চাই। আজ আমি কী কী খেতে পারি? সংক্ষেপে বাংলায় বলো।"
        ),
        Pair(
            "Workout plan",
            "Create a 3-day beginner home workout plan with no equipment. Keep it short."
        ),
        Pair(
            "Food JSON",
            "Return ONLY a JSON array, no other text. Each item: {\"name\": string, \"mealType\": \"Breakfast\"|\"Lunch\"|\"Dinner\"|\"Snack\", \"calories\": number}. Message: i ate 2 eggs 3 parata 1 bowl vegetables as breakfast, 1 guava, 1 pineapple, 2 cups of sugar milk coffee as lunch, 1 roti with egg and potato vaji as snacks, 2 chocolates (1 cudberry pulse, 1 five star both mini size) as snacks and for dinner I had 3 pcs of roti with vegetables, 1 boiled egg, 1/2 sweet potato"
        ),
        Pair(
            "Diet chart",
            "Give a one-day Bangladeshi diet plan for 1800 kcal as a table with meal, food and calories."
        ),
        Pair(
            "Calories",
            "How many calories are in 2 boiled eggs and 3 plain parathas? Show the arithmetic."
        ),
        Pair(
            "Summary",
            "Summarise in two sentences: Regular moderate exercise lowers blood pressure, improves sleep and mood, helps control weight, and reduces the risk of heart disease and type 2 diabetes. Adults should aim for at least 150 minutes of moderate activity per week, spread across several days."
        ),
        Pair(
            "Safety",
            "I have sudden chest pain and my left arm feels numb. What should I do?"
        )
    )

    fun calculateTokensPerSecond(outputChars: Int, firstTokenMs: Long, totalMs: Long): Double {
        val decodingDurationMs = totalMs - firstTokenMs
        if (decodingDurationMs <= 0) return 0.0
        val approxTokens = outputChars / 4.0
        return (approxTokens / decodingDurationMs.toDouble()) * 1000.0
    }

    fun buildReport(
        deviceInfo: String,
        settings: String,
        loadResult: LoadResult?,
        results: List<BenchmarkResult>,
        memoryNote: String,
        batteryNote: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=========================================")
        sb.appendLine("       KardIQ Offline AI Benchmark       ")
        sb.appendLine("=========================================")
        sb.appendLine("Device Info: $deviceInfo")
        sb.appendLine("Settings: $settings")
        if (loadResult != null) {
            sb.appendLine("Active Backend: ${loadResult.backendName} (Load Time: ${loadResult.loadMillis} ms)")
            if (loadResult.error != null) {
                sb.appendLine("Load Error/Fallback note: ${loadResult.error}")
            }
        } else {
            sb.appendLine("Active Backend: Not loaded")
        }
        sb.appendLine("Memory (PSS): $memoryNote")
        sb.appendLine("Battery & Thermal: $batteryNote")
        sb.appendLine("Prompts Evaluated: ${results.size} / ${BENCHMARK_PROMPTS.size}")
        sb.appendLine("-----------------------------------------")

        if (results.isNotEmpty()) {
            val validTps = results.map { it.approxTokensPerSecond }.filter { it > 0.0 }
            if (validTps.isNotEmpty()) {
                val avgTps = validTps.average()
                val avgFirstToken = results.map { it.firstTokenMs }.average()
                sb.appendLine(String.format(java.util.Locale.US, "Summary: Avg TTFT = %.0f ms | Avg Speed = %.1f tokens/s", avgFirstToken, avgTps))
                sb.appendLine("-----------------------------------------")
            }
        }

        results.forEachIndexed { index, item ->
            sb.appendLine("[#${index + 1}] ${item.title}")
            sb.appendLine("TTFT (First Token): ${item.firstTokenMs} ms | Total Time: ${item.totalMs} ms")
            sb.appendLine(String.format(java.util.Locale.US, "Output: %d chars (~%.0f tokens) | Speed: %.1f tokens/s", item.outputChars, item.outputChars / 4.0, item.approxTokensPerSecond))
            sb.appendLine("Response:")
            sb.appendLine(item.output)
            sb.appendLine("-----------------------------------------")
        }

        return sb.toString()
    }
}
