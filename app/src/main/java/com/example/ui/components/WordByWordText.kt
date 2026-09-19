package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.data.model.WordDto
import com.example.data.model.appendStyledWaqfText
import com.example.ui.theme.PrimaryGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordByWordText(
    words: List<WordDto>,
    modifier: Modifier = Modifier,
    ayahNumber: Int = 1,
    arabicFontSize: Float = 28f,
    arabicFont: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily.Default,
    arabicFontName: String = "Me Quran",
    showTransliteration: Boolean = false,
    onWordPlay: ((String) -> Unit)? = null,
    currentPlayingWordUrl: String? = null,
    surahNumber: Int = 1,
    ayahNumberInSurah: Int = 1
) {
    

    Column(modifier = modifier.fillMaxWidth()) {


        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                words.forEach { word ->
                    val isEnd = word.charTypeName == "end"
                    if (isEnd) {
                        // Ayah marker
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AyahNumberCircle(
                                number = ayahNumber,
                                size = (arabicFontSize * 0.95f).coerceIn(28f, 36f).dp,
                                backgroundColor = Color.Transparent,
                                borderColor = MaterialTheme.colorScheme.onSurface,
                                textColor = MaterialTheme.colorScheme.onSurface,
                                fontSize = (arabicFontSize * 0.45f).coerceIn(11f, 16f).sp
                            )
                        }
                    } else {
                        val rawAudioUrl = word.audioUrl
                        val wordUrl = if (!rawAudioUrl.isNullOrEmpty()) {
                            if (rawAudioUrl.startsWith("http://") || rawAudioUrl.startsWith("https://")) {
                                rawAudioUrl
                            } else if (rawAudioUrl.startsWith("//")) {
                                "https:$rawAudioUrl"
                            } else {
                                "https://audio.qurancdn.com/$rawAudioUrl"
                            }
                        } else {
                            String.format(
                                java.util.Locale.US,
                                "https://audio.qurancdn.com/wbw/%03d_%03d_%03d.mp3",
                                surahNumber,
                                ayahNumberInSurah,
                                word.position
                            )
                        }
                        val isHighlighted = currentPlayingWordUrl == wordUrl
                        var showPopup by remember { mutableStateOf(false) }

                        Box {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        showPopup = true
                                        onWordPlay?.invoke(wordUrl)
                                    }
                                    .background(
                                        color = if (isHighlighted) PrimaryGreen.copy(alpha = 0.15f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isHighlighted) 1.dp else 0.dp,
                                        color = if (isHighlighted) PrimaryGreen.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                // Arabic text
                                Text(
                                    text = androidx.compose.ui.text.buildAnnotatedString {
                                        appendStyledWaqfText(word.textUthmani ?: "", arabicFontSize, true, arabicFontName)
                                    },
                                    fontSize = arabicFontSize.sp,
                                    fontFamily = arabicFont,
                                    color = if (isHighlighted) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                // Word Translation (Bengali)
                                Text(
                                    text = word.translation?.text ?: "",
                                    fontSize = 11.sp,
                                    fontFamily = com.example.ui.theme.LocalBengaliFont.current,
                                    color = if (isHighlighted) PrimaryGreen.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )

                                // Word Transliteration (Toggle-able)
                                if (showTransliteration) {
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = word.transliteration?.text ?: "",
                                        fontSize = 10.sp,
                                        color = PrimaryGreen.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                            }

                            // Tooltip popup
                            if (showPopup) {
                                Popup(
                                    alignment = Alignment.BottomCenter,
                                    offset = IntOffset(0, -110),
                                    onDismissRequest = { showPopup = false },
                                    properties = PopupProperties(
                                        focusable = true,
                                        dismissOnClickOutside = true
                                    )
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .widthIn(max = 220.dp)
                                            .padding(4.dp)
                                            .border(1.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = word.textUthmani ?: "",
                                                fontSize = (arabicFontSize * 0.95f).sp,
                                                fontFamily = arabicFont,
                                                color = PrimaryGreen,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "উচ্চারণ: ${word.transliteration?.text ?: "N/A"}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "অর্থ: ${word.translation?.text ?: "N/A"}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension to scale Switch in Compose
private fun Int.toArabicDigits(): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return this.toString().map { if (it.isDigit()) arabicDigits[it - '0'] else it }.joinToString("")
}
