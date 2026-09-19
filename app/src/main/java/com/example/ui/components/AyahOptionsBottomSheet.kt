package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CombinedAyah
import com.example.data.model.appendStyledWaqfText
import com.example.utils.DateUtil
import com.example.utils.IndoPakTajweedParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahOptionsBottomSheet(
    ayah: CombinedAyah,
    isPlaying: Boolean,
    onPlayToggle: (CombinedAyah) -> Unit,
    onDismiss: () -> Unit,
    theme: String = "Light",
    showTajweed: Boolean = true,
    arabicFontName: String = "Me Quran",
    arabicFontSize: Float = 22f,
    onOpenPhotoCard: ((CombinedAyah, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showFullTafsirDialog by remember { mutableStateOf(false) }
    var showPhotoCardDialog by remember { mutableStateOf(false) }
    var showVideoCreatorDialog by remember { mutableStateOf(false) }

    val isDark = theme == "Dark"
    val isSepia = theme == "Sepia"

    val sheetBg = when {
        isDark -> Color(0xFF1E1E1E)
        isSepia -> Color(0xFFFBF4EB)
        else -> Color.White
    }

    val cardBg = when {
        isDark -> Color(0xFF2C2C2C)
        isSepia -> Color(0xFFF3E9DD)
        else -> Color(0xFFF8F9FA)
    }

    val primaryAccent = when {
        isDark -> Color(0xFF81C784)
        isSepia -> Color(0xFF8B6B48)
        else -> Color(0xFF1E5631)
    }

    val textColor = when {
        isDark -> Color(0xFFE0E0E0)
        isSepia -> Color(0xFF3E2723)
        else -> Color(0xFF1A1A1A)
    }

    val subtitleColor = when {
        isDark -> Color(0xFFA0A0A0)
        isSepia -> Color(0xFF795548)
        else -> Color(0xFF5F6368)
    }

    val surahData = remember(ayah.surahNumber) {
        com.example.data.QuranData.surahNames.find { it.first == ayah.surahNumber }
    }
    val surahName = surahData?.second?.first ?: "সূরা ${ayah.surahNumber}"
    val surahMeaning = surahData?.second?.second ?: ""
    val displaySurahTitle = if (surahMeaning.isNotEmpty()) "$surahName ($surahMeaning)" else surahName

    val parsedTafsir = remember(ayah.tafsirText) {
        ayah.tafsirText?.parseHtmlToAnnotatedString(primaryAccent)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBg,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = displaySurahTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = primaryAccent
                    )
                    Text(
                        text = "আয়াত ${DateUtil.toBengaliNumerals(ayah.numberInSurah)} • পারা ${DateUtil.toBengaliNumerals(ayah.juz)} • পৃষ্ঠা ${DateUtil.toBengaliNumerals(ayah.page)}",
                        fontSize = 12.sp,
                        color = subtitleColor
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = subtitleColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Arabic Text Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "আরবি আয়াত",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryAccent
                            )
                            Surface(
                                shape = CircleShape,
                                color = primaryAccent.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = DateUtil.toBengaliNumerals(ayah.numberInSurah),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val formattedArabic = remember(ayah.arabicText, showTajweed, theme, arabicFontName, arabicFontSize) {
                            if (showTajweed) {
                                IndoPakTajweedParser.parseIndoPakTajweed(
                                    text = ayah.arabicText,
                                    defaultColor = textColor,
                                    fontSize = arabicFontSize,
                                    showWaqfSigns = true,
                                    arabicFontName = arabicFontName
                                )
                            } else {
                                androidx.compose.ui.text.buildAnnotatedString {
                                    appendStyledWaqfText(
                                        text = ayah.arabicText,
                                        fontSize = arabicFontSize,
                                        showWaqfSigns = true,
                                        arabicFontName = arabicFontName
                                    )
                                }
                            }
                        }

                        Text(
                            text = formattedArabic,
                            fontSize = arabicFontSize.sp,
                            lineHeight = (arabicFontSize * 1.8f).sp,
                            fontFamily = com.example.ui.theme.getArabicFont(arabicFontName),
                            color = textColor,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Bangla Translation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "বাংলা অনুবাদ:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ayah.bengaliText.ifEmpty { "অনুবাদ উপলব্ধ নয়" },
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = textColor,
                            fontFamily = com.example.ui.theme.getBengaliFont("SolaimanLipi")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Tafsir Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "তাফসীর:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = primaryAccent
                            )
                            if (parsedTafsir != null && parsedTafsir.isNotEmpty()) {
                                TextButton(onClick = { showFullTafsirDialog = true }) {
                                    Text(
                                        text = "সম্পূর্ণ দেখুন",
                                        color = primaryAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (parsedTafsir != null && parsedTafsir.isNotEmpty()) {
                            Text(
                                text = parsedTafsir,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = textColor,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "এই আয়াতের তাফসীর লোড করতে সেটিংস থেকে তাফসীর প্যাক নির্বাচন করুন অথবা ইন্টারনেট সংযোগ পরীক্ষা করুন।",
                                fontSize = 13.sp,
                                color = subtitleColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Action Row (Play, Copy, Share, Photo Card, Video)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Audio
                Button(
                    onClick = { onPlayToggle(ayah) },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(52.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "তিলাওয়াত",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Copy
                OutlinedButton(
                    onClick = {
                        val textToCopy = "${ayah.arabicText}\n\n${ayah.bengaliText}\n\n($surahName: ${ayah.numberInSurah})"
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        Toast.makeText(context, "আয়াত ও অনুবাদ কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, primaryAccent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "কপি",
                        tint = primaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Share
                OutlinedButton(
                    onClick = {
                        val textToShare = "${ayah.arabicText}\n\n${ayah.bengaliText}\n\n($surahName: ${ayah.numberInSurah})"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textToShare)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "আয়াত শেয়ার করুন"))
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, primaryAccent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "শেয়ার",
                        tint = primaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Photo Card Quick Create
                Button(
                    onClick = {
                        if (onOpenPhotoCard != null) {
                            onOpenPhotoCard(ayah, surahName)
                        } else {
                            showPhotoCardDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFD97706) else Color(0xFFEAB308)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(52.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "ফটো কার্ড",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Quick Quran Video Creator Button
                Button(
                    onClick = {
                        showVideoCreatorDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(52.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "কুরআন ভিডিও",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Photo Card Customizer Dialog (Quick Create Flow)
    if (showPhotoCardDialog) {
        val quickCardPost = remember(ayah, surahName) {
            com.example.utils.PhotoCardBridge.fromAyah(ayah, surahName).toShortPost()
        }
        com.example.ui.screens.PhotoCardCustomizerDialog(
            post = quickCardPost,
            onDismiss = { showPhotoCardDialog = false }
        )
    }

    // Quick Quran Video Creator Dialog
    if (showVideoCreatorDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showVideoCreatorDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val videoVm: com.example.ui.viewmodels.QuranVideoViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                key = "quick_video_${ayah.surahNumber}_${ayah.numberInSurah}_${ayah.number}"
            )
            LaunchedEffect(ayah.number, ayah.surahNumber, ayah.numberInSurah, surahName) {
                videoVm.loadFromQuickCreate(
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.numberInSurah,
                    ayah = ayah,
                    surahName = surahName
                )
            }
            com.example.ui.screens.QuranVideoCreatorScreen(
                onNavigateBack = { showVideoCreatorDialog = false },
                viewModel = videoVm
            )
        }
    }

    // Full Tafsir Dialog
    if (showFullTafsirDialog) {
        AlertDialog(
            onDismissRequest = { showFullTafsirDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            title = {
                Column {
                    Text(
                        text = "$displaySurahTitle • আয়াত ${DateUtil.toBengaliNumerals(ayah.numberInSurah)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = primaryAccent
                    )
                    Text("সম্পূর্ণ তাফসীর", fontSize = 13.sp, color = subtitleColor)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (parsedTafsir != null && parsedTafsir.isNotEmpty()) {
                        Text(
                            text = parsedTafsir,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = textColor,
                            textAlign = TextAlign.Justify
                        )
                    } else {
                        Text("এই আয়াতের তাফসীর তথ্য পাওয়া যায়নি।", fontSize = 15.sp, color = subtitleColor)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullTafsirDialog = false }) {
                    Text("বন্ধ করুন", color = primaryAccent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = cardBg
        )
    }
}
