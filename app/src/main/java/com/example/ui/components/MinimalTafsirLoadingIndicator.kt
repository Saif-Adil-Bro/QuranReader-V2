package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen

/**
 * A sleek, minimal, light-colored loading indicator for Tafsir and Translation syncing.
 * Designed to provide gentle feedback without cluttering the screen.
 */
@Composable
fun MinimalTafsirLoadingIndicator(
    modifier: Modifier = Modifier,
    text: String = "তাফসীর লোড হচ্ছে...",
    subText: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) {
        Color(0xFF1E2822)
    } else {
        Color(0xFFF2FBF6)
    }
    val borderColor = if (isDark) {
        Color(0xFF284435)
    } else {
        Color(0xFFDCF3E7)
    }
    val primaryColor = PrimaryGreen.copy(alpha = 0.85f)
    val textColor = if (isDark) Color(0xFFC8D1CA) else Color(0xFF43534B)
    val bengaliFont = com.example.ui.theme.getBengaliFont("SolaimanLipi")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(0.8.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.6.dp,
                color = primaryColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    fontFamily = bengaliFont
                )
                if (subText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subText,
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.75f),
                        fontFamily = bengaliFont
                    )
                }
            }
        }
    }
}
