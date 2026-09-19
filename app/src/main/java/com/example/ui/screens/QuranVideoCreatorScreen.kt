package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.QuranData
import com.example.data.model.*
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.QuranVideoViewModel
import com.example.util.QariData
import com.example.utils.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranVideoCreatorScreen(
    onNavigateBack: () -> Unit,
    initialSurah: Int? = null,
    initialAyah: Int? = null,
    viewModel: QuranVideoViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val audioPrepState by viewModel.audioPrepState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackProgress by viewModel.currentPlaybackProgress.collectAsState()
    val currentAyahIndex by viewModel.currentAyahIndex.collectAsState()
    val playingAyahNumber by viewModel.playingAyahNumber.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportedUri by viewModel.exportedVideoUri.collectAsState()
    val exportError by viewModel.exportError.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(initialSurah, initialAyah) {
        if (initialSurah != null && initialSurah > 0) {
            val ayah = initialAyah ?: 1
            viewModel.loadSurah(initialSurah, ayah, ayah)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setCustomImage(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🎬 কুরআন ভিডিও ক্রিয়েটর",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = when (currentStep) {
                                1 -> "ধাপ ১: আয়াত নির্বাচন"
                                2 -> "ধাপ ২: অডিও ডাউনলোড ও প্রস্তুতি"
                                3 -> "ধাপ ৩: ডিজাইন ও লাইভ প্রিভিউ"
                                else -> "ধাপ ৪: ভিডিও রেন্ডারিং ও শেয়ার"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1 && !isExporting) {
                            viewModel.setStep(currentStep - 1)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Step Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (step in 1..4) {
                    val isActive = step <= currentStep
                    val isCurrent = step == currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    isCurrent -> PrimaryGreen
                                    isActive -> PrimaryGreen.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentStep) {
                    1 -> Step1ContentSelection(
                        config = config,
                        onSurahSelected = { surah, start, end -> viewModel.loadSurah(surah, start, end) },
                        onToggleBismillah = { viewModel.toggleIncludeBismillah(it) },
                        onNext = { viewModel.setStep(2) }
                    )
                    2 -> Step2AudioPreparation(
                        config = config,
                        audioPrepState = audioPrepState,
                        playingAyahNumber = playingAyahNumber,
                        onQariSelected = { qId, qName -> viewModel.setQari(qId, qName) },
                        onStartPrepare = { viewModel.startAudioPreparation() },
                        onPlayAyah = { viewModel.playSingleAyahAudio(it) },
                        onStopAyah = { viewModel.stopSingleAyahAudio() },
                        onNext = { viewModel.setStep(3) }
                    )
                    3 -> Step3DesignAndPreview(
                        config = config,
                        isPlaying = isPlaying,
                        playbackProgress = playbackProgress,
                        currentAyahIndex = currentAyahIndex,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSeek = { viewModel.seekToProgress(it) },
                        onTemplateSelected = { viewModel.setTemplate(it) },
                        onRatioSelected = { viewModel.setAspectRatio(it) },
                        onOverlaySelected = { viewModel.setOverlay(it) },
                        onAnimationSelected = { viewModel.setAnimationStyle(it) },
                        onPickGallery = { imagePickerLauncher.launch("image/*") },
                        onClearBackground = { viewModel.setBackgroundPreset(null); viewModel.setCustomImage(null) },
                        onToggleBangla = { viewModel.toggleBanglaTranslation(it) },
                        onToggleReference = { viewModel.toggleReference(it) },
                        onToggleLogo = { viewModel.toggleLogo(it) },
                        onToggleCredit = { viewModel.toggleCredit(it) },
                        onToggleWaqfSigns = { viewModel.toggleWaqfSigns(it) },
                        onArabicFontSizeChange = { viewModel.setArabicFontSize(it) },
                        onArabicLineSpacingChange = { viewModel.setArabicLineSpacing(it) },
                        onTranslationFontSizeChange = { viewModel.setTranslationFontSize(it) },
                        onTranslationLineSpacingChange = { viewModel.setTranslationLineSpacing(it) },
                        onArabicFontChange = { viewModel.setArabicFontName(it) },
                        onBengaliFontChange = { viewModel.setBengaliFontName(it) },
                        onCreditTextChange = { viewModel.setCreditText(it) },
                        onExport = { viewModel.startExport() }
                    )
                    4 -> Step4Export(
                        isExporting = isExporting,
                        progress = exportProgress,
                        exportedUri = exportedUri,
                        errorMessage = exportError,
                        onShare = { viewModel.shareExportedVideo() },
                        onCreateAnother = { viewModel.setStep(1) }
                    )
                }
            }
        }
    }
}

