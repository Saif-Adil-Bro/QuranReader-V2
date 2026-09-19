package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranData
import com.example.utils.DateUtil

@Composable
fun SurahNavigationButtons(
    currentSurahNumber: Int,
    onNavigateToSurah: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val prevSurahNum = currentSurahNumber - 1
    val nextSurahNum = currentSurahNumber + 1

    val prevSurahName = if (prevSurahNum >= 1) {
        QuranData.surahNames.find { it.first == prevSurahNum }?.second?.first ?: ""
    } else ""

    val nextSurahName = if (nextSurahNum <= 114) {
        QuranData.surahNames.find { it.first == nextSurahNum }?.second?.first ?: ""
    } else ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 24.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Surah Button
        if (prevSurahNum >= 1) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                color = containerColor,
                tonalElevation = 2.dp,
                onClick = { onNavigateToSurah(prevSurahNum) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "পূর্ববর্তী সূরা",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "পূর্ববর্তী সূরা",
                            fontSize = 11.sp,
                            color = subtextColor,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "${DateUtil.toBengaliNumerals(prevSurahNum)}. $prevSurahName",
                            fontSize = 14.sp,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Next Surah Button
        if (nextSurahNum <= 114) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                color = containerColor,
                tonalElevation = 2.dp,
                onClick = { onNavigateToSurah(nextSurahNum) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "পরবর্তী সূরা",
                            fontSize = 11.sp,
                            color = subtextColor,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "${DateUtil.toBengaliNumerals(nextSurahNum)}. $nextSurahName",
                            fontSize = 14.sp,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "পরবর্তী সূরা",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
