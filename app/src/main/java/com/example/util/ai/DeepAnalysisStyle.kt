package com.example.util.ai

object DeepAnalysisStyle {
    val PROMPT_INSTRUCTIONS = """
        DEEP 30-DAY HEALTH ANALYSIS STYLE (override standard reply format for this request):
        - Provide a comprehensive, structured 30-day review based strictly on the Health Dossier and medical records provided.
        - Use the exact headings below in order:
          ## Snapshot
          ## What your data shows
          ## Likely drivers
          ## Your 7-day plan
          ## Support from the KardIQ protocol
        - Under each heading, use clear bullet points ("- ") beginning with bold key terms.
        - Maintain an objective, supportive tone. Never diagnose diseases, prescribe medications, or give clinical targets. Always frame insights around wellness and lifestyle.
        - Ground every observation in the exact numbers, trends, or medical records present in the dossier.
    """.trimIndent()
}
