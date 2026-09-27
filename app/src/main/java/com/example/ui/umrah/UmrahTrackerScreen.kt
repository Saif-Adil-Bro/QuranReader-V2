package com.example.ui.umrah

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.umrah.UmrahContentData
import com.example.data.umrah.UmrahSessionEntity
import com.example.utils.DateUtil

enum class UmrahSubScreen {
    MAIN_TRACKER,
    GUIDELINES,
    HISTORY,
    SESSION_DETAIL,
    POST_DUAS,
    ZAMZAM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmrahTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: UmrahTrackerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentSubScreen by remember { mutableStateOf(UmrahSubScreen.MAIN_TRACKER) }
    var selectedSessionIdForDetail by remember { mutableStateOf<Long?>(null) }
    var showProhibitionsSheet by remember { mutableStateOf(false) }
    var showStepperOverview by remember { mutableStateOf(false) }

    BackHandler {
        if (currentSubScreen != UmrahSubScreen.MAIN_TRACKER) {
            currentSubScreen = UmrahSubScreen.MAIN_TRACKER
        } else if (showStepperOverview) {
            showStepperOverview = false
        } else {
            onNavigateBack()
        }
    }

    when (currentSubScreen) {
        UmrahSubScreen.GUIDELINES -> {
            UmrahGuidelinesScreen(
                onNavigateBack = { currentSubScreen = UmrahSubScreen.MAIN_TRACKER }
            )
            return
        }
        UmrahSubScreen.HISTORY -> {
            UmrahHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { currentSubScreen = UmrahSubScreen.MAIN_TRACKER },
                onSelectSession = { id ->
                    selectedSessionIdForDetail = id
                    currentSubScreen = UmrahSubScreen.SESSION_DETAIL
                }
            )
            return
        }
        UmrahSubScreen.SESSION_DETAIL -> {
            selectedSessionIdForDetail?.let { sId ->
                UmrahSessionDetailScreen(
                    sessionId = sId,
                    onNavigateBack = { currentSubScreen = UmrahSubScreen.HISTORY }
                )
            }
            return
        }
        UmrahSubScreen.POST_DUAS -> {
            UmrahPostDuasScreen(
                onNavigateBack = { currentSubScreen = UmrahSubScreen.MAIN_TRACKER }
            )
            return
        }
        UmrahSubScreen.ZAMZAM -> {
            UmrahZamzamScreen(
                onNavigateBack = { currentSubScreen = UmrahSubScreen.MAIN_TRACKER }
            )
            return
        }
        UmrahSubScreen.MAIN_TRACKER -> {
            // Main content continues below
        }
    }

    val session = uiState.activeSession
    val isNotStarted = session == null || session.currentStep == "NOT_STARTED"
    val isCompleted = session?.isCompleted == true || session?.currentStep == "COMPLETED"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isNotStarted || isCompleted -> "উমরাহ ট্র্যাকার"
                            session?.currentStep == "IHRAM" -> "ইহরাম"
                            session?.currentStep == "TAWAF" -> "তাওয়াফের বিস্তারিত"
                            session?.currentStep == "SAI" -> "সা'য়ী-এর বিবরণ"
                            session?.currentStep == "HALQ" -> "হলক / কসর"
                            else -> "উমরাহ ট্র্যাকার"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showStepperOverview) {
                            showStepperOverview = false
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { currentSubScreen = UmrahSubScreen.HISTORY }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "ইতিহাস",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F181F)
                )
            )
        },
        containerColor = Color(0xFF0F181F)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // 1. NOT STARTED (Screenshots 1 & 2)
                isNotStarted -> {
                    NotStartedContent(
                        onBeginUmrah = { viewModel.startUmrah() },
                        onReviewGuidelines = { currentSubScreen = UmrahSubScreen.GUIDELINES }
                    )
                }

                // 2. IN PROGRESS - OVERVIEW STEPPER (Screenshot 3)
                showStepperOverview -> {
                    session?.let { activeSession ->
                        StepperOverviewContent(
                            session = activeSession,
                            onContinue = { showStepperOverview = false }
                        )
                    }
                }

                // 3. STEP 1: IHRAM (Screenshots 6 & 7)
                session?.currentStep == "IHRAM" -> {
                    IhramStepContent(
                        checklist = uiState.checklistItems.filter { it.stepKey == "IHRAM" },
                        onToggleChecklist = { idx, checked ->
                            viewModel.toggleChecklistItem("IHRAM", idx, checked)
                        },
                        onViewProhibitions = { showProhibitionsSheet = true },
                        onCompleteIhram = { viewModel.completeIhramAndGoToTawaf() },
                        onViewOverview = { showStepperOverview = true }
                    )
                }

                // 4. STEP 2: TAWAF (Screenshots 4, 5, 8, 9, 10, 11, 12)
                session?.currentStep == "TAWAF" -> {
                    TawafStepContent(
                        session = session,
                        prepChecklist = uiState.checklistItems.filter { it.stepKey == "TAWAF_PREP" },
                        roundChecklist = uiState.checklistItems.filter { it.stepKey == "TAWAF_ROUND" },
                        finalChecklist = uiState.checklistItems.filter { it.stepKey == "TAWAF_FINAL" },
                        onToggleChecklist = { stepKey, idx, checked ->
                            viewModel.toggleChecklistItem(stepKey, idx, checked)
                        },
                        onSelectRound = { round -> viewModel.setTawafRound(round) },
                        onCompleteRound = { round -> viewModel.completeTawafRound(round) },
                        onFinishTawaf = { viewModel.completeTawafAndGoToSai() }
                    )
                }

                // 5. STEP 3: SA'I (Screenshots 13, 14, 15, 16)
                session?.currentStep == "SAI" -> {
                    SaiStepContent(
                        session = session,
                        prepChecklist = uiState.checklistItems.filter { it.stepKey == "SAI_PREP" },
                        roundChecklist = uiState.checklistItems.filter { it.stepKey == "SAI_ROUND" },
                        finalChecklist = uiState.checklistItems.filter { it.stepKey == "SAI_FINAL" },
                        onToggleChecklist = { stepKey, idx, checked ->
                            viewModel.toggleChecklistItem(stepKey, idx, checked)
                        },
                        onSelectRound = { round -> viewModel.setSaiRound(round) },
                        onCompleteRound = { round -> viewModel.completeSaiRound(round) },
                        onFinishSai = { viewModel.completeSaiAndGoToHalq() }
                    )
                }

                // 6. STEP 4: HALQ / QASR (Screenshots 17 & 18)
                session?.currentStep == "HALQ" -> {
                    HalqStepContent(
                        onCompleteUmrah = { viewModel.completeUmrah() }
                    )
                }

                // 7. COMPLETED (Screenshot 19)
                isCompleted -> {
                    UmrahCompletedContent(
                        onNavigateToDuas = { currentSubScreen = UmrahSubScreen.POST_DUAS },
                        onNavigateToZamzam = { currentSubScreen = UmrahSubScreen.ZAMZAM },
                        onStartNewUmrah = { viewModel.resetOrStartNewUmrah() }
                    )
                }
            }
        }
    }

    if (showProhibitionsSheet) {
        UmrahProhibitionsDialog(
            onDismiss = { showProhibitionsSheet = false }
        )
    }
}

