package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.amiriFont

/**
 * Checks whether a given string is predominantly Arabic text (Quran verse, Hadith matn, Dua, etc.).
 */
fun isArabicText(text: String): Boolean {
    // If text contains Bengali characters, it's a Bengali line (e.g. reference with Arabic name in brackets)
    if (text.any { it.code in 0x0980..0x09FF }) {
        return false
    }

    val clean = text.replace(Regex("[\\s\\d\\p{Punct}«»\"'\\-:\n]"), "")
    if (clean.isEmpty()) return false
    var arabicCharCount = 0
    var totalLetters = 0
    for (char in clean) {
        val code = char.code
        if (code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0x08A0..0x08FF || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF) {
            arabicCharCount++
        }
        if (Character.isLetter(char) || Character.isLetterOrDigit(char)) {
            totalLetters++
        }
    }
    return totalLetters > 0 && (arabicCharCount.toFloat() / totalLetters.toFloat()) > 0.65f
}

private val URL_REGEX = Regex("(https?://[\\w-]+(\\.[\\w-]+)+(/[^\\s]*)?|t\\.me/[^\\s]+)")

/**
 * Builds an AnnotatedString for a line with clickable web and telegram links.
 */
private fun buildAnnotatedTextWithLinks(
    text: String,
    baseColor: Color,
    linkColor: Color = Color(0xFF1E88E5)
): AnnotatedString {
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        val matches = URL_REGEX.findAll(text)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            // Append text preceding the URL
            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }

            // Append the URL with link annotation and style
            val rawUrl = match.value
            val fullUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                rawUrl
            } else {
                "https://$rawUrl"
            }

            pushStringAnnotation(tag = "URL", annotation = fullUrl)
            pushStyle(
                SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            )
            append(rawUrl)
            pop()
            pop()

            lastIndex = end
        }

        // Append remaining text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    return annotatedString
}

/**
 * Renders multi-line text with intelligent RTL support for Arabic blocks,
 * select & copy capability, and clickable URLs (e.g. t.me, https://...).
 */
@Composable
fun FormattedIslamicText(
    text: String,
    modifier: Modifier = Modifier,
    baseFontSize: TextUnit = 14.sp,
    baseColor: Color = MaterialTheme.colorScheme.onSurface,
    arabicColor: Color = MaterialTheme.colorScheme.onSurface,
    arabicFontSize: TextUnit = 20.sp,
    arabicLineHeight: TextUnit = 36.sp
) {
    val context = LocalContext.current
    val lines = text.split("\n")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
            } else if (isArabicText(line)) {
                // Check if the line is Bismillah to center it
                val cleanForBismillah = line.replace(Regex("[\\s\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
                val isBismillah = cleanForBismillah == "بسماللهالرحمنالرحيم" || cleanForBismillah == "بسمٱللهٱلرحمنٱلرحيم"
                
                // Arabic text line -> Render in RTL (Right-To-Left format, aligned right from right side)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = line,
                            fontSize = arabicFontSize,
                            fontFamily = amiriFont,
                            fontWeight = FontWeight.Medium,
                            color = arabicColor,
                            lineHeight = arabicLineHeight,
                            textAlign = if (isBismillah) TextAlign.Center else TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            } else {
                // Non-Arabic (Bengali/English) line -> Render in LTR format
                val isHeader = line.startsWith("✨") || line.startsWith("📖") || line.startsWith("📜") || line.startsWith("•")
                val isBold = isHeader || line.endsWith(":")
                val finalTextColor = if (isHeader) Color(0xFF10B981) else baseColor
                val finalFontSize = if (isHeader) (baseFontSize.value + 1.5f).sp else baseFontSize

                val annotatedText = buildAnnotatedTextWithLinks(
                    text = rawLine,
                    baseColor = finalTextColor,
                    linkColor = Color(0xFF2563EB)
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val hasLinks = annotatedText.getStringAnnotations(tag = "URL", start = 0, end = annotatedText.length).isNotEmpty()
                    if (hasLinks) {
                        ClickableText(
                            text = annotatedText,
                            style = TextStyle(
                                fontSize = finalFontSize,
                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                color = finalTextColor,
                                lineHeight = (baseFontSize.value * 1.6f).sp,
                                textAlign = TextAlign.Left
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "লিঙ্কটি খোলা যাচ্ছে না", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        )
                    } else {
                        Text(
                            text = rawLine,
                            fontSize = finalFontSize,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            color = finalTextColor,
                            lineHeight = (baseFontSize.value * 1.6f).sp,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
