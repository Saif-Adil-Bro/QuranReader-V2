package com.example.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.example.data.model.WAQF_CHARS
import com.example.ui.theme.TajweedColors

private fun String.toArabicNumerals(): String {
    val englishToArabic = mapOf(
        '0' to '٠', '1' to '١', '2' to '٢', '3' to '٣', '4' to '٤',
        '5' to '٥', '6' to '٦', '7' to '٧', '8' to '٨', '9' to '٩'
    )
    return this.map { englishToArabic[it] ?: it }.joinToString("")
}

private val COMBINING_DIACRITICS_REGEX = Regex("(</(?:tajweed|span)>)([\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06DC\\u06DF-\\u06E8\\u06EA-\\u06ED]+)")

fun sanitizeTajweedHtml(raw: String): String {
    var result = raw
    while (true) {
        val next = result.replace(COMBINING_DIACRITICS_REGEX, "$2$1")
        if (next == result) break
        result = next
    }
    return result
}

private fun AnnotatedString.Builder.appendTajweedWithWaqf(text: String, defaultColor: Color) {
    if (text.isEmpty()) return
    var lastIndex = 0
    for (i in text.indices) {
        val char = text[i]
        if (WAQF_CHARS.contains(char)) {
            if (i > lastIndex) {
                append(text.substring(lastIndex, i))
            }
            
            withStyle(
                style = SpanStyle(
                    fontSize = 0.85.em,
                    baselineShift = androidx.compose.ui.text.style.BaselineShift(0.0f)
                )
            ) {
                append(char.toString())
            }

            lastIndex = i + 1
        }
    }
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

fun parseTajweedText(raw: String, defaultColor: Color): AnnotatedString {
    val sanitizedRaw = sanitizeTajweedHtml(raw)
    return buildAnnotatedString {
        // Preprocess <span class=end>...</span> or <span class="end">...</span>
        val regexEnd = "<span\\s+class=['\"]?end['\"]?>([^<]+)</span>".toRegex()
        val preprocessed = sanitizedRaw.replace(regexEnd) { matchResult ->
            " <span class=\"end\">﴿${matchResult.groupValues[1].toArabicNumerals()}﴾</span> "
        }

        var currentIndex = 0
        while (currentIndex < preprocessed.length) {
            val nextTagStart = preprocessed.indexOf("<", currentIndex)
            if (nextTagStart == -1) {
                appendTajweedWithWaqf(preprocessed.substring(currentIndex), defaultColor)
                break
            }
            
            if (nextTagStart > currentIndex) {
                appendTajweedWithWaqf(preprocessed.substring(currentIndex, nextTagStart), defaultColor)
            }
            
            val nextTagEnd = preprocessed.indexOf(">", nextTagStart)
            if (nextTagEnd == -1) {
                appendTajweedWithWaqf(preprocessed.substring(nextTagStart), defaultColor)
                break
            }
            
            val tag = preprocessed.substring(nextTagStart + 1, nextTagEnd)
            
            if (tag.startsWith("/")) {
                if (tag.startsWith("/tajweed") || tag.startsWith("/span")) {
                    try {
                        pop()
                    } catch (e: Exception) {
                        // ignore if stack is empty
                    }
                }
            } else {
                if (tag.contains("class=")) {
                    val className = tag.substringAfter("class=").trim().substringBefore(" ").substringBefore(">").trim('\'', '"')
                    val color = if (className == "end") defaultColor else (TajweedColors[className] ?: defaultColor)
                    val fontFamily = if (className == "end") com.example.ui.theme.amiriFont else null
                    pushStyle(SpanStyle(color = color, fontFamily = fontFamily))
                }
            }
            currentIndex = nextTagEnd + 1
        }
    }
}

@Composable
fun TajweedText(
    rawTajweedText: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null
) {
    val annotatedString = parseTajweedText(rawTajweedText, color)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = annotatedString,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textAlign = textAlign
        )
    }
}
