package com.example.util.ai

object MedicalNotes {
    const val DOCTOR_LINE = "Check with your doctor first if you take diabetes or blood pressure medicine."

    val PROMPT_RULE: String = "SAFETY LINE: If your answer recommends fasting of any kind (intermittent, water, 24 or 36 hour, one meal a day), apple cider vinegar or any vinegar or cinnamon shot, a very low carbohydrate or ketogenic plan, or if the user context mentions diabetes, blood pressure or medicine, end the answer with this exact sentence in italics on its own line: *Check with your doctor first if you take diabetes or blood pressure medicine.*"

    private val KEYWORD_REGEXES = listOf(
        Regex("""\bfasting\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:intermittent|water|dry|sardine|extended|24|36|48|72)[- ]?(?:hour[- ]?)?fast(?:ing)?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bomad\b""", RegexOption.IGNORE_CASE),
        Regex("""\bone meal a day\b""", RegexOption.IGNORE_CASE),
        Regex("""vinegar""", RegexOption.IGNORE_CASE),
        Regex("""\bacv\b""", RegexOption.IGNORE_CASE),
        Regex("""cinnamon shot""", RegexOption.IGNORE_CASE),
        Regex("""diabet""", RegexOption.IGNORE_CASE),
        Regex("""insulin""", RegexOption.IGNORE_CASE),
        Regex("""blood pressure""", RegexOption.IGNORE_CASE),
        Regex("""hypertension""", RegexOption.IGNORE_CASE),
        Regex("""\bmedicine\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmedication\b""", RegexOption.IGNORE_CASE)
    )

    fun needsDoctorNote(vararg texts: String?): Boolean {
        for (text in texts) {
            if (text.isNullOrBlank()) continue
            if (KEYWORD_REGEXES.any { it.containsMatchIn(text) }) {
                return true
            }
        }
        return false
    }

    fun appendDoctorNote(
        answer: String,
        userText: String = "",
        beforeFinalMarker: Boolean = true,
        force: Boolean = false
    ): String {
        if (answer.isBlank()) return answer
        val lowerAnswer = answer.trim()
        if (lowerAnswer.startsWith("Error", ignoreCase = true) ||
            lowerAnswer.startsWith("Could not", ignoreCase = true) ||
            lowerAnswer.startsWith("Sorry", ignoreCase = true) ||
            lowerAnswer.contains("currently busy", ignoreCase = true)
        ) {
            return answer
        }
        if (answer.contains(DOCTOR_LINE)) {
            return answer
        }

        val shouldAppend = force || needsDoctorNote(userText, answer)
        if (!shouldAppend) {
            return answer
        }

        val formattedNote = "*$DOCTOR_LINE*"

        if (beforeFinalMarker) {
            val lines = answer.lines().toMutableList()
            var lastNonBlankIdx = -1
            for (i in lines.indices.reversed()) {
                if (lines[i].isNotBlank()) {
                    lastNonBlankIdx = i
                    break
                }
            }
            if (lastNonBlankIdx != -1 && lines[lastNonBlankIdx].trim().startsWith("YOUTUBE_SEARCH:")) {
                val markerLine = lines[lastNonBlankIdx]
                lines.removeAt(lastNonBlankIdx)
                val remaining = lines.joinToString("\n").trimEnd()
                return "$remaining\n\n$formattedNote\n\n$markerLine"
            }
        }

        return answer.trimEnd() + "\n\n$formattedNote"
    }
}
