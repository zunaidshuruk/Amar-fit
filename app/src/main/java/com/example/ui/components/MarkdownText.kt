package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Red500

private sealed class MarkdownBlock {
    data class Text(val lines: List<String>) : MarkdownBlock()
    data class Table(val lines: List<String>) : MarkdownBlock()
    object Rule : MarkdownBlock()
    data class Video(val query: String) : MarkdownBlock()
}

private val separatorRegex = Regex("^:?-{2,}:?$")

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    val textColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    val context = LocalContext.current

    Column(modifier = modifier) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Text -> {
                    Text(
                        text = buildMarkdownAnnotated(block.lines, fontSize),
                        color = color,
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    )
                }
                is MarkdownBlock.Table -> {
                    RenderTable(
                        block = block,
                        textColor = textColor,
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    )
                }
                is MarkdownBlock.Rule -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                }
                is MarkdownBlock.Video -> {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(block.query)}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red500,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Watch",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Watch: ${block.query}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderTable(
    block: MarkdownBlock.Table,
    textColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit
) {
    val parsedRows = mutableListOf<List<String>>()
    for (line in block.lines) {
        val cells = parseRowCells(line)
        if (cells.isEmpty()) continue
        if (cells.all { separatorRegex.matches(it) }) continue
        parsedRows.add(cells)
    }

    if (parsedRows.isEmpty()) return

    if (parsedRows.size == 1) {
        Text(
            text = buildInlineBoldAnnotated(parsedRows[0].joinToString("  •  ")),
            color = textColor,
            fontSize = fontSize,
            lineHeight = lineHeight
        )
        return
    }

    val headerRow = parsedRows[0]
    val bodyRows = parsedRows.subList(1, parsedRows.size)

    for (row in bodyRows) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            val firstCell = row.getOrNull(0) ?: ""
            if (firstCell.isNotBlank()) {
                val titleAnnotated = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInlineBold(this, firstCell)
                    }
                }
                Text(
                    text = titleAnnotated,
                    color = textColor,
                    fontSize = fontSize,
                    lineHeight = lineHeight
                )
            }
            for (i in 1 until row.size) {
                val cell = row[i]
                if (cell.isNotBlank()) {
                    val lineAnnotated = buildAnnotatedString {
                        if (i < headerRow.size && headerRow[i].isNotBlank()) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                appendInlineBold(this, headerRow[i])
                            }
                            append(": ")
                        }
                        appendInlineBold(this, cell)
                    }
                    Text(
                        text = lineAnnotated,
                        color = textColor,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val lines = text.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    var currentTextLines = mutableListOf<String>()
    var currentTableLines = mutableListOf<String>()
    var inTable = false

    fun flushText() {
        if (currentTextLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Text(currentTextLines.toList()))
            currentTextLines.clear()
        }
    }

    fun flushTable() {
        if (currentTableLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Table(currentTableLines.toList()))
            currentTableLines.clear()
            inTable = false
        }
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (isRuleLine(line)) {
            flushText()
            flushTable()
            blocks.add(MarkdownBlock.Rule)
            i++
        } else if (isVideoLine(line)) {
            flushText()
            flushTable()
            val trimmedStart = line.trimStart()
            var query = trimmedStart.substringAfter("YOUTUBE_SEARCH:").trim()
            if (query.startsWith("[") && query.endsWith("]") && query.length >= 2) {
                query = query.substring(1, query.length - 1).trim()
            }
            if (query.isNotBlank()) {
                blocks.add(MarkdownBlock.Video(query))
            }
            i++
        } else if (isTableLine(i, lines, inTable)) {
            flushText()
            inTable = true
            currentTableLines.add(line)
            i++
        } else {
            flushTable()
            currentTextLines.add(line)
            i++
        }
    }
    flushText()
    flushTable()
    return blocks
}

private fun isRuleLine(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.length < 3) return false
    val first = trimmed[0]
    if (first != '-' && first != '*' && first != '_') return false
    return trimmed.all { it == first }
}

private fun isVideoLine(line: String): Boolean {
    return line.trimStart().startsWith("YOUTUBE_SEARCH:")
}

private fun isSeparatorRow(line: String): Boolean {
    val cells = parseRowCells(line)
    return cells.isNotEmpty() && cells.all { separatorRegex.matches(it) }
}

private fun isTableLine(index: Int, lines: List<String>, inTable: Boolean): Boolean {
    val line = lines[index]
    if (isRuleLine(line) || isVideoLine(line)) return false
    val trimmed = line.trim()
    if (trimmed.startsWith("|")) return true
    if (!trimmed.contains("|")) return false
    val cells = parseRowCells(trimmed)
    if (cells.size < 2) return false
    if (inTable) return true
    if (index + 1 < lines.size) {
        val next = lines[index + 1]
        val nextTrimmed = next.trim()
        if (nextTrimmed.startsWith("|") || isSeparatorRow(nextTrimmed)) return true
        val nextCells = parseRowCells(nextTrimmed)
        if (nextCells.size >= 2 && !isRuleLine(next) && !isVideoLine(next)) return true
    }
    return false
}

private fun parseRowCells(line: String): List<String> {
    val rawCells = line.split("|").map { it.trim() }
    if (rawCells.isEmpty()) return emptyList()
    var start = 0
    var end = rawCells.size
    if (start < end && rawCells[start].isEmpty()) {
        start++
    }
    if (end > start && rawCells[end - 1].isEmpty()) {
        end--
    }
    return if (start < end) rawCells.subList(start, end) else emptyList()
}

private fun appendInlineBold(builder: AnnotatedString.Builder, text: String) {
    var currentIndex = 0
    while (currentIndex < text.length) {
        val boldStart = text.indexOf("**", currentIndex)
        if (boldStart != -1) {
            val boldEnd = text.indexOf("**", boldStart + 2)
            if (boldEnd != -1) {
                builder.append(text.substring(currentIndex, boldStart))
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(boldStart + 2, boldEnd))
                }
                currentIndex = boldEnd + 2
            } else {
                builder.append(text.substring(currentIndex))
                break
            }
        } else {
            builder.append(text.substring(currentIndex))
            break
        }
    }
}

private fun buildInlineBoldAnnotated(text: String): AnnotatedString {
    return buildAnnotatedString {
        appendInlineBold(this, text)
    }
}

private fun buildMarkdownAnnotated(lines: List<String>, fontSize: TextUnit): AnnotatedString {
    return buildAnnotatedString {
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
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (fontSize != TextUnit.Unspecified) fontSize * 1.2f else 18.sp
                )
            } else null

            if (style != null) {
                pushStyle(style)
            }

            appendInlineBold(this, line)

            if (style != null) {
                pop()
            }

            if (i < lines.size - 1) {
                append("\n")
            }
        }
    }
}