// ==================== 1. NOT STARTED CONTENT ====================
@Composable
fun NotStartedContent(
    onBeginUmrah: () -> Unit,
    onReviewGuidelines: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Your Journey Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "আপনার সফর",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আল্লাহ কবুল করুন",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E2D3A)
                        ) {
                            Text(
                                text = "শুরু হয়নি",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 0% Circular progress indicator
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .border(6.dp, Color(0xFF1E2D3A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "০%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Preparation Section
            Text(
                text = "প্রস্তুতি",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReviewGuidelines() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF0F766E).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "নির্দেশিকা দেখুন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "শুরু করার আগে নিয়ম, বিধি এবং নিষেধ সম্পর্কে জেনে নিন।",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Bottom Begin Button
        Button(
            onClick = onBeginUmrah,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(bottom = 6.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "উমরাহ শুরু করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==================== 2. OVERVIEW STEPPER CONTENT (Screenshot 3) ====================
@Composable
fun StepperOverviewContent(
    session: UmrahSessionEntity,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "বর্তমান অগ্রগতি",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )

            // Current Step Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (session.currentStep) {
                                "IHRAM" -> "ধাপ ১ (৪ এর মধ্যে)"
                                "TAWAF" -> "ধাপ ২ (৪ এর মধ্যে)"
                                "SAI" -> "ধাপ ৩ (৪ এর মধ্যে)"
                                "HALQ" -> "ধাপ ৪ (৪ এর মধ্যে)"
                                else -> "চলমান ধাপ"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F766E).copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = "চলমান",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2DD4BF),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress circle
                    val progressPercent = when (session.currentStep) {
                        "IHRAM" -> "০%"
                        "TAWAF" -> "${DateUtil.toBengaliNumerals((session.currentTawafRound * 100) / 28)}%"
                        "SAI" -> "${DateUtil.toBengaliNumerals(50 + (session.currentSaiRound * 50) / 7)}%"
                        "HALQ" -> "৯০%"
                        else -> "১০০%"
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(6.dp, Color(0xFF1E2D3A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = progressPercent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = when (session.currentStep) {
                            "IHRAM" -> "ইহরাম"
                            "TAWAF" -> "তাওয়াফ"
                            "SAI" -> "সাঈ"
                            "HALQ" -> "হলক / কসর"
                            else -> "উমরাহ"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (session.currentStep) {
                            "IHRAM" -> "নিয়ত ও পবিত্রতা বজায় রাখুন"
                            "TAWAF" -> "কাবা শরীফ প্রদক্ষিণ করুন"
                            "SAI" -> "সাফা ও মারওয়া পাহাড়ের মধ্যে সাঈ করুন"
                            "HALQ" -> "চুল কেটে উমরাহ সমাপ্ত করুন"
                            else -> ""
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${when (session.currentStep) {
                                    "IHRAM" -> "ইহরাম"
                                    "TAWAF" -> "তাওয়াফ"
                                    "SAI" -> "সাঈ"
                                    "HALQ" -> "হলক / কসর"
                                    else -> "ধাপ"
                                }} সম্পন্ন করে এগিয়ে যান",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Stepper timeline section
            Text(
                text = "ভ্রমণসূচী",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )

            StepperTimelineCard(currentStep = session.currentStep)
        }
    }
}

@Composable
fun StepperTimelineCard(currentStep: String) {
    val steps = listOf(
        Pair("১. ইহরাম", "IHRAM"),
        Pair("২. তাওয়াফ", "TAWAF"),
        Pair("৩. সাঈ", "SAI"),
        Pair("৪. হলক / কসর", "HALQ")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            steps.forEachIndexed { index, (title, key) ->
                val isCurrent = key == currentStep
                val isDone = isStepCompleted(key, currentStep)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                when {
                                    isDone -> Color(0xFF10B981)
                                    isCurrent -> Color(0xFF0F766E).copy(alpha = 0.4f)
                                    else -> Color.Transparent
                                },
                                CircleShape
                            )
                            .border(
                                1.5.dp,
                                if (isDone || isCurrent) Color(0xFF10B981) else Color(0xFF334155),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else if (isCurrent) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent) Color(0xFF34D399) else if (isDone) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = when {
                            isDone -> "সম্পন্ন"
                            isCurrent -> "চলমান ➔"
                            else -> "অপেক্ষমান"
                        },
                        fontSize = 12.sp,
                        color = if (isCurrent) Color(0xFF34D399) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

fun isStepCompleted(stepKey: String, currentStep: String): Boolean {
    val order = listOf("IHRAM", "TAWAF", "SAI", "HALQ", "COMPLETED")
    return order.indexOf(currentStep) > order.indexOf(stepKey)
}

// ==================== 3. STEP 1: IHRAM CONTENT (Screenshots 6 & 7) ====================
@Composable
fun IhramStepContent(
    checklist: List<com.example.data.umrah.UmrahChecklistEntity>,
    onToggleChecklist: (Int, Boolean) -> Unit,
    onViewProhibitions: () -> Unit,
    onCompleteIhram: () -> Unit,
    onViewOverview: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // Step Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ধাপ ১: ইহরাম",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F766E).copy(alpha = 0.35f)
                ) {
                    Text(
                        text = "চলমান",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2DD4BF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Info Banner with Prohibitions Link
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF09332A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "আপনি ইহরামের অবস্থায় প্রবেশ করেছেন। তাওয়াফ শুরু করার জন্য কাবাপ্রাঙ্গণে না পৌঁছানো পর্যন্ত ঘনঘন তালবিয়া পাঠ করতে থাকুন।",
                            fontSize = 13.sp,
                            color = Color(0xFFD1FAE5),
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewProhibitions() },
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "নিষিদ্ধ কাজগুলো দেখে নিন >",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34D399)
                        )
                    }
                }
            }
        }

        // Preparation Section
        item {
            Text(
                text = "প্রস্তুতি",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )
        }

        // Checklist items
        itemsIndexed(UmrahContentData.IHRAM_CHECKLIST) { index, itemText ->
            val entity = checklist.find { it.itemIndex == index }
            val isChecked = entity?.isChecked == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleChecklist(index, !isChecked) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                        .background(if (isChecked) Color(0xFF10B981) else Color.Transparent, CircleShape)
                        .border(1.5.dp, if (isChecked) Color(0xFF10B981) else Color(0xFF64748B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Text(
                    text = itemText,
                    fontSize = 14.sp,
                    color = if (isChecked) Color(0xFF94A3B8) else Color(0xFFE2E8F0),
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Niyyah Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "নিয়ত (সংকল্প)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = UmrahContentData.IHRAM_NIYYAH_ARABIC,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = UmrahContentData.IHRAM_NIYYAH_BN,
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Talbiyah Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "তালবিয়া",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = UmrahContentData.TALBIYAH_ARABIC,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        lineHeight = 36.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = UmrahContentData.TALBIYAH_BN,
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // Bottom Proceed Button
        item {
            Button(
                onClick = onCompleteIhram,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(
                    text = "ইহরাম সম্পন্ন করে এগিয়ে যান",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==================== 4. STEP 2: TAWAF CONTENT (Screenshots 4, 5, 8, 9, 10, 11, 12) ====================
@Composable
fun TawafStepContent(
    session: UmrahSessionEntity,
    prepChecklist: List<com.example.data.umrah.UmrahChecklistEntity>,
    roundChecklist: List<com.example.data.umrah.UmrahChecklistEntity>,
    finalChecklist: List<com.example.data.umrah.UmrahChecklistEntity>,
    onToggleChecklist: (String, Int, Boolean) -> Unit,
    onSelectRound: (Int) -> Unit,
    onCompleteRound: (Int) -> Unit,
    onFinishTawaf: () -> Unit
) {
    val round = session.currentTawafRound

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // Status Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "তাওয়াফ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F766E).copy(alpha = 0.35f)
                ) {
                    Text(
                        text = "চলমান",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2DD4BF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Current Progress Title
        item {
            Column {
                Text(
                    text = "বর্তমান অগ্রগতি",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "৭ এর মধ্যে ${DateUtil.toBengaliNumerals(round)} তম চক্কর",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 1 to 7 Round Chips (১, ২, ৩, ৪, ৫, ৬, ৭)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..7).forEach { r ->
                    val isActive = r == round
                    val isPast = r < round

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> Color(0xFF0F766E)
                                    isPast -> Color(0xFF064E3B)
                                    else -> Color(0xFF1E2D3A)
                                }
                            )
                            .border(
                                1.5.dp,
                                if (isActive) Color(0xFF2DD4BF) else Color.Transparent,
                                CircleShape
                            )
                            .clickable { onSelectRound(r) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = com.example.utils.DateUtil.toBengaliNumerals("$r"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive || isPast) Color(0xFF2DD4BF) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // If Round 1: Show Preparation Checklist (Screenshot 4)
        if (round == 1) {
            item {
                Text(
                    text = "প্রস্তুতি",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(UmrahContentData.TAWAF_PREP_CHECKLIST) { index, itemText ->
                val entity = prepChecklist.find { it.itemIndex == index }
                val isChecked = entity?.isChecked == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleChecklist("TAWAF_PREP", index, !isChecked) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(22.dp)
                            .background(if (isChecked) Color(0xFF10B981) else Color.Transparent, CircleShape)
                            .border(1.5.dp, if (isChecked) Color(0xFF10B981) else Color(0xFF64748B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Text(
                        text = itemText,
                        fontSize = 13.sp,
                        color = if (isChecked) Color(0xFF94A3B8) else Color(0xFFE2E8F0),
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Current round checklist items
        item {
            Text(
                text = "বর্তমান অগ্রগতি",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF10B981),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        itemsIndexed(UmrahContentData.TAWAF_ROUND_CHECKLIST) { index, itemText ->
            val entity = roundChecklist.find { it.itemIndex == index }
            val isChecked = entity?.isChecked == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleChecklist("TAWAF_ROUND", index, !isChecked) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                        .background(if (isChecked) Color(0xFF10B981) else Color.Transparent, CircleShape)
                        .border(1.5.dp, if (isChecked) Color(0xFF10B981) else Color(0xFF64748B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Text(
                    text = itemText,
                    fontSize = 13.sp,
                    color = if (isChecked) Color(0xFF94A3B8) else Color(0xFFE2E8F0),
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Green banner for Dua between Rukn Yamani and Hajr Aswad
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F4236))
            ) {
                Text(
                    text = "তাওয়াফের প্রতি চক্করে রুকন-ই ইয়ামানি এবং হাজরে আসওয়াদ এর মাঝখানে অনবরত নিচের দোয়াটি পাঠ করুন।",
                    fontSize = 13.sp,
                    color = Color(0xFFD1FAE5),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Dua Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "রুকন-ই ইয়ামানি ও হাজরে আসওয়াদ-এর মাঝখানের দুয়া",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = UmrahContentData.TAWAF_RUKN_YAMANI_DUA_ARABIC,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        lineHeight = 36.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = UmrahContentData.TAWAF_RUKN_YAMANI_DUA_BN,
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // If Round 7: Show Final Step Checklist (Screenshot 12)
        if (round == 7) {
            item {
                Text(
                    text = "তাওয়াফের শেষ ধাপ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(UmrahContentData.TAWAF_FINAL_CHECKLIST) { index, itemText ->
                val entity = finalChecklist.find { it.itemIndex == index }
                val isChecked = entity?.isChecked == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleChecklist("TAWAF_FINAL", index, !isChecked) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(22.dp)
                            .background(if (isChecked) Color(0xFF10B981) else Color.Transparent, CircleShape)
                            .border(1.5.dp, if (isChecked) Color(0xFF10B981) else Color(0xFF64748B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Text(
                        text = itemText,
                        fontSize = 13.sp,
                        color = if (isChecked) Color(0xFF94A3B8) else Color(0xFFE2E8F0),
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bottom Action Button
        item {
            Button(
                onClick = {
                    if (round < 7) {
                        onCompleteRound(round)
                    } else {
                        onFinishTawaf()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(
                    text = if (round < 7) "${DateUtil.toBengaliNumerals(round)} তম চক্কর সম্পন্ন করুন" else "তাওয়াফ শেষ করুন",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==================== 5. STEP 3: SA'I CONTENT (Screenshots 13, 14, 15, 16) ====================
@Composable
fun SaiStepContent(
    session: UmrahSessionEntity,
    prepChecklist: List<com.example.data.umrah.UmrahChecklistEntity>,
    roundChecklist: List<com.example.data.umrah.UmrahChecklistEntity>,
    finalChecklist: List<com.example.data.umrah.UmrahChecklistEntity>,
    onToggleChecklist: (String, Int, Boolean) -> Unit,
    onSelectRound: (Int) -> Unit,
    onCompleteRound: (Int) -> Unit,
    onFinishSai: () -> Unit
) {
    val round = session.currentSaiRound

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // Status Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "সাঈ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F766E).copy(alpha = 0.35f)
                ) {
                    Text(
                        text = if (round == 0) "শুরু করার জন্য প্রস্তুত" else "চলমান",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2DD4BF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Current Progress Title
        item {
            Column {
                Text(
                    text = "বর্তমান অগ্রগতি",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (round == 0) "০/৭ চক্কর" else "৭ এর মধ্যে ${DateUtil.toBengaliNumerals(round)} তম চক্কর (সাঈ)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Direction indicator: সাফা <-> মারওয়াহ
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "সাফা", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2DD4BF))
                Text(text = "মারওয়াহ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2DD4BF))
            }
        }

        // 1 to 7 Round Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..7).forEach { r ->
                    val isActive = r == round
                    val isPast = r < round

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> Color(0xFF0F766E)
                                    isPast -> Color(0xFF064E3B)
                                    else -> Color(0xFF1E2D3A)
                                }
                            )
                            .border(
                                1.5.dp,
                                if (isActive) Color(0xFF2DD4BF) else Color.Transparent,
                                CircleShape
                            )
                            .clickable { onSelectRound(r) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = com.example.utils.DateUtil.toBengaliNumerals("$r"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive || isPast) Color(0xFF2DD4BF) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Green light notice banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F4236))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "সবুজ বাতি এলাকা",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "সবুজ চিহ্নিত এলাকায় পুরুষদের কিছুটা দ্রুত হাঁটা বা দৌড়ানো সুন্নত। মহিলাদের জন্য স্বাভাবিক গতিতে হাঁটা নিয়ম।",
                        fontSize = 13.sp,
                        color = Color(0xFFD1FAE5),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // If Round 0: Show Preparation Checklist (Screenshot 13)
        if (round == 0) {
            item {
                Text(
                    text = "প্রস্তুতি",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(UmrahContentData.SAI_PREP_CHECKLIST) { index, itemText ->
                val entity = prepChecklist.find { it.itemIndex == index }
                val isChecked = entity?.isChecked == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleChecklist("SAI_PREP", index, !isChecked) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(22.dp)
                            .background(if (isChecked) Color(0xFF10B981) else Color.Transparent, CircleShape)
                            .border(1.5.dp, if (isChecked) Color(0xFF10B981) else Color(0xFF64748B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Text(
                        text = itemText,
                        fontSize = 13.sp,
                        color = if (isChecked) Color(0xFF94A3B8) else Color(0xFFE2E8F0),
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Current round checklist items
        item {
            Text(
                text = "বর্তমান অগ্রগতি",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF10B981),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        itemsIndexed(UmrahContentData.SAI_ROUND_CHECKLIST) { index, itemText ->
            val entity = roundChecklist.find { it.itemIndex == index }
            val isChecked = entity?.isChecked == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleChecklist("SAI_ROUND", index, !isChecked) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                        .background(if (isChecked) Color(0xFF10B981) else Color.Transparent, CircleShape)
                        .border(1.5.dp, if (isChecked) Color(0xFF10B981) else Color(0xFF64748B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Text(
                    text = itemText,
                    fontSize = 13.sp,
                    color = if (isChecked) Color(0xFF94A3B8) else Color(0xFFE2E8F0),
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Safa-Marwah Dua Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "সাফা ও মারওয়াহ-এর দুয়া",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = UmrahContentData.SAI_SAFA_MARWAH_DUA_ARABIC,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        lineHeight = 36.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = UmrahContentData.SAI_SAFA_MARWAH_DUA_BN,
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // If Round 7: Show Final Step Checklist (Screenshot 16)
        if (round == 7) {
            item {
                Text(
                    text = "সাঈর শেষ ধাপ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(UmrahContentData.SAI_FINAL_CHECKLIST) { index, itemText ->
                val entity = finalChecklist.find { it.itemIndex == index }
                val isChecked = entity?.isChecked == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleChecklist("SAI_FINAL", index, !isChecked) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(22.dp)
                            .background(if (isChecked) Color(0xFF10B981) else Color.Transparent, CircleShape)
                            .border(1.5.dp, if (isChecked) Color(0xFF10B981) else Color(0xFF64748B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Text(
                        text = itemText,
                        fontSize = 13.sp,
                        color = if (isChecked) Color(0xFF94A3B8) else Color(0xFFE2E8F0),
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bottom Action Button
        item {
            Button(
                onClick = {
                    if (round == 0) {
                        onSelectRound(1)
                    } else if (round < 7) {
                        onCompleteRound(round)
                    } else {
                        onFinishSai()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(
                    text = when (round) {
                        0 -> "১ম চক্কর শুরু করুন"
                        in 1..6 -> "${DateUtil.toBengaliNumerals(round)} তম চক্কর সম্পন্ন করুন"
                        else -> "সাঈ শেষ করুন"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==================== 6. STEP 4: HALQ / QASR CONTENT (Screenshots 17 & 18) ====================
@Composable
fun HalqStepContent(
    onCompleteUmrah: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // Step Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ধাপ ৪: হলক / কসর",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F766E).copy(alpha = 0.35f)
                ) {
                    Text(
                        text = "চলমান",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2DD4BF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Top Banner Notice
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF09332A))
            ) {
                Text(
                    text = "আপনি শেষ ধাপে আছেন। এটি সম্পন্ন করার পর আপনি ইহরাম অবস্থা থেকে বের হবেন এবং আপনার উমরাহ সম্পন্ন হবে।",
                    fontSize = 13.sp,
                    color = Color(0xFFD1FAE5),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Definitions Section (সংজ্ঞা)
        item {
            Text(
                text = "সংজ্ঞা",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = UmrahContentData.HALQ_DEF_TITLE,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = UmrahContentData.HALQ_DEF_DESC,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = UmrahContentData.QASR_DEF_TITLE,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = UmrahContentData.QASR_DEF_DESC,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Men's Guidelines
        item {
            Text(
                text = "পুরুষদের জন্য নির্দেশিকা",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        itemsIndexed(UmrahContentData.HALQ_MEN_GUIDELINES) { _, guideline ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                )
                Text(
                    text = guideline,
                    fontSize = 13.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Women's Guidelines
        item {
            Text(
                text = "মহিলাদের জন্য নির্দেশিকা",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        itemsIndexed(UmrahContentData.HALQ_WOMEN_GUIDELINES) { _, guideline ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                )
                Text(
                    text = guideline,
                    fontSize = 13.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Red Warning Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3F191E))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = UmrahContentData.HALQ_WARNING_TEXT,
                        fontSize = 13.sp,
                        color = Color(0xFFFECACA),
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Dua Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "হলক / কসর পরবর্তী দুয়া",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = UmrahContentData.HALQ_DUA_ARABIC,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        lineHeight = 36.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = UmrahContentData.HALQ_DUA_BN,
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // Complete Umrah Button
        item {
            Button(
                onClick = onCompleteUmrah,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(
                    text = "উমরাহ সম্পন্ন করুন",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==================== 7. COMPLETED CONTENT (Screenshot 19) ====================
@Composable
fun UmrahCompletedContent(
    onNavigateToDuas: () -> Unit,
    onNavigateToZamzam: () -> Unit,
    onStartNewUmrah: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // Big Green Celebration Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "আলহামদুলিল্লাহ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "উমরাহ সম্পন্ন",
                            fontSize = 14.sp,
                            color = Color(0xFFD1FAE5)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF064E3B)
                        ) {
                            Text(
                                text = "সম্পন্ন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 100% Circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(6.dp, Color(0xFF2DD4BF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "১০০%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Summary of Steps Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "উমরাহর ধাপসমূহ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    CompletedStepItem("ইহরাম", "নিয়ত ও পবিত্রতা বজায় রাখা হয়েছে")
                    CompletedStepItem("তাওয়াফ", "৭ চক্কর সম্পন্ন")
                    CompletedStepItem("সাঈ", "৭টি চক্কর সম্পন্ন হয়েছে")
                    CompletedStepItem("হলক / কসর", "চুল কাটা সম্পন্ন")
                }
            }
        }

        // Post-Umrah Section Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16232D))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "উমরাহ পরবর্তী",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    // Duas Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDuas() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "দোয়াসমূহ",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "উমরাহ সম্পন্ন করার পর সুপারিশকৃত দুয়াসমূহ।",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E2D3A), thickness = 0.8.dp)

                    // Zamzam Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToZamzam() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "জমজমের পানি",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "জমজম পানি পান করুন এবং প্রচুর দুআ করুন।",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Start New Umrah Button
        item {
            OutlinedButton(
                onClick = onStartNewUmrah,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
            ) {
                Text(
                    text = "নতুন উমরাহ শুরু করুন",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
fun CompletedStepItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF064E3B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F766E).copy(alpha = 0.35f)
        ) {
            Text(
                text = "সম্পন্ন",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2DD4BF),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
