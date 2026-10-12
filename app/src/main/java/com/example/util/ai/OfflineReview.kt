package com.example.util.ai

import com.example.data.local.CoachTopic

object OfflineReview {

    fun build(dossier: Dossier, topics: List<CoachTopic>): String {
        val sb = StringBuilder()
        sb.append("## Offline health review (computed on your phone)\n")
        sb.append("*Your last 30 days, from your own records. For the full AI analysis, connect to the internet and tap Deep review.*\n\n")

        // ## Snapshot
        sb.append("## Snapshot\n")
        val snapshotLines = mutableListOf<String>()
        for (section in dossier.sections) {
            val titleUpper = section.title.trim().uppercase()
            if (titleUpper == "PROFILE" || titleUpper == "MEDICAL RECORDS" || section.title.isBlank()) {
                continue
            }
            for (line in section.lines.take(2)) {
                if (!line.lowercase().contains("no records")) {
                    val cleanLine = if (line.startsWith("- ")) line.substring(2).trim() else line.trim()
                    if (cleanLine.isNotBlank()) {
                        snapshotLines.add(cleanLine)
                    }
                }
            }
        }
        if (snapshotLines.isEmpty()) {
            sb.append("- No records in the last 30 days yet. Log sleep, steps, food, glucose and blood pressure to get a review.\n")
        } else {
            for (line in snapshotLines) {
                sb.append("- $line\n")
            }
        }
        sb.append("\n")

        // ## What your data shows
        sb.append("## What your data shows\n")
        if (dossier.findings.isEmpty()) {
            sb.append("- Nothing stands out yet; keep logging.\n")
        } else {
            for (finding in dossier.findings) {
                val tagLower = finding.tag.lowercase()
                val titleLower = finding.title.lowercase()
                val label = when {
                    finding.severity == 3 -> "Important:"
                    finding.severity == 2 -> "Attention:"
                    tagLower.contains("goal_met") || tagLower.contains("good") || tagLower.contains("at or above") ||
                    titleLower.contains("goal_met") || titleLower.contains("good") || titleLower.contains("at or above") -> "Good news:"
                    else -> "Info:"
                }
                val t = finding.title.trim()
                val formattedTitle = if (t.endsWith(".")) t else "$t."
                sb.append("- **$label** $formattedTitle ${finding.evidence}\n")
            }
        }
        sb.append("\n")

        // ## Patterns
        if (dossier.patterns.isNotEmpty()) {
            sb.append("## Patterns\n")
            for (pattern in dossier.patterns) {
                val cleanPattern = if (pattern.startsWith("- ")) pattern.substring(2).trim() else pattern.trim()
                if (cleanPattern.isNotBlank()) {
                    sb.append("- $cleanPattern\n")
                }
            }
            sb.append("\n")
        }

        // ## Support from the KardIQ protocol
        val supportLines = mutableListOf<String>()
        if (topics.isNotEmpty()) {
            val eligible = dossier.findings.filter { it.topic != FindingTopic.DATA }
            val sortedEligible = eligible.sortedByDescending { it.severity }
            val sev23 = sortedEligible.filter { it.severity >= 2 }
            val sev1NotGood = sortedEligible.filter { it.severity == 1 && !isGoodNews(it) }

            val chosenFindings = if (sev23.size >= 2) {
                sev23.take(4)
            } else {
                (sev23 + sev1NotGood).take(4)
            }

            val usedHitsSet = mutableSetOf<String>()

            for (finding in chosenFindings) {
                if (supportLines.size >= 3) break
                val query = when (finding.topic) {
                    FindingTopic.SLEEP -> "sleep insomnia relax"
                    FindingTopic.GLUCOSE -> "sugar glucose insulin"
                    FindingTopic.BLOOD_PRESSURE -> "heart artery pressure"
                    FindingTopic.ACTIVITY -> "exercise walking"
                    FindingTopic.NUTRITION -> "weight fat sugar"
                    FindingTopic.HEART -> "heart stress"
                    FindingTopic.WEIGHT -> "weight fat"
                    FindingTopic.HYDRATION -> "drink water"
                    FindingTopic.DATA -> ""
                }
                if (query.isBlank()) continue

                val hits = KnowledgeRetriever.search(query, topics, 3)
                val bestHit = hits.firstOrNull { hit ->
                    val key = "${hit.topic.condition}:${hit.topic.habit}"
                    !usedHitsSet.contains(key)
                }

                if (bestHit != null) {
                    val key = "${bestHit.topic.condition}:${bestHit.topic.habit}"
                    usedHitsSet.add(key)
                    val name = KnowledgeRetriever.protocolNames(listOf(bestHit)).firstOrNull() ?: bestHit.topic.habit
                    val cleanTitle = finding.title.trim().removeSuffix(".")
                    val benefitsTrimmed = bestHit.topic.benefits.trim().take(200)
                    supportLines.add("- **$name** (for: $cleanTitle): $benefitsTrimmed")
                }
            }
        }

        if (supportLines.isNotEmpty()) {
            sb.append("## Support from the KardIQ protocol\n")
            sb.append("*General wellness suggestions from the lifestyle protocol, not medical advice.*\n")
            for (line in supportLines) {
                sb.append("$line\n")
            }
            sb.append("\n")
        }

        // ## Track and discuss
        sb.append("## Track and discuss\n")
        for (finding in dossier.findings) {
            if (finding.topic == FindingTopic.DATA) {
                val cleanTitle = finding.title.trim().removeSuffix(".")
                sb.append("- Add readings: $cleanTitle\n")
            }
        }
        for (finding in dossier.findings) {
            if (finding.severity == 3) {
                val cleanTitle = finding.title.trim().removeSuffix(".")
                sb.append("- Discuss with your doctor: $cleanTitle\n")
            }
        }
        sb.append("- Re-check these numbers again in a week.\n")

        val supportText = supportLines.joinToString(" ")
        val needsDoctorNote = MedicalNotes.needsDoctorNote(supportText) ||
                dossier.flags.any { it.equals("diabetes", ignoreCase = true) || it.equals("hypertension", ignoreCase = true) } ||
                dossier.findings.any { it.severity == 3 }

        var result = sb.toString().trimEnd()
        if (needsDoctorNote) {
            result += "\n\n*" + MedicalNotes.DOCTOR_LINE + "*"
        }
        return result
    }

    private fun isGoodNews(finding: Finding): Boolean {
        val tagLower = finding.tag.lowercase()
        val titleLower = finding.title.lowercase()
        return tagLower.contains("goal_met") || tagLower.contains("good") || tagLower.contains("at or above") ||
                titleLower.contains("goal_met") || titleLower.contains("good") || titleLower.contains("at or above")
    }
}
