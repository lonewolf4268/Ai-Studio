package com.aistudio.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Compose Markdown renderer supporting bold, italics, headers (#, ##, ###),
 * bullet lists, numbered lists, inline code (`code`), and links.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 14.sp
) {
    val lines = markdown.lines()

    Column(modifier = modifier) {
        var isInList = false

        for (line in lines) {
            val trimmed = line.trim()

            when {
                // Headers
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.substring(2), textColor),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (fontSize.value + 4).sp
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.substring(3), textColor),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (fontSize.value + 2).sp
                        ),
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.substring(4), textColor),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                // Bullet List
                trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "• ",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize
                        )
                        Text(
                            text = parseMarkdownInline(trimmed.substring(2), textColor),
                            fontSize = fontSize,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                // Numbered List (e.g. "1. ")
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val dotIdx = trimmed.indexOf('.')
                    val num = trimmed.substring(0, dotIdx + 1)
                    val content = trimmed.substring(dotIdx + 1).trim()

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "$num ",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize
                        )
                        Text(
                            text = parseMarkdownInline(content, textColor),
                            fontSize = fontSize,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                // Regular Paragraph line
                else -> {
                    if (trimmed.isNotBlank()) {
                        Text(
                            text = parseMarkdownInline(trimmed, textColor),
                            fontSize = fontSize,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Parses inline markdown styling: **bold**, *italic*, `code`, and [links](url).
 */
private fun parseMarkdownInline(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            when {
                // Bold (**text**)
                i + 1 < len && text[i] == '*' && text[i + 1] == '*' -> {
                    val endIdx = text.indexOf("**", i + 2)
                    if (endIdx != -1) {
                        val boldText = text.substring(i + 2, endIdx)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                            append(boldText)
                        }
                        i = endIdx + 2
                        continue
                    }
                }
                // Italic (*text*)
                text[i] == '*' -> {
                    val endIdx = text.indexOf('*', i + 1)
                    if (endIdx != -1) {
                        val italicText = text.substring(i + 1, endIdx)
                        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = defaultColor)) {
                            append(italicText)
                        }
                        i = endIdx + 1
                        continue
                    }
                }
                // Inline Code (`code`)
                text[i] == '`' -> {
                    val endIdx = text.indexOf('`', i + 1)
                    if (endIdx != -1) {
                        val codeText = text.substring(i + 1, endIdx)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x33808080),
                                color = defaultColor
                            )
                        ) {
                            append(" $codeText ")
                        }
                        i = endIdx + 1
                        continue
                    }
                }
                // Links ([text](url))
                text[i] == '[' -> {
                    val closeBracket = text.indexOf(']', i + 1)
                    val openParen = text.indexOf('(', closeBracket)
                    val closeParen = text.indexOf(')', openParen)

                    if (closeBracket != -1 && openParen == closeBracket + 1 && closeParen != -1) {
                        val label = text.substring(i + 1, closeBracket)
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF3B82F6),
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(label)
                        }
                        i = closeParen + 1
                        continue
                    }
                }
            }

            append(text[i])
            i++
        }
    }
}
