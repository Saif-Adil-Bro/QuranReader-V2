package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen

enum class TafsirFallbackType {
    NO_INTERNET,
    NO_TAFSIR_SELECTED,
    NOT_FOUND,
    ERROR
}

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
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(13.dp),
                strokeWidth = 1.6.dp,
                color = primaryColor
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    fontFamily = bengaliFont
                )
                if (subText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subText,
                        fontSize = 10.5.sp,
                        color = textColor.copy(alpha = 0.75f),
                        fontFamily = bengaliFont
                    )
                }
            }
        }
    }
}

/**
 * A clean, minimal fallback card for Tafsir when offline, not found, not selected, or timed out.
 */
@Composable
fun MinimalTafsirFallbackCard(
    type: TafsirFallbackType,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val bengaliFont = com.example.ui.theme.getBengaliFont("SolaimanLipi")

    val (icon: ImageVector, defaultTitle: String, defaultMsg: String, defaultAction: String?, accentColor: Color, bgColor: Color, borderColor: Color) = when (type) {
        TafsirFallbackType.NO_INTERNET -> {
            val acc = if (isDark) Color(0xFFEAB308) else Color(0xFFB45309)
            val bg = if (isDark) Color(0xFF282315) else Color(0xFFFFFBEB)
            val br = if (isDark) Color(0xFF45391F) else Color(0xFFFEF3C7)
            Tuple7(
                Icons.Outlined.WifiOff,
                "ইন্টারনেট সংযোগ নেই",
                "অফলাইনে এই আয়াতের তাফসীর সংরক্ষিত নেই। ইন্টারনেট চালু করুন অথবা সেটিংস থেকে তাফসীর প্যাক ডাউনলোড করে অফলাইনে পড়ুন।",
                "পুনরায় চেষ্টা করুন",
                acc, bg, br
            )
        }
        TafsirFallbackType.NO_TAFSIR_SELECTED -> {
            val acc = PrimaryGreen
            val bg = if (isDark) Color(0xFF1E2822) else Color(0xFFF0FDF4)
            val br = if (isDark) Color(0xFF244333) else Color(0xFFDCFCE7)
            Tuple7(
                Icons.Outlined.MenuBook,
                "কোনো তাফসীর নির্বাচন করা নেই",
                "তাফসীর দেখতে সেটিংস থেকে পছন্দের তাফসীর সিলেক্ট করুন অথবা অফলাইন প্যাক ডাউনলোড করুন।",
                "তাফসীর নির্বাচন করুন",
                acc, bg, br
            )
        }
        TafsirFallbackType.NOT_FOUND -> {
            val acc = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            val bg = if (isDark) Color(0xFF1E2228) else Color(0xFFF8FAFC)
            val br = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            Tuple7(
                Icons.Outlined.SearchOff,
                "এই আয়াতের তাফসীর পাওয়া যায়নি",
                "নির্বাচিত তাফসীরগ্রন্থে এই আয়াতের পৃথক কোনো তাফসীর উল্লেখ নেই।",
                null,
                acc, bg, br
            )
        }
        TafsirFallbackType.ERROR -> {
            val acc = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
            val bg = if (isDark) Color(0xFF2C1E1E) else Color(0xFFFEF2F2)
            val br = if (isDark) Color(0xFF502828) else Color(0xFFFEE2E2)
            Tuple7(
                Icons.Outlined.ErrorOutline,
                "তাফসীর লোড হতে ব্যর্থ হয়েছে",
                "সার্ভার থেকে তাফসীর সংগ্রহ করা সম্ভব হয়নি। সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।",
                "পুনরায় চেষ্টা করুন",
                acc, bg, br
            )
        }
    }

    val displayTitle = title ?: defaultTitle
    val displayMsg = message ?: defaultMsg
    val displayAction = actionText ?: defaultAction

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(0.8.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = displayTitle,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayTitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = bengaliFont
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayMsg,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                fontFamily = bengaliFont
            )
            if (displayAction != null && onAction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onAction,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = displayAction,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = bengaliFont
                        )
                    }
                }
            }
        }
    }
}

private data class Tuple7<A, B, C, D, E, F, G>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G
)

