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
        if (topics.isNotEmpty()) {
            val protocolLines = mutableListOf<String>()
            val seenHits = mutableSetOf<String>()
            for (topic in topics) {
                val hits = KnowledgeRetriever.search(topic.condition, listOf(topic), 2)
                for (hit in hits) {
                    val key = "${hit.topic.condition}:${hit.topic.habit}"
                    if (seenHits.add(key)) {
                        protocolLines.add("- ${hit.topic.condition}: ${hit.topic.habit}")
                    }
                }
            }
            if (protocolLines.isNotEmpty()) {
                sb.append("## Support from the KardIQ protocol\n")
                for (pLine in protocolLines) {
                    sb.append("$pLine\n")
                }
                sb.append("\n")
            }
        }

        var result = sb.toString().trimEnd()

        val needsDoc = MedicalNotes.needsDoctorNote(result, dossier.profileLine, dossier.medicalLines.joinToString(" ")) ||
                dossier.findings.any { it.severity == 3 }

        if (needsDoc && !result.contains(MedicalNotes.DOCTOR_LINE)) {
            result += "\n\n*${MedicalNotes.DOCTOR_LINE}*"
        }

        return result
    }
}
