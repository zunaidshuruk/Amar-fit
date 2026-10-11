package com.example.util.ai

import com.example.data.local.CoachTopic

data class KnowledgeHit(val topic: CoachTopic, val score: Int)

object KnowledgeRetriever {
    const val DOCTOR_LINE = MedicalNotes.DOCTOR_LINE

    private val STOP_WORDS = setOf(
        "the", "a", "an", "to", "for", "of", "and", "or", "is", "are",
        "what", "should", "i", "my", "me", "do", "does", "how", "can",
        "in", "on", "with", "better", "best", "good"
    )

    private val SYNONYM_GROUPS = mapOf(
        "sleep" to setOf("sleep", "slept", "insomnia", "sleepless", "bedtime", "night", "nap", "melatonin", "magnesium", "relax", "rest", "ঘুম"),
        "drink" to setOf("drink", "tea", "tonic", "juice", "latte", "shot", "elixir", "coffee", "milk", "water", "shake", "smoothie", "পানীয়", "চা"),
        "sugar" to setOf("sugar", "glucose", "insulin", "diabetes", "diabetic", "carb", "carbs", "postprandial", "spike", "চিনি", "সুগার"),
        "weight" to setOf("weight", "fat", "visceral", "belly", "obesity", "lose", "slim", "ওজন"),
        "heart" to setOf("heart", "artery", "arterial", "cardio", "cardiovascular", "cholesterol", "pressure", "bp", "হার্ট"),
        "gut" to setOf("gut", "digestion", "bloating", "constipation", "fiber", "fibre", "leaky"),
        "fasting" to setOf("fast", "fasting", "intermittent", "omad", "autophagy", "রোজা", "উপবাস"),
        "energy" to setOf("energy", "tired", "fatigue", "alert", "alertness", "caffeine", "coffee"),
        "stress" to setOf("stress", "anxiety", "anxious", "vagus", "breathing", "calm"),
        "exercise" to setOf("exercise", "workout", "hiit", "walk", "walking", "yoga", "sprint", "zone"),
        "hormone" to setOf("hormone", "hormonal", "testosterone", "sexual")
    )

    fun search(query: String, topics: List<CoachTopic>, max: Int = 4): List<KnowledgeHit> {
        val lowerQuery = query.lowercase()
        val words = Regex("""[\p{L}\p{N}]+""").findAll(lowerQuery).map { it.value }.filter { it !in STOP_WORDS }.toList()
        if (words.isEmpty()) return emptyList()

        val wordGroupMap = mutableMapOf<String, Set<String>>()
        for (w in words) {
            var group = setOf(w)
            for ((_, synonyms) in SYNONYM_GROUPS) {
                if (w in synonyms) {
                    group = group + synonyms
                }
            }
            wordGroupMap[w] = group
        }

        val hits = mutableListOf<KnowledgeHit>()

        for (topic in topics) {
            val condLower = topic.condition.lowercase()
            val habitLower = topic.habit.lowercase()
            val benLower = topic.benefits.lowercase()

            var totalScore = 0

            for ((queryWord, group) in wordGroupMap) {
                var wordScore = 0

                val condHit = group.any { g -> isWordOrPrefixMatch(g, condLower) }
                if (condHit) {
                    wordScore += 3
                    if (isWordOrPrefixMatch(queryWord, condLower)) {
                        wordScore += 3
                    }
                }

                val habitHit = group.any { g -> isWordOrPrefixMatch(g, habitLower) }
                if (habitHit) {
                    wordScore += 2
                    if (isWordOrPrefixMatch(queryWord, habitLower)) {
                        wordScore += 2
                    }
                }

                val benHit = group.any { g -> isWordOrPrefixMatch(g, benLower) }
                if (benHit) {
                    wordScore += 1
                    if (isWordOrPrefixMatch(queryWord, benLower)) {
                        wordScore += 1
                    }
                }

                totalScore += wordScore
            }

            if (totalScore >= 2) {
                hits.add(KnowledgeHit(topic, totalScore))
            }
        }

        return hits.sortedByDescending { it.score }.take(max)
    }

    private fun isWordOrPrefixMatch(term: String, text: String): Boolean {
        if (term.isBlank() || text.isBlank()) return false
        val textWords = Regex("""[\p{L}\p{N}]+""").findAll(text).map { it.value }
        return textWords.any { textWord -> textWord == term || textWord.startsWith(term) }
    }

    fun formatBlock(hits: List<KnowledgeHit>): String {
        if (hits.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("KARDIQ PROTOCOL (entries from the app's lifestyle protocol; answer from these, name the entry you use, and do not invent recipes, ingredients or amounts):\n")
        hits.forEachIndexed { index, hit ->
            val num = index + 1
            val cond = hit.topic.condition
            val habitTrimmed = hit.topic.habit.take(400)
            val benTrimmed = hit.topic.benefits.take(220)
            sb.append("$num. [$cond] $habitTrimmed - $benTrimmed\n")
        }
        return sb.toString().trimEnd()
    }

    fun protocolNames(hits: List<KnowledgeHit>): List<String> {
        return hits.map { hit ->
            val habit = hit.topic.habit
            val idxColon = habit.indexOf(':')
            val idxParen = habit.indexOf('(')
            val cutIdx = listOf(idxColon, idxParen).filter { it >= 0 }.minOrNull() ?: habit.length
            habit.substring(0, cutIdx).trim().take(40)
        }
    }

    fun needsDoctorNote(hits: List<KnowledgeHit>, message: String): Boolean {
        val texts = mutableListOf<String?>()
        texts.add(message)
        for (hit in hits) {
            texts.add(hit.topic.condition)
            texts.add(hit.topic.habit)
        }
        return MedicalNotes.needsDoctorNote(*texts.toTypedArray())
    }
}
