package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Red500

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val lines: List<String>) : MarkdownBlock()
    data class Bullets(val items: List<Pair<Int, String>>) : MarkdownBlock()      // (indent level 0..2, text)
    data class Numbered(val items: List<Pair<String, String>>) : MarkdownBlock()  // (number as written, text)
    data class Callout(val lines: List<String>) : MarkdownBlock()
    data class Table(val lines: List<String>) : MarkdownBlock()
    object Rule : MarkdownBlock()
    data class Video(val query: String) : MarkdownBlock()
}

private val separatorRegex = Regex("^:?-{2,}:?$")
private val headingRegex = Regex("""^\s{0,3}(#{1,6})\s+(.+)$""")
private val bulletRegex = Regex("""^(\s*)[*\-•]\s+(.+)$""")
private val numberedRegex = Regex("""^\s*(\d{1,2})[.)]\s+(.+)$""")
private val quoteRegex = Regex("""^\s*>\s?(.*)$""")
private val calloutLabelRegex = Regex("""^\*\*(.+?)\*\*:?\s*(.*)$""")

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

    val baseSize = if (fontSize != TextUnit.Unspecified) fontSize else 16.sp
    val bodyLineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else baseSize * 1.45f
    val accent = MaterialTheme.colorScheme.primary
    val codeBg = textColor.copy(alpha = 0.12f)

    Column(modifier = modifier) {
        for ((index, block) in blocks.withIndex()) {
            val topPadding = when {
                index == 0 -> 0.dp
                block is MarkdownBlock.Table || block is MarkdownBlock.Rule || block is MarkdownBlock.Video -> 0.dp
                blocks[index - 1] is MarkdownBlock.Heading -> 6.dp
                block is MarkdownBlock.Heading -> 14.dp
                block is MarkdownBlock.Callout -> 12.dp
                else -> 8.dp
            }

            when (block) {
                is MarkdownBlock.Heading -> {
                    Row(
                        modifier = Modifier
                            .padding(top = topPadding)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildInlineAnnotated(block.text, codeBg),
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = baseSize * (if (block.level == 1) 1.25f else 1.1f),
                            lineHeight = bodyLineHeight
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineAnnotated(block.lines.joinToString("\n"), codeBg),
                        color = textColor,
                        fontSize = fontSize,
                        lineHeight = bodyLineHeight,
                        modifier = Modifier.padding(top = topPadding)
                    )
                }
                is MarkdownBlock.Bullets -> {
                    Column(modifier = Modifier.padding(top = topPadding)) {
                        block.items.forEachIndexed { itemIndex, (indent, itemText) ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(
                                    start = (indent * 16).dp,
                                    top = if (itemIndex == 0) 0.dp else 4.dp
                                )
                            ) {
                                Text(
                                    text = "•",
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontSize,
                                    modifier = Modifier.width(14.dp)
                                )
                                Text(
                                    text = buildInlineAnnotated(itemText, codeBg),
                                    color = textColor,
                                    fontSize = fontSize,
                                    lineHeight = bodyLineHeight,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                is MarkdownBlock.Numbered -> {
                    Column(modifier = Modifier.padding(top = topPadding)) {
                        block.items.forEachIndexed { itemIndex, (number, itemText) ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(top = if (itemIndex == 0) 0.dp else 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = number,
                                        color = accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = buildInlineAnnotated(itemText, codeBg),
                                    color = textColor,
                                    fontSize = fontSize,
                                    lineHeight = bodyLineHeight,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                is MarkdownBlock.Callout -> {
                    val nonBlankIndex = block.lines.indexOfFirst { it.isNotBlank() }
                    val labelMatch = if (nonBlankIndex != -1) {
                        calloutLabelRegex.matchEntire(block.lines[nonBlankIndex].trim())
                    } else null

                    val label: String?
                    val bodyLines = mutableListOf<String>()

                    if (labelMatch != null) {
                        label = labelMatch.groupValues[1].removeSuffix(":").trim()
                        val remainder = labelMatch.groupValues[2]
                        if (remainder.isNotBlank()) {
                            bodyLines.add(remainder)
                        }
                        for (j in (nonBlankIndex + 1) until block.lines.size) {
                            bodyLines.add(block.lines[j])
                        }
                    } else {
                        label = null
                        bodyLines.addAll(block.lines)
                    }

                    val bodyText = bodyLines.joinToString("\n").trim()

                    Column(
                        modifier = Modifier
                            .padding(top = topPadding)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.10f))
                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (label != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                        }
                        if (bodyText.isNotBlank()) {
                            Text(
                                text = buildInlineAnnotated(bodyText, codeBg),
                                color = textColor,
                                fontSize = fontSize,
                                lineHeight = bodyLineHeight,
                                modifier = if (label != null) Modifier.padding(top = 2.dp) else Modifier
                            )
                        }
                    }
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

private enum class BlockKind {
    PARAGRAPH, BULLETS, NUMBERED, CALLOUT
}

private fun parseTextBlocks(lines: List<String>): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()

    var currentKind: BlockKind? = null
    val currentParagraph = mutableListOf<String>()
    val currentBullets = mutableListOf<Pair<Int, String>>()
    val currentNumbered = mutableListOf<Pair<String, String>>()
    val currentCallout = mutableListOf<String>()

    fun flush() {
        when (currentKind) {
            BlockKind.PARAGRAPH -> {
                if (currentParagraph.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(currentParagraph.toList()))
                    currentParagraph.clear()
                }
            }
            BlockKind.BULLETS -> {
                if (currentBullets.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Bullets(currentBullets.toList()))
                    currentBullets.clear()
                }
            }
            BlockKind.NUMBERED -> {
                if (currentNumbered.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Numbered(currentNumbered.toList()))
                    currentNumbered.clear()
                }
            }
            BlockKind.CALLOUT -> {
                if (currentCallout.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Callout(currentCallout.toList()))
                    currentCallout.clear()
                }
            }
            null -> {}
        }
        currentKind = null
    }

    for (line in lines) {
        if (line.trim().isEmpty()) {
            flush()
            continue
        }

        val headingMatch = headingRegex.matchEntire(line)
        if (headingMatch != null) {
            flush()
            val hashes = headingMatch.groupValues[1]
            val text = headingMatch.groupValues[2].trim()
            val level = minOf(hashes.length, 3)
            blocks.add(MarkdownBlock.Heading(level, text))
            continue
        }

        val trimmedStart = line.trimStart()
        val bulletMatch = if (!trimmedStart.startsWith("**")) bulletRegex.matchEntire(line) else null
        if (bulletMatch != null) {
            if (currentKind != BlockKind.BULLETS) {
                flush()
                currentKind = BlockKind.BULLETS
            }
            val leadingSpaces = bulletMatch.groupValues[1].length
            val indent = minOf(leadingSpaces / 2, 2)
            val text = bulletMatch.groupValues[2]
            currentBullets.add(Pair(indent, text))
            continue
        }

        val numberedMatch = numberedRegex.matchEntire(line)
        if (numberedMatch != null) {
            if (currentKind != BlockKind.NUMBERED) {
                flush()
                currentKind = BlockKind.NUMBERED
            }
            val num = numberedMatch.groupValues[1]
            val text = numberedMatch.groupValues[2]
            currentNumbered.add(Pair(num, text))
            continue
        }

        val quoteMatch = quoteRegex.matchEntire(line)
        if (quoteMatch != null) {
            if (currentKind != BlockKind.CALLOUT) {
                flush()
                currentKind = BlockKind.CALLOUT
            }
            val text = quoteMatch.groupValues[1]
            currentCallout.add(text)
            continue
        }

        if (currentKind != BlockKind.PARAGRAPH) {
            flush()
            currentKind = BlockKind.PARAGRAPH
        }
        currentParagraph.add(line)
    }

    flush()
    return blocks
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val lines = text.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    var currentTextLines = mutableListOf<String>()
    var currentTableLines = mutableListOf<String>()
    var inTable = false

    fun flushText() {
        if (currentTextLines.isNotEmpty()) {
            blocks.addAll(parseTextBlocks(currentTextLines))
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

private fun buildInlineAnnotated(text: String, codeBackground: Color): AnnotatedString {
    return buildAnnotatedString {
        appendInlineAnnotated(this, text, codeBackground)
    }
}

private fun appendInlineAnnotated(
    builder: AnnotatedString.Builder,
    text: String,
    codeBackground: Color
) {
    var i = 0
    while (i < text.length) {
        // 1. **bold**
        if (text.startsWith("**", i)) {
            val closeBold = text.indexOf("**", i + 2)
            if (closeBold != -1) {
                val inner = text.substring(i + 2, closeBold)
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInlineAnnotated(this, inner, codeBackground)
                }
                i = closeBold + 2
                continue
            }
        }

        // 2. `code`
        if (text[i] == '`') {
            val closeCode = text.indexOf('`', i + 1)
            if (closeCode != -1) {
                val inner = text.substring(i + 1, closeCode)
                builder.withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground
                    )
                ) {
                    append(inner)
                }
                i = closeCode + 1
                continue
            }
        }

        // 3. *italic*
        if (text[i] == '*') {
            val nextChar = text.getOrNull(i + 1)
            if (nextChar != null && nextChar != '*' && !nextChar.isWhitespace()) {
                var closeItalic = -1
                var search = i + 2
                while (search < text.length) {
                    val candidate = text.indexOf('*', search)
                    if (candidate == -1) break
                    val prevChar = text[candidate - 1]
                    val nextAfterCandidate = text.getOrNull(candidate + 1)
                    if (!prevChar.isWhitespace() && nextAfterCandidate != '*') {
                        closeItalic = candidate
                        break
                    }
                    search = candidate + 1
                }

                if (closeItalic != -1) {
                    val inner = text.substring(i + 1, closeItalic)
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInlineAnnotated(this, inner, codeBackground)
                    }
                    i = closeItalic + 1
                    continue
                }
            }
        }

        // Literal character
        builder.append(text[i])
        i++
    }
}
