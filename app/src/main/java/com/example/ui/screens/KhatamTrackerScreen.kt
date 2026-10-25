package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen
import com.example.utils.KhatamPlan
import com.example.utils.KhatamTrackerManager
import com.example.utils.WaqtPacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatamTrackerScreen(
    onBackClick: () -> Unit,
    onNavigateToHifz: () -> Unit,
    onNavigateToReader: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        KhatamTrackerManager.initialize(context)
    }

    val plan by KhatamTrackerManager.planFlow.collectAsState()
    val waqtPacing by KhatamTrackerManager.waqtPacingFlow.collectAsState()

    var showDuaDialog by remember { mutableStateOf(false) }
    var showPlanChangeDialog by remember { mutableStateOf(false) }
    var showCustomPageDialog by remember { mutableStateOf(false) }

    val passedDays = maxOf(0, ((System.currentTimeMillis() - plan.startDate) / (1000 * 60 * 60 * 24)).toInt())
    val remainingDays = maxOf(1, plan.targetDays - passedDays)
    val progressFraction = (plan.pagesRead.toFloat() / plan.totalPages.toFloat()).coerceIn(0f, 1f)
    val currentJuz = (plan.pagesRead / 20) + 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "কুরআন খতম ট্র্যাকার",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${plan.title} • ওয়াক্তভিত্তিক পেসিং",
                            fontSize = 11.sp,
                            color = PrimaryGreen
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Quick Switch to Hifz Tracker
                    Surface(
                        onClick = onNavigateToHifz,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "হিফজ ট্র্যাকার",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // --- 1. Completion Celebration Banner (if completed) ---
            if (plan.isCompleted) {
                item {
                    CelebrationCard(onReadDua = { showDuaDialog = true })
                }
            }

            // --- 2. Main Progress Hero Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Indicator
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(80.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    strokeWidth = 7.dp
                                )
                                CircularProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier.fillMaxSize(),
                                    color = PrimaryGreen,
                                    strokeWidth = 7.dp
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(progressFraction * 100).toInt()}%",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "সম্পন্ন",
                                        fontSize = 9.sp,
                                        color = PrimaryGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Key Metrics
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${plan.pagesRead} / ${plan.totalPages} পৃষ্ঠা",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "চলমান: পারা ${currentJuz.coerceAtMost(30)}",
                                    fontSize = 12.sp,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("অবশিষ্ট দিন", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$remainingDays দিন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("অতিক্রান্ত দিন", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$passedDays দিন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("স্ট্রিক", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${plan.streak} দিন 🔥", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Linear progress bar
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryGreen,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // --- 3. 5-Waqt Daily Routine Pacing Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "৫ ওয়াক্তের দৈনিক রুটিন",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "আজকের লক্ষ্য: ${waqtPacing.dailyTargetPages} পৃষ্ঠা",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Waqt Items Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WaqtPill(name = "ফজর", pages = waqtPacing.fajarPages, isDone = waqtPacing.isFajarDone) {
                                KhatamTrackerManager.toggleWaqt(context, 0)
                            }
                            WaqtPill(name = "জোহর", pages = waqtPacing.dhuhrPages, isDone = waqtPacing.isDhuhrDone) {
                                KhatamTrackerManager.toggleWaqt(context, 1)
                            }
                            WaqtPill(name = "আসর", pages = waqtPacing.asrPages, isDone = waqtPacing.isAsrDone) {
                                KhatamTrackerManager.toggleWaqt(context, 2)
                            }
                            WaqtPill(name = "মাগরিব", pages = waqtPacing.maghribPages, isDone = waqtPacing.isMaghribDone) {
                                KhatamTrackerManager.toggleWaqt(context, 3)
                            }
                            WaqtPill(name = "এশা", pages = waqtPacing.ishaPages, isDone = waqtPacing.isIshaDone) {
                                KhatamTrackerManager.toggleWaqt(context, 4)
                            }
                        }
                    }
                }
            }

            // --- 4. Quick Add Pages & Auto-Sync Section ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "কুইক তিলাওয়াত লগ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { KhatamTrackerManager.addPages(context, 1) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("+১ পৃষ্ঠা", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Button(
                                onClick = { KhatamTrackerManager.addPages(context, 5) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("+৫ পৃষ্ঠা", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Button(
                                onClick = { KhatamTrackerManager.addPages(context, 20) },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("+১ পারা (২০পৃ.)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }

                            OutlinedIconButton(
                                onClick = { showCustomPageDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Custom input", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto Sync Switch Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "কুরআন রিডার থেকে অটো-সিঙ্ক",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "কুরআন পাঠ করার সাথে সাথে খতম প্রগ্রেস স্বয়ংক্রিয় আপডেট হবে।",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = plan.autoSyncEnabled,
                                onCheckedChange = { KhatamTrackerManager.toggleAutoSync(context, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Continue reading shortcut
                        Button(
                            onClick = onNavigateToReader,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("সরাসরি কুরআনে পড়া শুরু করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- 5. Plan Configuration & Dua Khatam Actions ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPlanChangeDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("প্ল্যান পরিবর্তন", color = PrimaryGreen, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showDuaDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("খতমে কুরআনের দুআ", fontSize = 12.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // --- Dialogs ---
    if (showDuaDialog) {
        DuaKhatmulQuranDialog(onDismiss = { showDuaDialog = false })
    }

    if (showPlanChangeDialog) {
        ChangePlanDialog(
            currentDays = plan.targetDays,
            onDismiss = { showPlanChangeDialog = false },
            onSelectPlan = { days, name ->
                KhatamTrackerManager.updatePlan(context, days, name)
                showPlanChangeDialog = false
            }
        )
    }

    if (showCustomPageDialog) {
        CustomPageDialog(
            currentPage = plan.pagesRead,
            totalPages = plan.totalPages,
            onDismiss = { showCustomPageDialog = false },
            onSave = {
                KhatamTrackerManager.setDirectPages(context, it)
                showCustomPageDialog = false
            }
        )
    }
}

@Composable
fun WaqtPill(name: String, pages: Int, isDone: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isDone) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (isDone) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.width(60.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
        ) {
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDone) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$pages পৃ.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CelebrationCard(onReadDua: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46)),
        border = BorderStroke(1.dp, Color(0xFF34D399))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉 আলহামদুলিল্লাহ! খতম সম্পন্ন 🎉", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "আল্লাহ আপনার কুরআন তিলাওয়াত কবুল করুন। এখনই খতমে কুরআনের বিশেষ দুআ পাঠ করুন।",
                fontSize = 12.sp,
                color = Color(0xFFD1FAE5),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onReadDua,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("খতমে কুরআনের দুআ পড়ুন", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DuaKhatmulQuranDialog(onDismiss: () -> Unit) {
    val (arabic, transliteration, bangla) = KhatamTrackerManager.getDuaKhatmulQuran()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "খতমে কুরআনের বিশেষ দুআ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Arabic
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryGreen.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = arabic,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Right,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("উচ্চারণ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                Text(transliteration, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.height(10.dp))

                Text("অর্থ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                Text(bangla, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("আমিন")
            }
        }
    )
}

@Composable
fun ChangePlanDialog(
    currentDays: Int,
    onDismiss: () -> Unit,
    onSelectPlan: (Int, String) -> Unit
) {
    val plans = listOf(
        Pair("৩০ দিনে খতম (রমজান/দ্রুত)", 30),
        Pair("৬০ দিনে খতম (২ মাস)", 60),
        Pair("৯০ দিনে খতম (৩ মাস)", 90),
        Pair("১৮০ দিনে খতম (৬ মাস)", 180),
        Pair("৩৬৫ দিনে খতম (১ বছর)", 365)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন খতম প্ল্যান নির্বাচন", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plans.forEach { (name, days) ->
                    val isSelected = currentDays == days
                    Surface(
                        onClick = { onSelectPlan(days, name) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun CustomPageDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var pageInput by remember { mutableStateOf(currentPage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("পৃষ্ঠা সংখ্যা সেট করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("বর্তমানে কত পৃষ্ঠা পড়া সম্পন্ন হয়েছে তা লিখুন (১ থেকে $totalPages):", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = pageInput.toIntOrNull() ?: currentPage
                    onSave(p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
