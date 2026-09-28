package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val annotatedString = buildAnnotatedString {
        val lines = text.split("\n")
        
        for (i in lines.indices) {
            var line = lines[i]
            var isHeader = false
            
            if (line.startsWith("### ")) {
                line = line.removePrefix("### ")
                isHeader = true
            } else if (line.startsWith("## ")) {
                line = line.removePrefix("## ")
                isHeader = true
            } else if (line.startsWith("# ")) {
                line = line.removePrefix("# ")
                isHeader = true
            }
            
            if (line.trimStart().startsWith("* ")) {
                line = line.replaceFirst("* ", "• ")
            } else if (line.trimStart().startsWith("- ")) {
                line = line.replaceFirst("- ", "• ")
            }
            
            val style = if (isHeader) {
                SpanStyle(fontWeight = FontWeight.Bold, fontSize = if (fontSize != TextUnit.Unspecified) fontSize * 1.2f else 18.sp)
            } else null
            
            if (style != null) {
                pushStyle(style)
            }
            
            var currentIndex = 0
            while (currentIndex < line.length) {
                val boldStart = line.indexOf("**", currentIndex)
                if (boldStart != -1) {
                    val boldEnd = line.indexOf("**", boldStart + 2)
                    if (boldEnd != -1) {
                        append(line.substring(currentIndex, boldStart))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(line.substring(boldStart + 2, boldEnd))
                        }
                        currentIndex = boldEnd + 2
                    } else {
                        append(line.substring(currentIndex))
                        break
                    }
                } else {
                    append(line.substring(currentIndex))
                    break
                }
            }
            
            if (style != null) {
                pop()
            }
            
            if (i < lines.size - 1) {
                append("\n")
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight
    )
}