@Composable
fun Step1ContentSelection(
    config: QuranVideoConfig,
    onSurahSelected: (Int, Int, Int) -> Unit,
    onToggleBismillah: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    var expandedSurahDropdown by remember { mutableStateOf(false) }
    var selectedSurahNumber by remember { mutableIntStateOf(config.surahNumber) }
    var startAyah by remember { mutableIntStateOf(config.ayahStart) }
    var endAyah by remember { mutableIntStateOf(config.ayahEnd) }

    val currentBanglaName = QuranData.getSurahNameBangla(selectedSurahNumber)
    val currentArabicName = QuranData.getSurahNameArabic(selectedSurahNumber)
    val totalAyahs = QuranData.getAyahCount(selectedSurahNumber)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Intro Card
        Card(
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(32.dp))
                Column {
                    Text("কুরআন ভিডিও ক্রিয়েটর (৪-ধাপ পদ্ধতি)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("১. আয়াত সিলেক্ট ➔ ২. অডিও ডাউনলোড ও প্রিপারেশন ➔ ৩. ডিজাইন ও প্রিভিউ ➔ ৪. রেন্ডারিং ও শেয়ার", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Surah Selection
        Text("১. সূরা নির্বাচন করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        OutlinedCard(
            onClick = { expandedSurahDropdown = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "সূরা $currentBanglaName ($currentArabicName)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "মোট আয়াত সংখ্যা: ${DateUtil.toBengaliNumerals(totalAyahs)}টি",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expandedSurahDropdown,
            onDismissRequest = { expandedSurahDropdown = false },
            modifier = Modifier.heightIn(max = 350.dp)
        ) {
            QuranData.surahNames.forEach { item ->
                val sNum = item.first
                val sBangla = item.second.first
                val sArabic = item.second.second
                DropdownMenuItem(
                    text = {
                        Text("${DateUtil.toBengaliNumerals(sNum)}. সূরা $sBangla ($sArabic)")
                    },
                    onClick = {
                        selectedSurahNumber = sNum
                        startAyah = 1
                        endAyah = 1
                        expandedSurahDropdown = false
                        onSurahSelected(sNum, 1, 1)
                    }
                )
            }
        }

        // Ayah Range Selection
        Text("২. আয়াতের পরিসীমা নির্বাচন করুন (১ হতে ${DateUtil.toBengaliNumerals(totalAyahs)})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Start Ayah Stepper
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "শুরুর আয়াত",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            onClick = {
                                if (startAyah > 1) {
                                    startAyah--
                                    if (endAyah < startAyah) endAyah = startAyah
                                    onSurahSelected(selectedSurahNumber, startAyah, endAyah)
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "কমান",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        BasicTextField(
                            value = if (startAyah > 0) startAyah.toString() else "",
                            onValueChange = { input ->
                                val cleaned = input.filter { it.isDigit() }
                                val num = cleaned.toIntOrNull() ?: 1
                                val validNum = num.coerceIn(1, totalAyahs)
                                startAyah = validNum
                                if (endAyah < startAyah) endAyah = startAyah
                                onSurahSelected(selectedSurahNumber, startAyah, endAyah)
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .width(46.dp)
                                .padding(horizontal = 4.dp)
                        )

                        Surface(
                            onClick = {
                                if (startAyah < totalAyahs) {
                                    startAyah++
                                    if (endAyah < startAyah) endAyah = startAyah
                                    onSurahSelected(selectedSurahNumber, startAyah, endAyah)
                                }
                            },
                            shape = CircleShape,
                            color = PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "বাড়ান",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // End Ayah Stepper
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "শেষ আয়াত",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            onClick = {
                                if (endAyah > startAyah) {
                                    endAyah--
                                    onSurahSelected(selectedSurahNumber, startAyah, endAyah)
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "কমান",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        BasicTextField(
                            value = if (endAyah > 0) endAyah.toString() else "",
                            onValueChange = { input ->
                                val cleaned = input.filter { it.isDigit() }
                                val num = cleaned.toIntOrNull() ?: startAyah
                                val validNum = num.coerceIn(startAyah, totalAyahs)
                                endAyah = validNum
                                onSurahSelected(selectedSurahNumber, startAyah, endAyah)
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .width(46.dp)
                                .padding(horizontal = 4.dp)
                        )

                        Surface(
                            onClick = {
                                if (endAyah < totalAyahs) {
                                    endAyah++
                                    onSurahSelected(selectedSurahNumber, startAyah, endAyah)
                                }
                            },
                            shape = CircleShape,
                            color = PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "বাড়ান",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bismillah Checkbox for Surahs other than Surah Al-Fatiha (Surah 1)
        if (selectedSurahNumber > 1) {
            Surface(
                onClick = {
                    onToggleBismillah(!config.includeBismillah)
                },
                shape = RoundedCornerShape(12.dp),
                color = if (config.includeBismillah) PrimaryGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    if (config.includeBismillah) PrimaryGreen.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = config.includeBismillah,
                        onCheckedChange = { onToggleBismillah(it) },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "শুরুতে বিসমিল্লাহ যুক্ত করুন",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ভিডিওর শুরুতে সূরা আল-ফাতিহার প্রথম আয়াত (বিসমিল্লাহ) তিলাওয়াতসহ যোগ হবে",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Preview of selected verses
        Text("নির্বাচিত আয়াতসমূহ (${DateUtil.toBengaliNumerals(config.selectedAyahs.size)}টি)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                config.selectedAyahs.forEach { ayah ->
                    val isBismillah = ayah.surahNumber == 1 && ayah.numberInSurah == 1 && config.surahNumber != 1
                    Column {
                        if (isBismillah) {
                            Surface(
                                color = PrimaryGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "বিসমিল্লাহির রাহমানির রাহিম",
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = ayah.arabicText,
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryGreen,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBismillah) ayah.bengaliText else "${DateUtil.toBengaliNumerals(ayah.numberInSurah)}. ${ayah.bengaliText}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Next Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("পরবর্তী ধাপ: অডিও প্রস্তুতি ➔", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun Step2AudioPreparation(
    config: QuranVideoConfig,
    audioPrepState: AudioPreparationState,
    playingAyahNumber: Int?,
    onQariSelected: (String, String) -> Unit,
    onStartPrepare: () -> Unit,
    onPlayAyah: (PreparedAyahAudio) -> Unit,
    onStopAyah: () -> Unit,
    onNext: () -> Unit
) {
    var expandedQariDropdown by remember { mutableStateOf(false) }
    val currentQariDisplay = com.example.util.QariData.getQariDisplayName(config.qariId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(30.dp))
                Column {
                    Text("ধাপ ২: অডিও ডাউনলোড ও টাইমিং প্রস্তুতি", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Text("আগে অডিও ডাউনলোড করে প্রতিটি আয়াতের সঠিক সময়কাল (timing) পরিমাপ করা হবে, যাতে ভিডিওতে কোনো মিস-ম্যাচ না হয়।", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Qari Selection
        Text("১. ক্বারী / তেলাওয়াতকারী নির্বাচন করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        OutlinedCard(
            onClick = { expandedQariDropdown = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = PrimaryGreen)
                    Column {
                        Text(
                            text = config.qariName.ifEmpty { currentQariDisplay },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "ক্বারী পরিবর্তন করতে স্পর্শ করুন",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryGreen)
            }

            DropdownMenu(
                expanded = expandedQariDropdown,
                onDismissRequest = { expandedQariDropdown = false },
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                com.example.util.QariData.list.forEach { item ->
                    val isSelected = config.qariId == item.id
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = item.nameBengali,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.nameEnglish,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGreen)
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                        },
                        onClick = {
                            onQariSelected(item.id, item.nameBengali)
                            expandedQariDropdown = false
                        }
                    )
                }
            }
        }

        // Preparation Status Card
        Text("২. অডিও প্রস্তুতি ও টাইমিং যাচাই", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "নির্বাচিত আয়াত: ${DateUtil.toBengaliNumerals(config.selectedAyahs.size)}টি",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                    if (audioPrepState.isPrepared) {
                        val totalSec = audioPrepState.preparedAudios.sumOf { it.durationMs } / 1000f
                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "মোট দৈর্ঘ্য: ${String.format(java.util.Locale.US, "%.1f", totalSec)} সেকেন্ড",
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (audioPrepState.isPreparing) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { audioPrepState.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = PrimaryGreen
                        )
                        Text(
                            text = "আয়াত ${DateUtil.toBengaliNumerals(audioPrepState.currentAyahNumber)} ডাউনলোড ও কনভার্ট হচ্ছে (${(audioPrepState.progress * 100).toInt()}%)...",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryGreen
                        )
                    }
                } else if (audioPrepState.errorMessage != null) {
                    Text(
                        text = "ত্রুটি: ${audioPrepState.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.5.sp
                    )
                }

                // List of Ayahs with Play Buttons & Duration
                if (audioPrepState.isPrepared) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        audioPrepState.preparedAudios.forEach { prep ->
                            val isThisPlaying = playingAyahNumber == prep.ayahNumber
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isThisPlaying) PrimaryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isThisPlaying) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (isThisPlaying) onStopAyah() else onPlayAyah(prep)
                                            },
                                            modifier = Modifier.size(32.dp).clip(CircleShape).background(PrimaryGreen)
                                        ) {
                                            Icon(
                                                if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            val ayahTitle = if (prep.ayahNumber == 1 && config.surahNumber != 1) "বিসমিল্লাহ" else "আয়াত ${DateUtil.toBengaliNumerals(prep.numberInSurah)}"
                                            Text(ayahTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(prep.arabicPreview, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${String.format(java.util.Locale.US, "%.1f", prep.durationMs / 1000f)} সে.",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Start Prepare / Re-prepare Button
                Button(
                    onClick = onStartPrepare,
                    enabled = !audioPrepState.isPreparing,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (audioPrepState.isPrepared) MaterialTheme.colorScheme.surfaceVariant else PrimaryGreen,
                        contentColor = if (audioPrepState.isPrepared) MaterialTheme.colorScheme.onSurface else Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        if (audioPrepState.isPrepared) Icons.Default.Refresh else Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (audioPrepState.isPrepared) "পুনরায় অডিও ডাউনলোড ও যাচাই করুন" else "📥 অডিও ডাউনলোড ও প্রিপারেশন শুরু করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Next Button
        Button(
            onClick = onNext,
            enabled = audioPrepState.isPrepared && !audioPrepState.isPreparing,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("পরবর্তী ধাপ: টেমপ্লেট ও ডিজাইন প্রিভিউ ➔", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun Step3DesignAndPreview(
    config: QuranVideoConfig,
    isPlaying: Boolean,
    playbackProgress: Float,
    currentAyahIndex: Int,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onTemplateSelected: (QuranVideoTemplate) -> Unit,
    onRatioSelected: (VideoAspectRatio) -> Unit,
    onOverlaySelected: (BackgroundOverlay) -> Unit,
    onAnimationSelected: (TextAnimationStyle) -> Unit,
    onPickGallery: () -> Unit,
    onClearBackground: () -> Unit,
    onToggleBangla: (Boolean) -> Unit,
    onToggleReference: (Boolean) -> Unit,
    onToggleLogo: (Boolean) -> Unit,
    onToggleCredit: (Boolean) -> Unit,
    onToggleWaqfSigns: (Boolean) -> Unit,
    onArabicFontSizeChange: (Float) -> Unit,
    onArabicLineSpacingChange: (Float) -> Unit,
    onTranslationFontSizeChange: (Float) -> Unit,
    onTranslationLineSpacingChange: (Float) -> Unit,
    onArabicFontChange: (String) -> Unit,
    onBengaliFontChange: (String) -> Unit,
    onCreditTextChange: (String) -> Unit = {},
    onExport: () -> Unit
) {
    val t = config.template
    var showAdvanced by remember { mutableStateOf(false) }
    var expandedArabicFontDropdown by remember { mutableStateOf(false) }
    var expandedBengaliFontDropdown by remember { mutableStateOf(false) }

    val arabicFontsList = listOf(
        "Noorehira" to "নূরে হেরা (Noorehira - Indo-Pak)",
        "Me Quran" to "মি কুরআন (Me Quran - Indo-Pak)",
        "PDMS Saleem" to "সেলিম (PDMS Saleem - Indo-Pak)",
        "Scheherazade New" to "শাহরাজাদ (Scheherazade New)",
        "Amiri" to "আমিরি (Amiri)",
        "Amiri Quran" to "আমিরি কুরআন (Amiri Quran)",
        "Uthman Taha" to "উছমান তাহা (Uthman Taha)",
        "Lateef" to "লতীফ (Lateef)",
        "Almarai" to "আল মারাই (Almarai)",
        "Tajawal" to "তাজাওয়াল (Tajawal)"
    )

    val banglaFontsList = listOf(
        "SolaimanLipi" to "সোলাইমান লিপি (SolaimanLipi)",
        "Hind Siliguri" to "হিন্দ শিলিগুড়ি (Hind Siliguri)",
        "Shorif Shishir Unicode" to "শরীফ শিশির (Shorif Shishir)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Video Live Preview Card
        val previewAspectRatio = when (config.aspectRatio) {
            VideoAspectRatio.PORTRAIT_9_16 -> 9f / 16f
            VideoAspectRatio.SQUARE_1_1 -> 1f
            VideoAspectRatio.LANDSCAPE_16_9 -> 16f / 9f
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(if (config.aspectRatio == VideoAspectRatio.PORTRAIT_9_16) 0.75f else 0.95f)
                .aspectRatio(previewAspectRatio),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            border = BorderStroke(2.dp, t.accentColor.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(t.gradientColors))
            ) {
                // Background Overlay
                val overlayAlpha = when (config.overlay) {
                    BackgroundOverlay.NONE -> 0f
                    BackgroundOverlay.LIGHT -> 0.30f
                    BackgroundOverlay.MEDIUM -> 0.55f
                    BackgroundOverlay.DARK -> 0.78f
                }
                if (overlayAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = overlayAlpha))
                    )
                }

                // Inner Frame Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Logo & Reference
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (config.showLogo) {
                            Text(
                                text = config.logoText,
                                color = t.referenceColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        if (config.showReference) {
                            val activeAyah = config.selectedAyahs.getOrNull(currentAyahIndex)
                            val refText = if (activeAyah != null && activeAyah.surahNumber == 1 && activeAyah.numberInSurah == 1 && config.surahNumber != 1) {
                                "সূরা ${config.surahName} • বিসমিল্লাহ"
                            } else {
                                "সূরা ${config.surahName} • আয়াত ${DateUtil.toBengaliNumerals(activeAyah?.numberInSurah ?: config.ayahStart)}"
                            }
                            Text(
                                text = refText,
                                color = t.referenceColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Main Ayah Content (Live Dynamic Arabic & Bangla Line Spacing)
                    val activeAyah = config.selectedAyahs.getOrNull(currentAyahIndex)
                    val rawArabic = activeAyah?.arabicText ?: "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
                    val processedArabic = com.example.utils.QuranIndoPakNormalizer.processIndoPakText(
                        rawText = rawArabic,
                        showWaqfSigns = config.showWaqfSigns
                    )
                    val customArabicFamily = com.example.ui.theme.getArabicFont(config.arabicFontName)
                    val customBengaliFamily = com.example.ui.theme.getBengaliFont(config.bengaliFontName)
                    val ayahPreviewScrollState = rememberScrollState()

                    LaunchedEffect(currentAyahIndex, playbackProgress) {
                        if (ayahPreviewScrollState.maxValue > 0) {
                            val targetScroll = (ayahPreviewScrollState.maxValue * playbackProgress).toInt()
                            ayahPreviewScrollState.scrollTo(targetScroll)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(ayahPreviewScrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = processedArabic,
                                color = t.arabicColor,
                                fontSize = (config.arabicFontSize * 0.75f).sp,
                                fontFamily = customArabicFamily,
                                textAlign = TextAlign.Center,
                                lineHeight = (config.arabicFontSize * 0.75f * config.arabicLineSpacing).sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (config.showBanglaTranslation && !activeAyah?.bengaliText.isNullOrBlank()) {
                                Text(
                                    text = activeAyah?.bengaliText ?: "",
                                    color = t.translationColor,
                                    fontSize = (config.translationFontSize * 0.75f).sp,
                                    fontFamily = customBengaliFamily,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (config.translationFontSize * 0.75f * config.translationLineSpacing).sp
                                )
                            }
                        }
                    }

                    // Footer Credit
                    if (config.showCredit) {
                        Text(
                            text = config.creditText,
                            color = t.translationColor.copy(alpha = 0.6f),
                            fontSize = 8.5.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Playback Timeline Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        Text(config.qariName, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }
                }

                Slider(
                    value = playbackProgress,
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryGreen,
                        activeTrackColor = PrimaryGreen
                    )
                )
            }
        }

        // 1. Template Picker
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("১. ভিডিও টেমপ্লেট নির্বাচন করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(QuranVideoTemplate.values()) { tmpl ->
                    val isSelected = config.template == tmpl
                    OutlinedCard(
                        onClick = { onTemplateSelected(tmpl) },
                        modifier = Modifier.width(130.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(tmpl.gradientColors))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(tmpl.accentColor.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("۞", color = tmpl.arabicColor, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tmpl.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = tmpl.arabicColor,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = tmpl.description,
                                fontSize = 9.sp,
                                color = tmpl.translationColor.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Aspect Ratio Picker
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("২. আসপেক্ট রেশিও (Aspect Ratio)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoAspectRatio.values().forEach { ratio ->
                    val isSelected = config.aspectRatio == ratio
                    OutlinedButton(
                        onClick = { onRatioSelected(ratio) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) PrimaryGreen.copy(alpha = 0.12f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = ratio.label.split(" ")[0],
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. Background Source & Overlay
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("৩. ব্যাকগ্রাউন্ড ছবি ও ওভারলে", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPickGallery,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("গ্যালারি ছবি", fontSize = 13.sp)
                }

                if (config.customImageUri != null || config.backgroundPresetName != null) {
                    OutlinedButton(
                        onClick = onClearBackground,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ডিফল্ট গ্রেডিয়েন্ট", fontSize = 13.sp)
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BackgroundOverlay.values()) { ov ->
                    FilterChip(
                        selected = config.overlay == ov,
                        onClick = { onOverlaySelected(ov) },
                        label = { Text(ov.label, fontSize = 12.sp) }
                    )
                }
            }
        }

        // 4. Advanced Typography (Arabic & Bangla Line Spacing & Fonts)
        Card(
            onClick = { showAdvanced = !showAdvanced },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryGreen)
                    Text("ফন্ট, লাইন স্পেসিং ও কাস্টমাইজেশন", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
                Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
        }

        AnimatedVisibility(visible = showAdvanced) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                // Animation Style
                Text("টেক্সট অ্যানিমেশন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TextAnimationStyle.values()) { anim ->
                        FilterChip(
                            selected = config.animationStyle == anim,
                            onClick = { onAnimationSelected(anim) },
                            label = { Text(anim.label, fontSize = 12.sp) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Arabic Font Selection Dropdown
                Text("🕌 আরবি ফন্ট ও লাইন স্পেসিং", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedCard(
                    onClick = { expandedArabicFontDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val selectedArabic = arabicFontsList.find { it.first == config.arabicFontName }?.second ?: config.arabicFontName
                            Text(selectedArabic, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("আরবি ফন্ট পরিবর্তন করতে ট্যাপ করুন", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryGreen)
                    }

                    DropdownMenu(
                        expanded = expandedArabicFontDropdown,
                        onDismissRequest = { expandedArabicFontDropdown = false },
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        arabicFontsList.forEach { (key, label) ->
                            val isSelected = config.arabicFontName == key
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGreen) }
                                } else null,
                                onClick = {
                                    onArabicFontChange(key)
                                    expandedArabicFontDropdown = false
                                }
                            )
                        }
                    }
                }

                // Arabic Font Size Slider
                Text("আরবি ফন্ট সাইজ: ${config.arabicFontSize.toInt()}", fontSize = 13.sp)
                Slider(
                    value = config.arabicFontSize,
                    onValueChange = onArabicFontSizeChange,
                    valueRange = 20f..40f
                )

                // Arabic Line Spacing Slider (1.4x to 2.4x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("আরবি লাইন স্পেস (Line Spacing):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${String.format(java.util.Locale.US, "%.2f", config.arabicLineSpacing)}x", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                }
                Slider(
                    value = config.arabicLineSpacing,
                    onValueChange = onArabicLineSpacingChange,
                    valueRange = 1.40f..2.40f,
                    steps = 10
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Bangla Font Selection Dropdown
                Text("🇧🇩 বাংলা ফন্ট ও লাইন স্পেসিং", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedCard(
                    onClick = { expandedBengaliFontDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val selectedBangla = banglaFontsList.find { it.first == config.bengaliFontName }?.second ?: config.bengaliFontName
                            Text(selectedBangla, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("বাংলা ফন্ট পরিবর্তন করতে ট্যাপ করুন", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryGreen)
                    }

                    DropdownMenu(
                        expanded = expandedBengaliFontDropdown,
                        onDismissRequest = { expandedBengaliFontDropdown = false }
                    ) {
                        banglaFontsList.forEach { (key, label) ->
                            val isSelected = config.bengaliFontName == key
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGreen) }
                                } else null,
                                onClick = {
                                    onBengaliFontChange(key)
                                    expandedBengaliFontDropdown = false
                                }
                            )
                        }
                    }
                }

                // Translation Font Size Slider
                Text("অনুবাদ ফন্ট সাইজ: ${config.translationFontSize.toInt()}", fontSize = 13.sp)
                Slider(
                    value = config.translationFontSize,
                    onValueChange = onTranslationFontSizeChange,
                    valueRange = 12f..24f
                )

                // Bangla Line Spacing Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("অনুবাদ লাইন স্পেস:", fontSize = 13.sp)
                    Text("${String.format(java.util.Locale.US, "%.2f", config.translationLineSpacing)}x", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                }
                Slider(
                    value = config.translationLineSpacing,
                    onValueChange = onTranslationLineSpacingChange,
                    valueRange = 1.10f..1.80f,
                    steps = 7
                )

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("বাংলা অনুবাদ প্রদর্শন করুন", fontSize = 13.sp)
                    Switch(checked = config.showBanglaTranslation, onCheckedChange = onToggleBangla)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("সূরা ও আয়াত রেফারেন্স দেখান", fontSize = 13.sp)
                    Switch(checked = config.showReference, onCheckedChange = onToggleReference)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ওয়াকফ ও তাজবীদ চিহ্ন (Waqf Signs)", fontSize = 13.sp)
                        Text("ইন্দো-পাক ওয়াকফ চিহ্ন প্রদর্শন নিয়ন্ত্রণ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = config.showWaqfSigns, onCheckedChange = onToggleWaqfSigns)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("লোগো (Quran READER)", fontSize = 13.sp)
                    Switch(checked = config.showLogo, onCheckedChange = onToggleLogo)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ক্রেডিট (MuslimsLibrary)", fontSize = 13.sp)
                    Switch(checked = config.showCredit, onCheckedChange = onToggleCredit)
                }

                if (config.showCredit) {
                    OutlinedTextField(
                        value = config.creditText,
                        onValueChange = onCreditTextChange,
                        label = { Text("ক্রেডিট টেক্সট", fontSize = 12.sp) },
                        placeholder = { Text("MuslimsLibrary") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Render Action Button
        Button(
            onClick = onExport,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🎬 ৭২০p ভিডিও রেন্ডারিং ও শেয়ার শুরু করুন ➔", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
        }
    }
}

@Composable
fun Step4Export(
    isExporting: Boolean,
    progress: Float,
    exportedUri: Uri?,
    errorMessage: String?,
    onShare: () -> Unit,
    onCreateAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isExporting) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp,
                color = PrimaryGreen
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "ভিডিও রেন্ডার হচ্ছে...",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${(progress * 100).toInt()}% সম্পন্ন",
                fontSize = 14.sp,
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "সঠিক অডিও টাইমিং ও ১.৮x আরবি স্পেসিং সমন্বয়ে ৭২০p কোয়ালিটিতে আপনার ভিডিও প্রস্তুত করা হচ্ছে।",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else if (errorMessage != null) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ভিডিও তৈরি করা সম্ভব হয়নি",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onCreateAnother,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("আবার চেষ্টা করুন")
            }
        } else {
            // Success
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "মাশাআল্লাহ! ভিডিও প্রস্তুত!",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ভিডিওটি আপনার ফোনের গ্যালারিতে (Movies/QuranReader) সফলভাবে সংরক্ষিত হয়েছে।",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ভিডিও শেয়ার করুন", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCreateAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("নতুন ভিডিও বানান", fontSize = 14.sp)
            }
        }
    }
}
