package com.example.ui.screens


import androidx.compose.ui.text.withStyle

import androidx.compose.ui.text.withStyle


import android.widget.Toast

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.BookmarkEntity
import com.example.data.model.Surah
import com.example.ui.theme.*
import com.example.ui.viewmodels.GamePhase
import com.example.ui.viewmodels.GameSource
import com.example.ui.viewmodels.GameType
import com.example.ui.viewmodels.WordGameConfig
import com.example.ui.viewmodels.SettingsViewModel
import com.example.ui.viewmodels.UserNote
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.model.appendStyledWaqfText

data class MenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

data class MenuCategory(
    val title: String,
    val icon: ImageVector,
    val items: List<MenuItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSurah: (Int) -> Unit = {},
    onNavigateToPage: (Int) -> Unit = {},
    onNavigateToJuz: (Int) -> Unit = {},
    onNavigateToAyah: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToPlayer: () -> Unit = {},
    onNavigateToPosts: () -> Unit = {},
    onNavigateToMushafPage: (String, Int) -> Unit = { _, _ -> },
    initialSubScreen: String? = null,
    initialDuaId: Int? = null,
    highlightHijriAdjustment: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scrollState = rememberScrollState()
    var shouldHighlightHijri by remember { mutableStateOf(highlightHijriAdjustment) }

    LaunchedEffect(highlightHijriAdjustment) {
        if (highlightHijriAdjustment) {
            // Scroll down to the general settings area
            try {
                scrollState.animateScrollTo(1400)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Keep the pulsing highlight effect for 5 seconds
            kotlinx.coroutines.delay(5000)
            shouldHighlightHijri = false
        }
    }
    
    val showTranslation by viewModel.showTranslation.collectAsState()
    val showTransliteration by viewModel.showTransliteration.collectAsState()
    val showTajweed by viewModel.showTajweed.collectAsState()
    val keepScreenOn by viewModel.keepScreenOnFlow.collectAsState()
    val hijriOffset by viewModel.hijriOffset.collectAsState()
    val combinedHijriOffset by viewModel.combinedHijriOffset.collectAsState()
    val tanzilTextStyle by viewModel.tanzilTextStyle.collectAsState()
    val username by viewModel.username.collectAsState()
    val readingTime by viewModel.readingTimeMinutes.collectAsState()
    val bookmarkList by viewModel.bookmarks.collectAsState(initial = emptyList())
    
    var activeDialog by remember(initialSubScreen) { mutableStateOf<String?>(initialSubScreen) }
    
    val menuCategories = listOf(
        MenuCategory(
            title = "কুরআন শিক্ষা ও মিডিয়া",
            icon = Icons.Default.MenuBook,
            items = listOf(
                MenuItem("subjectwise", "বিষয়ভিত্তিক কুরআন", Icons.Default.Category, Color(0xFF3B82F6)),
                MenuItem("learn", "কুরআন শিক্ষা", Icons.Default.Book, Color(0xFF4F46E5)),
                MenuItem("hifz", "কুরআন হিফজ", Icons.Default.CheckCircle, Color(0xFF6366F1)),
                MenuItem("player", "কুরআন প্লেয়ার", Icons.Default.MusicNote, Color(0xFF06B6D4)),
                MenuItem("video", "ভিডিও এডিটর", Icons.Default.Videocam, Color(0xFFEF4444))
            )
        ),
        MenuCategory(
            title = "দুআ ও ইবাদত",
            icon = Icons.Default.AutoAwesome,
            items = listOf(
                MenuItem("dua", "কুরআনিক দুআ", Icons.Default.Schedule, Color(0xFF8B5CF6)),
                MenuItem("morning_evening_dua", "সকাল সন্ধ্যার দুআ", Icons.Default.WbSunny, Color(0xFFF59E0B)),
                MenuItem("manzil", "মানযিল", Icons.Default.AutoAwesome, Color(0xFF10B981)),
                MenuItem("qibla", "কিবলা কম্পাস", Icons.Default.Explore, Color(0xFFEAB308)),
                MenuItem("prayer_times", "নামাজের সময়সূচি", Icons.Default.AccessTime, Color(0xFF059669)),
                MenuItem("calendar", "ক্যালেন্ডার", Icons.Default.CalendarMonth, Color(0xFF10B981)),
                MenuItem("planner", "কুরআন প্ল্যানার", Icons.Default.DateRange, Color(0xFF10B981))
            )
        ),
        MenuCategory(
            title = "ব্যক্তিগত টুলস",
            icon = Icons.Default.Person,
            items = listOf(
                MenuItem("bookmark", "বুকমার্ক", Icons.Default.Bookmark, Color(0xFFEF4444)),
                MenuItem("note", "নোট", Icons.Default.Edit, Color(0xFF0D9488)),
                MenuItem("game", "ওয়ার্ড গেম", Icons.Default.PlayCircle, Color(0xFFEC4899))
            )
        ),
        MenuCategory(
            title = "অ্যাপ সিস্টেম ও সেটিংস",
            icon = Icons.Default.Settings,
            items = listOf(
                MenuItem("font_settings", "ফন্ট ও তাজভীদ", Icons.Default.FontDownload, Color(0xFF10B981)),
                MenuItem("theme", "অ্যাপ থিম", Icons.Default.Palette, Color(0xFF9C27B0)),
                MenuItem("notifications", "নোটিফিকেশন", Icons.Default.Notifications, Color(0xFFFBBF24)),
                MenuItem("offline_sync", "অফলাইন ডাউনলোড", Icons.Default.Download, Color(0xFFF59E0B)),
                MenuItem("backup", "ব্যাকআপ", Icons.Default.Cloud, Color(0xFF6B7280)),
                MenuItem("about", "সম্পর্কে", Icons.Default.Info, Color(0xFF4CAF50)),
                MenuItem("contact", "যোগাযোগ", Icons.Default.ContactMail, Color(0xFFF97316))
            )
        )
    )
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মেনু অপশন",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GrayText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider(color = Border, thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // 1. Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, PrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .clickable { activeDialog = "profile" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(PrimaryGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Profile Details
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = username,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${bookmarkList.size} বুকমার্ক",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val hoursText = if (readingTime >= 60) {
                                    val hrs = readingTime / 60
                                    val mins = readingTime % 60
                                    if (mins > 0) "$hrs ঘণ্টা $mins মি. পড়া" else "$hrs ঘণ্টা পড়া"
                                } else {
                                    "$readingTime মিনিট পড়া"
                                }
                                Text(
                                    text = hoursText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Right arrow
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Edit Profile",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Islamic Posts & Photo Cards Banner Card (Below Profile Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigateToPosts() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "ইসলামিক ব্লগ ও ফটো কার্ড",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF064E3B)
                            )
                            Text(
                                text = "অনলাইন আপডেট, নসীহত ও কাস্টম ফটো কার্ড",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "নতুন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // 2. Categorized Menu Items Grid
            menuCategories.forEachIndexed { categoryIndex, category ->
                if (categoryIndex > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Category Title Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = category.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val chunkedItems = category.items.chunked(3)
                chunkedItems.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            val isBackup = item.id == "backup"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(if (isBackup) 0.dp else 2.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isBackup) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable {
                                        if (isBackup) {
                                            android.widget.Toast.makeText(context, "শীঘ্রই আসছে!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else if (item.id == "player") {
                                            onNavigateToPlayer()
                                        } else {
                                            activeDialog = item.id
                                        }
                                    }
                                    .alpha(if (isBackup) 0.5f else 1f)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(item.color.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = item.color,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        
                        // Symmetrical spaces if chunk contains less than 3 items
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Border, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            // 3. Settings Segment (Backward Compatibility)
            Text(
                text = "অ্যাপ সেটিংস",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Hijri Date Adjustment
            val hijriBorderColor = if (shouldHighlightHijri) PrimaryGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            val hijriBorderWidth = if (shouldHighlightHijri) 2.dp else 1.dp
            val hijriBgColor = if (shouldHighlightHijri) PrimaryGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = hijriBgColor),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(hijriBorderWidth, hijriBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "হিজরি তারিখ সমন্বয়",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "বর্তমান আরবি তারিখ: ${com.example.utils.DateUtil.getTodayHijriDateStr(combinedHijriOffset)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.setHijriOffset(hijriOffset - 1) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.background, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = if (combinedHijriOffset > 0) "+${com.example.utils.DateUtil.toBengaliNumerals(combinedHijriOffset)}" 
                                   else if (combinedHijriOffset < 0) "-${com.example.utils.DateUtil.toBengaliNumerals(-combinedHijriOffset)}" 
                                   else "০",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { viewModel.setHijriOffset(hijriOffset + 1) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.background, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Hijri Date Information Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "দ্রষ্টব্য",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ইসলামী হিজরি তারিখ সূর্যাস্তের (~সন্ধ্যা ৬টা) পরেই পরবর্তী দিনের জন্য গণনা শুরু হয়। চাঁদ দেখার পার্থক্যের কারণে স্থানীয় তারিখের অমিল দেখা দিলে প্রয়োজনে তারিখ সমন্বয় (-/+) ব্যবহার করতে পারেন।",
                        fontSize = 13.sp,
                        color = Color(0xFF424242),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Text(
                        text = "🌙 নতুন চাঁদ ও হিজরি মাসের নির্ভরযোগ্য আপডেট পেতে আমাদের টেলিগ্রাম চ্যানেলে যুক্ত থাকুন:",
                        fontSize = 13.sp,
                        color = Color(0xFF424242),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://t.me/AlHaqChandDekhaCommittee") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF229ED9)),
                        border = BorderStroke(1.dp, Color(0xFF229ED9)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "https://t.me/AlHaqChandDekhaCommittee",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অনুবাদ প্রদর্শন করুন (Show Translation)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আরবি আয়াতের নিচে বাংলা অনুবাদ প্রদর্শন করুন",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showTranslation,
                        onCheckedChange = { viewModel.toggleTranslation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryGreen
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "শব্দে শব্দে উচ্চারণ (Word Transliteration)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "প্রতিটি শব্দের নিচে বাংলা উচ্চারণ প্রদর্শন করুন",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showTransliteration,
                        onCheckedChange = { viewModel.setShowTransliteration(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "তাজবীদ কালার (Tajweed Colors)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আরবি আয়াতে তাজবীদের নিয়ম অনুযায়ী বিভিন্ন রঙ প্রদর্শন করুন",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showTajweed,
                        onCheckedChange = { viewModel.setShowTajweed(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryGreen
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Keep Screen On settings
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.setKeepScreenOn(!keepScreenOn) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "সবসময় ডিসপ্লে অন রাখুন",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "অ্যাপ ব্যবহার করার সময় স্ক্রিনের আলো নিভবে না",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = { viewModel.setKeepScreenOn(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryGreen
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val availableTafsirs by viewModel.availableTafsirs.collectAsState()
    val availableTranslations by viewModel.availableTranslations.collectAsState()
    val selectedTranslationIds by viewModel.selectedTranslationIds.collectAsState()
            val selectedTafsirIds by viewModel.selectedTafsirIds.collectAsState()
            val downloadedTafsirIds by viewModel.downloadedTafsirIds.collectAsState()
            val downloadingTafsirIds by viewModel.downloadingTafsirIds.collectAsState()
            val tafsirDownloadProgress by viewModel.tafsirDownloadProgress.collectAsState()
            val selectedQariId by viewModel.selectedQariId.collectAsState()
            var showTafsirDialog by remember { mutableStateOf(false) }
            var showQariDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Qari Selection
                    Text(
                        text = "ক্বারী নির্বাচন করুন (Qari)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showQariDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        val selectedQariName = com.example.util.QariData.getQariDisplayName(selectedQariId)
                        Text(text = selectedQariName, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Qari")
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    
                    
                    if (showQariDialog) {
                        val selectionItems = com.example.util.QariData.list.map { item ->
                            com.example.ui.components.SelectionItem(
                                id = item.id,
                                title = item.nameEnglish,
                                subtitle = item.nameBengali,
                                icon = androidx.compose.material.icons.Icons.Default.RecordVoiceOver
                            )
                        }
                        com.example.ui.components.SmartSelectionDialog(
                            title = "ক্বারী নির্বাচন করুন",
                            subtitle = "আপনার পছন্দের তেলাওয়াতকারী বেছে নিন",
                            headerIcon = androidx.compose.material.icons.Icons.Default.RecordVoiceOver,
                            items = selectionItems,
                            selectedId = selectedQariId,
                            onSelectItem = { qariId ->
                                viewModel.setSelectedQariId(qariId)
                                showQariDialog = false
                            },
                            onDismiss = { showQariDialog = false },
                            showSearch = true,
                            searchPlaceholder = "ক্বারী খুঁজুন..."
                        )
                    }

                    
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "তানজিল কুরআন স্ক্রিপ্ট স্টাইল",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ডিফল্টভাবে অফলাইন ও অনলাইন উভয় অবস্থায় লোকাল quran.db (Indo-Pak) ব্যবহৃত হবে। প্রয়োজনে তানজিল স্ক্রিপ্ট নির্বাচন করতে পারেন।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val scriptOptions = listOf(
                        Pair("default-indopak", "ডিফল্ট অফলাইন স্ক্রিপ্ট (Indo-Pak / Local DB)"),
                        Pair("quran-uthmani", "উসমানী স্ক্রিপ্ট (Uthmani - Online)"),
                        Pair("quran-simple", "সহজ স্ক্রিপ্ট (Simple - Online)"),
                        Pair("quran-simple-clean", "হরকত ছাড়া ক্লিন (Simple Clean - Online)"),
                        Pair("quran-simple-plain", "প্লেইন স্ক্রিপ্ট (Simple Plain - Online)")
                    )

                    scriptOptions.forEach { (styleId, styleName) ->
                        val isSelected = tanzilTextStyle == styleId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { viewModel.setTanzilTextStyle(styleId) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = styleName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setTanzilTextStyle(styleId) },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // --- DIALOGS AND BOTTOM SHEETS ---
    if (activeDialog == "prayer_times") {
        val prayerRepo = remember(context) { com.example.data.repository.PrayerTimesRepository.getInstance(context) }
        val prayerSchedule by prayerRepo.todaySchedule.collectAsState()
        val isHanafiAsr by prayerRepo.isHanafi.collectAsState()

        com.example.ui.components.PrayerTimesDetailSheet(
            schedule = prayerSchedule,
            isHanafi = isHanafiAsr,
            hijriOffset = combinedHijriOffset,
            onDistrictSelected = { prayerRepo.setDistrict(it) },
            onHanafiChanged = { prayerRepo.setHanafi(it) },
            onDismiss = {
                if (initialSubScreen != null) {
                    onNavigateBack()
                } else {
                    activeDialog = null
                }
            }
        )
    } else if (activeDialog != null) {
        MenuDetailDialog(
            type = activeDialog!!,
            viewModel = viewModel,
            onDismiss = {
                if (initialSubScreen != null) {
                    onNavigateBack()
                } else {
                    activeDialog = null
                }
            },
            onNavigateToSurah = onNavigateToSurah,
            onNavigateToPage = onNavigateToPage,
            onNavigateToJuz = onNavigateToJuz,
            onNavigateToAyah = onNavigateToAyah,
            onNavigateToMushafPage = onNavigateToMushafPage,
            initialDuaId = if (activeDialog == "dua" || activeDialog == "morning_evening_dua") initialDuaId else null
        )
    }
}

@Composable
fun MenuDetailDialog(
    type: String,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onNavigateToSurah: (Int) -> Unit = {},
    onNavigateToPage: (Int) -> Unit = {},
    onNavigateToJuz: (Int) -> Unit = {},
    onNavigateToAyah: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToMushafPage: (String, Int) -> Unit = { _, _ -> },
    initialDuaId: Int? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogHijriOffset by viewModel.hijriOffset.collectAsState()
    val dialogCombinedHijriOffset by viewModel.combinedHijriOffset.collectAsState()
    
    // Hold selected dua state for back click handling
    var selectedDuaForDuaTab by remember(initialDuaId, type) { 
        mutableStateOf<com.example.data.DuaItem?>(
            if (initialDuaId != null && initialDuaId != -1) {
                try {
                    com.example.data.DuaData.initialize(context)
                    if (type == "morning_evening_dua") {
                        com.example.data.DuaData.morningEveningDuas.find { it.id == initialDuaId }
                    } else {
                        com.example.data.DuaData.richDuas.find { it.id == initialDuaId }
                    }
                } catch (e: Exception) {
                    null
                }
            } else null
        )
    }
    
    var subjectwiseBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var subjectwiseManzilInfoAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val handleBack: () -> Unit = {
        if ((type == "dua" || type == "morning_evening_dua") && selectedDuaForDuaTab != null) {
            selectedDuaForDuaTab = null
        } else if ((type == "subjectwise" || type == "manzil") && subjectwiseBackAction != null) {
            subjectwiseBackAction?.invoke()
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = handleBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
        val bgGradient = if (isDark) {
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E2A22),
                    Color(0xFF15201A),
                    Color(0xFF0F1713)
                )
            )
        } else {
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE8F5E9),
                    Color(0xFFEEF7F0),
                    Color(0xFFDAECE0)
                )
            )
        }
        
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .let { if (type == "qibla") it.background(bgGradient) else it.background(MaterialTheme.colorScheme.background) },
            color = if (type == "qibla") Color.Transparent else MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                val title = when (type) {
                    "calendar" -> "ক্যালেন্ডার"
                    "profile" -> "আমার প্রোফাইল"
                    "bookmark" -> "বুকমার্ক তালিকা"
                    "note" -> "আমার নোটপ্যাড"
                    "planner" -> "কুরআন প্ল্যানার"
                    "subjectwise" -> "বিষয়ভিত্তিক কুরআন"
                    "manzil" -> "মানযিল"
                    "dua" -> "কুরআনিক দুআ"
                    "morning_evening_dua" -> "সকাল সন্ধ্যার দুআ"
                    "qibla" -> "কিবলা কম্পাস"
                    "game" -> "ওয়ার্ড গেম"
                    "player" -> "কুরআন অডিও প্লেয়ার"
                    "hifz" -> "হিফজ ট্র্যাকার"
                    "learn" -> "কুরআন শিক্ষা"
                    "video" -> "ভিডিও এডিটর"
                    "offline_sync" -> "কুরআন অফলাইন ডাউনলোড"
                    "font_settings" -> "ফন্ট ও তাজভীদ"
                    "backup" -> "ব্যাকআপ"
                    "notifications" -> "নোটিফিকেশন সেটিংস"
                    "theme" -> "অ্যাপ থিম"
                    "about" -> "আমাদের সম্পর্কে ও প্রাইভেসি"
                    "contact" -> "যোগাযোগ"
                    else -> "বিস্তারিত"
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (type == "manzil") {
                        IconButton(onClick = { subjectwiseManzilInfoAction?.invoke() }) {
                            Icon(Icons.Default.Info, contentDescription = "মানযিল পরিচিতি", tint = PrimaryGreen)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp)) // Symmetrical spacer
                    }
                }
                
                if (type != "qibla") {
                    HorizontalDivider(color = Border)
                }
                
                // Dialog Content Body
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(if (type == "qibla") 0.dp else 16.dp)
                ) {
                    when (type) {
                        "calendar" -> com.example.ui.components.IslamicCalendarView(
                            hijriOffset = dialogCombinedHijriOffset,
                            onHijriOffsetChange = { newCombined ->
                                val diff = newCombined - dialogCombinedHijriOffset
                                viewModel.setHijriOffset(dialogHijriOffset + diff)
                            }
                        )
                        "profile" -> ProfileDialogContent(viewModel)
                        "bookmark" -> BookmarkDialogContent(
                            viewModel = viewModel,
                            onBookmarkClick = { bookmark ->
                                onDismiss()
                                when (bookmark.type) {
                                    "SURAH" -> onNavigateToSurah(bookmark.referenceId)
                                    "MUSHAF_PAGE" -> {
                                        val targetMushaf = bookmark.mushafId ?: "imdadia_hafezi"
                                        onNavigateToMushafPage(targetMushaf, bookmark.referenceId)
                                    }
                                    "PAGE" -> onNavigateToPage(bookmark.referenceId)
                                    "JUZ" -> onNavigateToJuz(bookmark.referenceId)
                                    "AYAH" -> {
                                        val (surahNum, ayahNum) = com.example.data.QuranData.getSurahAndAyahFromGlobal(bookmark.referenceId)
                                        onNavigateToAyah(surahNum, ayahNum)
                                    }
                                }
                            }
                        )
                        "note" -> NotepadDialogContent(viewModel)
                        "planner" -> PlannerDialogContent(viewModel)
                        "subjectwise" -> SubjectwiseDialogContent(
                            viewModel = viewModel,
                            onDismiss = onDismiss,
                            onRegisterBackAction = { subjectwiseBackAction = it },
                            onRegisterManzilInfoAction = { subjectwiseManzilInfoAction = it }
                        )
                        "manzil" -> SubjectwiseDialogContent(
                            viewModel = viewModel,
                            initialCategoryName = "মানযিল",
                            onDismiss = onDismiss,
                            onRegisterBackAction = { subjectwiseBackAction = it },
                            onRegisterManzilInfoAction = { subjectwiseManzilInfoAction = it }
                        )
                        "dua" -> DuaDialogContent(
                            viewModel = viewModel,
                            selectedDua = selectedDuaForDuaTab,
                            onSelectedDuaChange = { selectedDuaForDuaTab = it },
                            isMorningEvening = false
                        )
                        "qibla" -> QiblaDialogContent()
                        "morning_evening_dua" -> DuaDialogContent(
                            viewModel = viewModel,
                            selectedDua = selectedDuaForDuaTab,
                            onSelectedDuaChange = { selectedDuaForDuaTab = it },
                            isMorningEvening = true
                        )
                        "game" -> GameDialogContent(viewModel)
                        "player" -> PlayerDialogContent()
                        "hifz" -> HifzDialogContent(viewModel)
                        "learn" -> LearnDialogContent()
                        "video" -> QuranVideoCreatorScreen(
                            onNavigateBack = { onDismiss() }
                        )
                        "offline_sync" -> OfflineSyncDialogContent(viewModel)
                        "font_settings" -> FontSettingsContent(viewModel = viewModel, onDismiss = onDismiss)
                        "backup" -> BackupDialogContent()
                        "notifications" -> NotificationDialogContent(viewModel)
                        "theme" -> ThemeDialogContent(viewModel)
                        "about" -> AboutDialogContent()
                        "contact" -> ContactDialogContent()
                    }
                }
            }
        }
    }
}

// --- 1. PROFILE DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialogContent(viewModel: SettingsViewModel) {
    val username by viewModel.username.collectAsState()
    val readingMins by viewModel.readingTimeMinutes.collectAsState()
    var tempName by remember { mutableStateOf(username) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(PrimaryGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "প্রোফাইল পরিবর্তন করুন",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = tempName,
            onValueChange = { tempName = it },
            label = { Text("ব্যবহারকারীর নাম") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                focusedLabelColor = PrimaryGreen
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { viewModel.updateUsername(tempName) },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("নাম পরিবর্তন করুন", color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Border)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "পড়ার সময় বৃদ্ধি করুন (সিমুলেটর)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.addReadingTime(15) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                modifier = Modifier.weight(1f)
            ) {
                Text("+১৫ মিনিট", color = Color.White, fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.addReadingTime(30) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.weight(1f)
            ) {
                Text("+৩০ মিনিট", color = Color.White, fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.addReadingTime(60) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                modifier = Modifier.weight(1f)
            ) {
                Text("+১ ঘণ্টা", color = Color.White, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("মোট অধ্যয়নকাল", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (readingMins >= 60) "${readingMins / 60} ঘণ্টা ${readingMins % 60} মিনিট" else "$readingMins মিনিট",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        }
    }
}

// --- 2. BOOKMARK DIALOG ---
@Composable
fun BookmarkDialogContent(
    viewModel: SettingsViewModel,
    onBookmarkClick: (BookmarkEntity) -> Unit = {}
) {
    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    
    if (bookmarks.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = GrayText.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "কোনো বুকমার্ক পাওয়া যায়নি!",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "সুরা বা পৃষ্ঠা পড়ার সময় উপরে বুকমার্ক বাটনে ক্লিক করুন।",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bookmarks) { bookmark ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookmarkClick(bookmark) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bookmark.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val displayType = when (bookmark.type) {
                                "SURAH" -> "সুরা"
                                "PAGE" -> "পৃষ্ঠা (হাফেজী)"
                                "MUSHAF_PAGE" -> "মুসহাফ পৃষ্ঠা"
                                "JUZ" -> "পারা"
                                "AYAH" -> "আয়াত"
                                else -> bookmark.type
                            }
                            Text(
                                text = "প্রকার: $displayType • আইডি: ${bookmark.referenceId}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.removeBookmark(bookmark) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

// --- 3. NOTEPAD DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadDialogContent(viewModel: SettingsViewModel) {
    val notes by viewModel.notes.collectAsState()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Add Note Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("নতুন নোট লিখুন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("নোটের শিরোনাম") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("এখানে বিস্তারিত লিখুন...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            viewModel.addNote(title, content)
                            title = ""
                            content = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নোট যুক্ত করুন", color = Color.White)
                }
            }
        }
        
        // Notes List
        Text(
            text = "নোটের তালিকা (${notes.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো নোট পাওয়া যায়নি!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(note.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                IconButton(onClick = { viewModel.deleteNote(note.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(note.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(note.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 4. PLANNER DIALOG ---
@Composable
fun PlannerDialogContent(viewModel: SettingsViewModel) {
    val target by viewModel.plannerTarget.collectAsState()
    val pagesRead by viewModel.plannerPagesRead.collectAsState()
    val startDate by viewModel.plannerStartDate.collectAsState()
    val streak by viewModel.plannerStreak.collectAsState()
    val reminderEnabled by viewModel.plannerReminderEnabled.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.togglePlannerReminder(true)
            }
        }
    )

    val khatamPlans = listOf(
        Pair("৩০ দিনে খতম", 30),
        Pair("৬০ দিনে খতম", 60),
        Pair("৯০ দিনে খতম", 90),
        Pair("৬ মাসে খতম", 180),
        Pair("১ বছরে খতম", 365)
    )
    
    val selectedPlan = khatamPlans.find { it.first == target } ?: khatamPlans.first()
    val totalDays = selectedPlan.second
    val passedDays = maxOf(0, ((System.currentTimeMillis() - startDate) / (1000 * 60 * 60 * 24)).toInt())
    val remainingDays = maxOf(1, totalDays - passedDays)
    val remainingPages = maxOf(0, 610 - pagesRead)
    
    // Dynamic daily target adjustment
    val dailyTargetPages = maxOf(1, kotlin.math.ceil(remainingPages.toDouble() / remainingDays).toInt())
    
    val progressPercentage = pagesRead.toFloat() / 610f

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        
        // --- 1. Dynamic Progress & Streak Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Circular Progress
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(72.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                            strokeWidth = 6.dp,
                        )
                        CircularProgressIndicator(
                            progress = { progressPercentage },
                            modifier = Modifier.size(72.dp),
                            color = PrimaryGreen,
                            strokeWidth = 6.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progressPercentage * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(selectedPlan.first, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("মোট পড়া: $pagesRead / ৬০৪ পৃষ্ঠা", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("টানা পড়া: $streak দিন (Streak)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- 2. Dynamic Daily Target ---
        Text("আজকের লক্ষ্য", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$dailyTargetPages", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("পৃষ্ঠা পড়তে হবে", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "বাকি দিন: $remainingDays | বাকি পৃষ্ঠা: $remainingPages",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.addPlannerPages(dailyTargetPages) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সম্পন্ন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- 3. Milestones & Badges ---
        Text("মাইলফলক ও ব্যাজ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        
        val totalJuz = 30
        val completedJuz = pagesRead / 20 // Approx 20 pages per juz
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(10) { i ->
                val juzTarget = (i + 1) * 3
                val isUnlocked = completedJuz >= juzTarget
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isUnlocked) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${juzTarget} পারা", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // --- 4. Select Plan ---
        Text("লক্ষ্য পরিবর্তন করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(khatamPlans) { plan ->
                val isSel = target == plan.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) PrimaryGreen else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (isSel) PrimaryGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.updatePlannerTarget(plan.first) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(plan.first, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- 5. Smart Reminder ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("স্মার্ট রিমাইন্ডার", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("পড়ার সময় মনে করিয়ে দিতে নোটিফিকেশন", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = reminderEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.togglePlannerReminder(true)
                        }
                    } else {
                        viewModel.togglePlannerReminder(false)
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
            )
        }

        if (reminderEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            val hour by viewModel.plannerReminderHour.collectAsState()
            val minute by viewModel.plannerReminderMinute.collectAsState()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "রিমাইন্ডারের সময় নির্ধারণ করুন:",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val amPm = if (hour >= 12) "PM" else "AM"
                        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                        val formattedDisplayTime = String.format("%02d:%02d %s", displayHour, minute, amPm)
                        
                        Text(
                            text = "বর্তমান সময়: $formattedDisplayTime",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hour controls
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ঘণ্টা", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        val newHour = if (hour == 0) 23 else hour - 1
                                        viewModel.updatePlannerReminderTime(newHour, minute)
                                    },
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease hour", modifier = Modifier.size(16.dp))
                                }
                                
                                Text(
                                    text = String.format("%02d", hour),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.widthIn(min = 24.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                IconButton(
                                    onClick = { 
                                        val newHour = if (hour == 23) 0 else hour + 1
                                        viewModel.updatePlannerReminderTime(newHour, minute)
                                    },
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase hour", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        
                        // Minute controls
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("মিনিট", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        val newMin = if (minute == 0) 55 else ((minute - 5) / 5) * 5
                                        viewModel.updatePlannerReminderTime(hour, newMin)
                                    },
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease minute", modifier = Modifier.size(16.dp))
                                }
                                
                                Text(
                                    text = String.format("%02d", minute),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.widthIn(min = 24.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                IconButton(
                                    onClick = { 
                                        val newMin = if (minute >= 55) 0 else ((minute + 5) / 5) * 5
                                        viewModel.updatePlannerReminderTime(hour, newMin)
                                    },
                                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase minute", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// --- 5. SUBJECTWISE DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectwiseDialogContent(
    viewModel: SettingsViewModel? = null,
    initialCategoryName: String? = null,
    onDismiss: () -> Unit = {},
    onRegisterBackAction: (((() -> Unit)?) -> Unit) = {},
    onRegisterManzilInfoAction: (((() -> Unit)?) -> Unit) = {}
) {
    val context = LocalContext.current
    val arabicFontName = viewModel?.arabicFontName?.collectAsState()?.value ?: "Me Quran"
    val arabicFont = com.example.ui.theme.getArabicFont(arabicFontName)
    val subjectCategoryListState = rememberLazyListState()
    val subjectTopicListState = rememberLazyListState()
    val subjectVerseListState = rememberLazyListState()
    var selectedCategory by remember { mutableStateOf<com.example.data.SubjectwiseCategory?>(null) }
    var selectedTopic by remember { mutableStateOf<com.example.data.SubjectwiseTopic?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var allCategories by remember { mutableStateOf<List<com.example.data.SubjectwiseCategory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showTranslation by remember { mutableStateOf(true) }
    var showManzilInfo by remember { mutableStateOf(false) }

    val handleInternalBack = {
        if (showManzilInfo) {
            showManzilInfo = false
        } else if (selectedTopic != null && initialCategoryName == null) {
            selectedTopic = null
        } else if (selectedCategory != null && initialCategoryName == null) {
            selectedCategory = null
        } else {
            onDismiss()
        }
    }

    DisposableEffect(selectedCategory, selectedTopic, initialCategoryName, showManzilInfo) {
        onRegisterBackAction(handleInternalBack)
        onRegisterManzilInfoAction { showManzilInfo = true }
        onDispose {
            onRegisterBackAction(null)
            onRegisterManzilInfoAction(null)
        }
    }

    androidx.activity.compose.BackHandler {
        handleInternalBack()
    }

    LaunchedEffect(initialCategoryName) {
        val loaded = com.example.data.SubjectwiseQuranRepository.getCategories(context)
        allCategories = loaded
        isLoading = false
        if (!initialCategoryName.isNullOrEmpty() && selectedCategory == null) {
            val matchedCategory = loaded.find { it.categoryNameBn == "মানযিল" }
            if (matchedCategory != null) {
                selectedCategory = matchedCategory
                if (matchedCategory.topics.isNotEmpty()) {
                    selectedTopic = matchedCategory.topics[0]
                }
            }
        }
    }

    if (showManzilInfo) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showManzilInfo = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
        ) {
            ManzilInfoContent()
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PrimaryGreen)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "অফলাইন ডাটাবেজ থেকে বিষয়ভিত্তিক আয়াত লোড হচ্ছে...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (selectedCategory == null) {
        // --- PAGE 1: CATEGORIES LIST (বিষয়ভিত্তিক বিভাগসমূহ) ---
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("বিষয়, সূরা বা আয়াত অনুসন্ধান করুন...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGreen) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            val baseCategories = remember(allCategories, initialCategoryName) {
                if (initialCategoryName == null) {
                    allCategories.filter { it.categoryNameBn != "মানযিল" }
                } else {
                    allCategories
                }
            }

            val filteredCategories = remember(searchQuery, baseCategories) {
                val query = searchQuery.trim().lowercase()
                if (query.isEmpty()) baseCategories
                else {
                    baseCategories.filter { cat ->
                        cat.categoryNameBn.lowercase().contains(query) ||
                        cat.topics.any { topic ->
                            topic.titleBn.lowercase().contains(query) ||
                            topic.verses.any { v ->
                                v.surahName.lowercase().contains(query) ||
                                v.banglaTranslation.lowercase().contains(query)
                            }
                        }
                    }
                }
            }

            if (filteredCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "কোনো বিষয় পাওয়া যায়নি",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    state = subjectCategoryListState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredCategories, key = { _, cat -> cat.categoryId }) { index, category ->
                        Card(
                            onClick = { selectedCategory = category },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = PrimaryGreen.copy(alpha = 0.12f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = category.categoryNameBn,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = PrimaryGreen.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "${category.topics.size}টি বিষয়",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    val totalAyahs = category.topics.sumOf { it.verses.size }
                                    Text(
                                        text = "মোট $totalAyahs টি কুরআনের আয়াত সংকলিত",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else if (selectedTopic == null) {
        // --- PAGE 2: TOPICS LIST IN SELECTED CATEGORY (ক্যাটাগরির অন্তর্ভুক্ত বিষয়সমূহ) ---
        val category = selectedCategory!!

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.categoryNameBn,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "মোট ${category.topics.size}টি বিষয়ের তালিকা",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                state = subjectTopicListState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(category.topics, key = { _, topic -> topic.topicId }) { index, topic ->
                    Card(
                        onClick = { selectedTopic = topic },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PrimaryGreen.copy(alpha = 0.12f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topic.titleBn,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${topic.verses.size}টি সম্পর্কিত আয়াত",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // --- PAGE 3: VERSE CARDS FOR SELECTED TOPIC (বিষয়ের সমস্ত আয়াত) ---
        val category = selectedCategory!!
        val topic = selectedTopic!!
        val isManzilMode = initialCategoryName != null || category.categoryNameBn == "মানযিল"

        Column(modifier = Modifier.fillMaxSize()) {
            // Header / Sub-controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isManzilMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = topic.titleBn,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${category.categoryNameBn} • ${topic.verses.size}টি কার্ড (অফলাইন কুরআন থেকে)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "মানযিল • ${topic.verses.size}টি কার্ড (অফলাইন কুরআন থেকে)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Translation ON/OFF Toggle
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (showTranslation) PrimaryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showTranslation = !showTranslation }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (showTranslation) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (showTranslation) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showTranslation) "অনুবাদ ON" else "অনুবাদ OFF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showTranslation) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                state = subjectVerseListState,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(topic.verses, key = { _, v -> v.id }) { index, verse ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = PrimaryGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (verse.verseNo.contains("-") || verse.verseNo.contains(",")) "আয়াত ${verse.verseNo}" else "আয়াত #${verse.verseNo}",
                                        color = PrimaryGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = "${verse.surahName} • আয়াত ${verse.verseNo}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Arabic Text from Offline quran.db
                            if (verse.arabicText.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    val annotatedArabicText = androidx.compose.ui.text.buildAnnotatedString {
                                        val regex = Regex("﴿.*?﴾")
                                        var lastIndex = 0
                                        regex.findAll(verse.arabicText).forEach { matchResult ->
                                            val textPart = verse.arabicText.substring(lastIndex, matchResult.range.first)
                                            appendStyledWaqfText(textPart, 22f, true, arabicFontName)
                                            
                                            withStyle(
                                                androidx.compose.ui.text.SpanStyle(
                                                    fontFamily = arabicFont,
                                                    fontSize = 22.sp
                                                )
                                            ) {
                                                append(matchResult.value)
                                            }
                                            lastIndex = matchResult.range.last + 1
                                        }
                                        val remainingText = verse.arabicText.substring(lastIndex)
                                        appendStyledWaqfText(remainingText, 22f, true, arabicFontName)
                                    }
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = annotatedArabicText,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 38.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = arabicFont,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                        )
                                    }
                                }
                            }

                            // Bangla Translation from Offline quran.db
                            if (showTranslation && verse.banglaTranslation.isNotEmpty()) {
                                Text(
                                    text = "বাংলা অনুবাদ:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = verse.banglaTranslation,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Action Buttons: Copy, Share (Text & Image)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Copy Button
                                OutlinedButton(
                                    onClick = {
                                        com.example.utils.SubjectwiseShareUtil.copyToClipboard(
                                            context,
                                            verse,
                                            topic.titleBn
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "কপি",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("কপি", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Text Share Button
                                    FilledTonalButton(
                                        onClick = {
                                            com.example.utils.SubjectwiseShareUtil.shareAsText(
                                                context,
                                                verse,
                                                topic.titleBn
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = PrimaryGreen.copy(alpha = 0.12f),
                                            contentColor = PrimaryGreen
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "টেক্সট শেয়ার",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("টেক্সট", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }

                                    // Image Share Button
                                    Button(
                                        onClick = {
                                            com.example.utils.SubjectwiseShareUtil.shareAsImage(
                                                context,
                                                verse,
                                                topic.titleBn
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryGreen,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "ছবি শেয়ার",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ছবি", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

// --- 6. DUA DIALOG ---
@Composable
fun DuaDialogContent(
    viewModel: SettingsViewModel,
    selectedDua: com.example.data.DuaItem?,
    onSelectedDuaChange: (com.example.data.DuaItem?) -> Unit,
    isMorningEvening: Boolean = false
) {
    val arabicFontName by viewModel.arabicFontName.collectAsState()
    val arabicFont = com.example.ui.theme.getArabicFont(arabicFontName)
    val allDuas = if (isMorningEvening) com.example.data.DuaData.morningEveningDuas else com.example.data.DuaData.richDuas
    val duaListState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    
    fun formatToBanglaNumber(num: Int): String {
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return num.toString().map { char ->
            if (char.isDigit()) banglaDigits[char - '0'] else char
        }.joinToString("")
    }

    val filteredDuas = remember(searchQuery, allDuas) {
        if (searchQuery.isBlank()) {
            allDuas
        } else {
            allDuas.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.segments.any { segment -> 
                    segment.translation.contains(searchQuery, ignoreCase = true) ||
                    segment.transliteration.contains(searchQuery, ignoreCase = true) ||
                    segment.arabic.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    val isDetailMode = selectedDua != null
    Crossfade(targetState = isDetailMode, label = "DuaNavigation") { showDetail ->
        if (!showDetail) {
            // DUAS INDEX / SUCI LIST
            Column(modifier = Modifier.fillMaxSize()) {
                // Beautiful Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("দুআ খুঁজুন...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimaryGreen
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        cursorColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (filteredDuas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "কোনো দুআ পাওয়া যায়নি",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = duaListState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(filteredDuas, key = { it.id }) { dua ->
                            val index = allDuas.indexOf(dua) + 1
                            val banglaIndex = formatToBanglaNumber(index)
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectedDuaChange(dua) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Index Circle Icon - warm coral-red color matching the screenshot
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                color = Color(0xFFE55353),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = banglaIndex,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(14.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dua.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 20.sp
                                        )
                                        if (dua.segments.isNotEmpty()) {
                                            val firstTranslation = dua.segments.first().translation
                                            if (firstTranslation.isNotEmpty()) {
                                                Text(
                                                    text = firstTranslation,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                    thickness = 0.8.dp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // DUA DETAILS PAGE WITH HORIZONTAL SWIPE
            val initialPageIndex = remember {
                if (selectedDua != null) {
                    val idx = filteredDuas.indexOf(selectedDua)
                    if (idx >= 0) idx else 0
                } else 0
            }

            val pagerState = rememberPagerState(
                initialPage = initialPageIndex,
                pageCount = { filteredDuas.size }
            )

            LaunchedEffect(selectedDua) {
                if (selectedDua != null) {
                    val idx = filteredDuas.indexOf(selectedDua)
                    if (idx in 0 until filteredDuas.size && pagerState.currentPage != idx) {
                        pagerState.scrollToPage(idx)
                    }
                }
            }

            LaunchedEffect(pagerState) {
                androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
                    val currentDuaFromPager = filteredDuas.getOrNull(page)
                    if (currentDuaFromPager != null && currentDuaFromPager != selectedDua) {
                        onSelectedDuaChange(currentDuaFromPager)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val currentDua = filteredDuas.getOrNull(pageIndex)
                if (currentDua != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Main Details Area (Custom elegant container)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            val formattedIndex = formatToBanglaNumber(allDuas.indexOf(currentDua) + 1)
                            
                            // Title styled centered or structured beautifully like the screenshot
                            Text(
                                text = "[$formattedIndex] ${currentDua.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 26.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            )
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(bottom = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            
                            currentDua.segments.forEachIndexed { index, segment ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                                
                                // Arabic Text - large, RTL aligned display
                                if (segment.arabic.isNotEmpty() && segment.arabic != "null") {
                                    val cleanForBismillah = segment.arabic.replace(Regex("[\\s\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
                                    val isBismillah = cleanForBismillah == "بسماللهالرحمنالرحيم" || cleanForBismillah == "بسمٱللهٱلرحمنٱلرحيم"
                                    
                                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                                        Text(
                                            text = segment.arabic,
                                            fontSize = 26.sp,
                                            fontFamily = arabicFont,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            lineHeight = 44.sp
                                        )
                                    }
                                }
                                
                                // Translation with Left-Border Accent Bar exactly like the screenshot
                                if (segment.translation.isNotEmpty() && segment.translation != "null") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(Color(0xFF00B4D8), RoundedCornerShape(2.dp)) // Cyan/teal left border accent
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = segment.translation,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                                
                                // Transliteration with Left-Border Accent Bar exactly like the screenshot
                                if (segment.transliteration.isNotEmpty() && segment.transliteration != "null") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(Color(0xFF00B4D8).copy(alpha = 0.6f), RoundedCornerShape(2.dp)) // Accent left border
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = segment.transliteration,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                                
                                // Prekkhapot (Dua's context) exactly like the screenshot:
                                // "দোয়ার প্রেক্ষাপট: এটি দুনিয়া-আখিরাত উভয় জগতে সফলতার জন্য..."
                                if (segment.bottom.isNotEmpty() && segment.bottom != "null") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        val trimmed = segment.bottom.trim()
                                        val contextText = if (trimmed.startsWith("দোয়ার প্রেক্ষাপট") || trimmed.startsWith("দোয়ার প্রেক্ষাপট")) {
                                            trimmed
                                        } else {
                                            "দোয়ার প্রেক্ষাপট: ${segment.bottom}"
                                        }
                                        Text(
                                            text = contextText,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                                
                                // Reference - small, styled at the bottom
                                if (segment.reference.isNotEmpty() && segment.reference != "null") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = segment.reference,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                            
                            // Copy & Share Actions Row with App Credit
                            DuaActionButtonsRow(dua = currentDua)
                        }
                    }
                }
            }
        }
    }
}

private @Composable
fun DuaActionButtonsRow(
    dua: com.example.data.DuaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showShareMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Copy Button
            OutlinedButton(
                onClick = { com.example.utils.DuaShareUtil.copyToClipboard(context, dua) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "কপি",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // 2. Share Button (With Dropdown Menu)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedButton(
                    onClick = { showShareMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "শেয়ার",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Dropdown Menu for Image & Text Share Options
                androidx.compose.material3.DropdownMenu(
                    expanded = showShareMenu,
                    onDismissRequest = { showShareMenu = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Text Share",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "টেক্সট শেয়ার",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            showShareMenu = false
                            com.example.utils.DuaShareUtil.shareAsText(context, dua)
                        }
                    )
                    
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Image Share",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "ছবি শেয়ার",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            showShareMenu = false
                            com.example.utils.DuaShareUtil.shareAsImage(context, dua)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // App Credit with Logo & Name: (logo) ❝কুরআন রিডার❞ অ্যাপ
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_launcher),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "❝কুরআন রিডার❞ অ্যাপ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// --- 7. WORD GAME DIALOG ---
@Composable
fun GameDialogContent(viewModel: SettingsViewModel) {
    val phase by viewModel.gamePhase.collectAsState()
    
    when (phase) {
        com.example.ui.viewmodels.GamePhase.SETUP -> GameSetupScreen(viewModel)
        com.example.ui.viewmodels.GamePhase.LOADING -> GameLoadingScreen()
        com.example.ui.viewmodels.GamePhase.PLAYING -> GamePlayingScreen(viewModel)
        com.example.ui.viewmodels.GamePhase.RESULT -> GameResultScreen(viewModel)
    }
}

@Composable
fun GameSetupScreen(viewModel: SettingsViewModel) {
    val config by viewModel.gameConfig.collectAsState()
    val errorMessage by viewModel.gameErrorMessage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val surahs = com.example.data.surahInfoList
    var isSurahDropdownExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    androidx.compose.runtime.LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearGameErrorMessage()
        }
    }
    
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val inactiveBg = MaterialTheme.colorScheme.surface
    val inactiveBorder = if (isDark) Color(0xFF2A3E39) else Border
    val textInactive = MaterialTheme.colorScheme.onSurface
    
    // Orange/Amber selections
    val amberActiveBg = if (isDark) Color(0xFF452B09) else Color(0xFFFDE6B0)
    val amberActiveBorder = if (isDark) Color(0xFFFBBF24) else PrimaryGreen
    val amberActiveText = if (isDark) Color(0xFFFBBF24) else PrimaryGreen
    
    // Teal/Mint selections
    val tealActiveBg = if (isDark) Color(0xFF0A3631) else Color(0xFFD1FAF5)
    val tealActiveBorder = if (isDark) Color(0xFF34D399) else PrimaryGreen
    val tealActiveText = if (isDark) Color(0xFF34D399) else PrimaryGreen
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text("১. কিসের উপর গেম খেলতে চান?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val isEntireSelected = config.source == com.example.ui.viewmodels.GameSource.ENTIRE_QURAN
            Card(
                modifier = Modifier.weight(1f).clickable { viewModel.updateGameConfig(config.copy(source = com.example.ui.viewmodels.GameSource.ENTIRE_QURAN)) },
                colors = CardDefaults.cardColors(containerColor = if (isEntireSelected) amberActiveBg else inactiveBg),
                border = BorderStroke(1.dp, if (isEntireSelected) amberActiveBorder else inactiveBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(androidx.compose.material.icons.Icons.Default.MenuBook, contentDescription = null, tint = if (isEntireSelected) amberActiveText else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("সম্পূর্ণ কুরআন", fontSize = 13.sp, fontWeight = if (isEntireSelected) FontWeight.Bold else FontWeight.Medium, color = if (isEntireSelected) amberActiveText else textInactive)
                }
            }
            val isSpecificSelected = config.source == com.example.ui.viewmodels.GameSource.SPECIFIC_SURAH
            Card(
                modifier = Modifier.weight(1f).clickable { viewModel.updateGameConfig(config.copy(source = com.example.ui.viewmodels.GameSource.SPECIFIC_SURAH)) },
                colors = CardDefaults.cardColors(containerColor = if (isSpecificSelected) amberActiveBg else inactiveBg),
                border = BorderStroke(1.dp, if (isSpecificSelected) amberActiveBorder else inactiveBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(androidx.compose.material.icons.Icons.Default.Description, contentDescription = null, tint = if (isSpecificSelected) amberActiveText else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("নির্দিষ্ট সূরা", fontSize = 13.sp, fontWeight = if (isSpecificSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSpecificSelected) amberActiveText else textInactive)
                }
            }
        }
        
        if (config.source == com.example.ui.viewmodels.GameSource.SPECIFIC_SURAH) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { isSurahDropdownExpanded = true },
                colors = CardDefaults.cardColors(containerColor = inactiveBg),
                border = BorderStroke(1.dp, inactiveBorder)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val selectedName = surahs.find { it.first == config.selectedSurah }?.second?.arabicName ?: "সূরা নির্বাচন করুন"
                    Text("নির্বাচিত সূরা: $selectedName", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            androidx.compose.material3.DropdownMenu(
                expanded = isSurahDropdownExpanded,
                onDismissRequest = { isSurahDropdownExpanded = false }
            ) {
                surahs.forEach { surahInfo ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("${surahInfo.first}. ${surahInfo.second.arabicName}") },
                        onClick = { 
                            viewModel.updateGameConfig(config.copy(selectedSurah = surahInfo.first))
                            isSurahDropdownExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("২. গেমের ধরণ নির্ধারণ করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val isArToBn = config.type == com.example.ui.viewmodels.GameType.ARABIC_TO_BENGALI
            Card(
                modifier = Modifier.weight(1f).clickable { viewModel.updateGameConfig(config.copy(type = com.example.ui.viewmodels.GameType.ARABIC_TO_BENGALI)) },
                colors = CardDefaults.cardColors(containerColor = if (isArToBn) tealActiveBg else inactiveBg),
                border = BorderStroke(1.dp, if (isArToBn) tealActiveBorder else inactiveBorder)
            ) {
                Text("আরবি -> বাংলা", modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = if (isArToBn) FontWeight.Bold else FontWeight.Medium, color = if (isArToBn) tealActiveText else textInactive)
            }
            val isBnToAr = config.type == com.example.ui.viewmodels.GameType.BENGALI_TO_ARABIC
            Card(
                modifier = Modifier.weight(1f).clickable { viewModel.updateGameConfig(config.copy(type = com.example.ui.viewmodels.GameType.BENGALI_TO_ARABIC)) },
                colors = CardDefaults.cardColors(containerColor = if (isBnToAr) tealActiveBg else inactiveBg),
                border = BorderStroke(1.dp, if (isBnToAr) tealActiveBorder else inactiveBorder)
            ) {
                Text("বাংলা -> আরবি", modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = if (isBnToAr) FontWeight.Bold else FontWeight.Medium, color = if (isBnToAr) tealActiveText else textInactive)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("৩. মোট কতটি প্রশ্ন?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 30).forEach { count ->
                val isCountSelected = config.totalQuestions == count
                Card(
                    modifier = Modifier.weight(1f).clickable { viewModel.updateGameConfig(config.copy(totalQuestions = count)) },
                    colors = CardDefaults.cardColors(containerColor = if (isCountSelected) amberActiveBg else inactiveBg),
                    border = BorderStroke(1.dp, if (isCountSelected) amberActiveBorder else inactiveBorder)
                ) {
                    Text("$count টি", modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = if (isCountSelected) FontWeight.Bold else FontWeight.Medium, color = if (isCountSelected) amberActiveText else textInactive)
                }
            }
        }
        

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.startDynamicGame() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)) // Orange like in image
        ) {
            Text("গেম শুরু করুন", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(androidx.compose.material.icons.Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun GameLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryGreen)
            Spacer(modifier = Modifier.height(16.dp))
            Text("কুরআন থেকে শব্দ সংগ্রহ করা হচ্ছে...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GamePlayingScreen(viewModel: SettingsViewModel) {
    val score by viewModel.gameScore.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val lastCorrect by viewModel.lastAnswerCorrect.collectAsState()
    val selectedAnswer by viewModel.selectedAnswer.collectAsState()
    val questions by viewModel.dynamicQuestions.collectAsState()
    val config by viewModel.gameConfig.collectAsState()
    val arabicFontName by viewModel.arabicFontName.collectAsState()
    val arabicFont = com.example.ui.theme.getArabicFont(arabicFontName)
    val bengaliFont = com.example.ui.theme.LocalBengaliFont.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    val vibrator = androidx.compose.runtime.remember {
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }
    if (questions.isEmpty()) return
    
    val question = questions[currentIndex]
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("স্কোর: $score", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryGreen)
            Text("প্রশ্ন: ${currentIndex + 1}/${questions.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Question Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("নিচের শব্দটির সঠিক অর্থ নির্বাচন করুন:", fontFamily = bengaliFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = question.question,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (config.type == com.example.ui.viewmodels.GameType.ARABIC_TO_BENGALI) 32.sp else 24.sp,
                    fontFamily = if (config.type == com.example.ui.viewmodels.GameType.ARABIC_TO_BENGALI) arabicFont else bengaliFont,
                    lineHeight = if (config.type == com.example.ui.viewmodels.GameType.ARABIC_TO_BENGALI) 48.sp else 32.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Options List
        question.options.forEach { opt ->
            val isCorrectOpt = opt == question.correctAnswer
            val isAnswered = lastCorrect != null // once answered, show correct/wrong
            val isThisSelectedOption = selectedAnswer == opt
            
            val borderCol = when {
                isAnswered && isCorrectOpt -> if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
                isAnswered && isThisSelectedOption && !isCorrectOpt -> if (isDark) Color(0xFFF87171) else Color.Red
                else -> if (isDark) Color(0xFF2A3E39) else Border
            }
            val bgCol = when {
                isAnswered && isCorrectOpt -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAF5)
                isAnswered && isThisSelectedOption && !isCorrectOpt -> if (isDark) Color(0xFF450A0A) else Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surface
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable(enabled = lastCorrect == null) {
                        viewModel.submitAnswer(opt)
                        if (opt != question.correctAnswer) {
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(200)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = bgCol),
                border = BorderStroke(1.dp, borderCol)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = opt,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (config.type == com.example.ui.viewmodels.GameType.BENGALI_TO_ARABIC) 22.sp else 16.sp,
                        fontFamily = if (config.type == com.example.ui.viewmodels.GameType.BENGALI_TO_ARABIC) arabicFont else bengaliFont,
                        lineHeight = if (config.type == com.example.ui.viewmodels.GameType.BENGALI_TO_ARABIC) 36.sp else 24.sp,
                        color = if (isAnswered && isThisSelectedOption && !isCorrectOpt) {
                            if (isDark) Color(0xFFF87171) else Color.Red
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (isAnswered && isCorrectOpt) {
                        Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = null, tint = if (isDark) Color(0xFF34D399) else PrimaryGreen)
                    } else if (isAnswered && isThisSelectedOption && !isCorrectOpt) {
                        Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = null, tint = if (isDark) Color(0xFFF87171) else Color.Red)
                    }
                }
            }
        }
        
        if (lastCorrect != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.nextQuestion() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text(if (currentIndex == questions.size - 1) "ফলাফল দেখুন" else "পরবর্তী প্রশ্ন", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

fun shareBitmap(context: android.content.Context, bitmap: android.graphics.Bitmap) {
    try {
        val cacheDir = java.io.File(context.cacheDir, "shared_images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val file = java.io.File(cacheDir, "game_result_${System.currentTimeMillis()}.png")
        val out = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "ফলাফল কার্ড শেয়ার করুন"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "শেয়ার করতে সমস্যা হয়েছে: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}


@Composable
fun GameResultScreen(viewModel: SettingsViewModel) {
    val score by viewModel.gameScore.collectAsState()
    val total = viewModel.dynamicQuestions.value.size
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    
    val comment = when {
        score == total -> "মাশাআল্লাহ! অসাধারণ!"
        score >= total * 0.8 -> "আলহামদুলিল্লাহ! খুব ভালো!"
        score >= total * 0.5 -> "ভালো চেষ্টা, আরো চর্চা করুন!"
        else -> "ইনশাআল্লাহ! পরবর্তীতে আরো ভালো হবে।"
    }
    
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    val outerCardBg = if (isDark) Color(0xFF0C241F) else Color(0xFFE8F5E9)
    val innerCardBg = if (isDark) Color(0xFF061A16) else Color(0xFFE8F5E9)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp).drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            },
            colors = CardDefaults.cardColors(containerColor = outerCardBg),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("কুরআন শব্দ গেইম", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = PrimaryGreen)
                Text("কুরআন রিডার", fontSize = 12.sp, color = PrimaryGreen.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = innerCardBg),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha=0.2f))
                ) {
                   Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                       Text("আপনার স্কোর", fontSize = 16.sp, color = PrimaryGreen)
                       Spacer(modifier = Modifier.height(8.dp))
                       Row(verticalAlignment = Alignment.Bottom) {
                           Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                           Text("/$total", fontSize = 24.sp, color = PrimaryGreen, modifier = Modifier.padding(bottom = 6.dp))
                       }
                       Spacer(modifier = Modifier.height(16.dp))
                       Text(comment, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryGreen)
                   }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { /* Share link */ },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                ) {
                    Text("আপনিও খেলুন: ❝কুরআন রিডার❞ অ্যাপ-এ", fontSize = 12.sp, color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { 
                android.widget.Toast.makeText(context, "শেয়ারের প্রস্তুতি চলছে...", android.widget.Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    try {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        shareBitmap(context, bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(context, "শেয়ার ব্যর্থ হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Icon(androidx.compose.material.icons.Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ফলাফল কার্ড শেয়ার করুন", color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = { viewModel.resetGame() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
            border = BorderStroke(1.dp, PrimaryGreen)
        ) {
            Text("পুনরায় খেলুন", fontWeight = FontWeight.Bold)
        }
    }
}

// --- 8. AUDIO PLAYER DIALOG ---
@Composable
fun PlayerDialogContent() {
    var isPlaying by remember { mutableStateOf(false) }
    var currentReciter by remember { mutableStateOf("মিশারি রাশিদ আল-আফাসি") }
    var speed by remember { mutableStateOf(1f) }
    var sliderVal by remember { mutableStateOf(0.3f) }
    
    val reciters = listOf("মিশারি রাশিদ আল-আফাসি", "আব্দুল বাসিত আব্দুস সামাদ", "মাহের আল-মুআইকিলী")
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ক্বারী বা তেলাওয়াতকারী নির্বাচন করুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        reciters.forEach { r ->
            val isSel = currentReciter == r
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) PrimaryGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (isSel) PrimaryGreen else Border, RoundedCornerShape(8.dp))
                    .clickable { currentReciter = r }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(r, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (isSel) PrimaryGreen else MaterialTheme.colorScheme.onSurface)
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
        
        // Player Controller Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("সুরা আল-ফাতিহা", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(currentReciter, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Slider
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    colors = SliderDefaults.colors(thumbColor = PrimaryGreen, activeTrackColor = PrimaryGreen)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0:45", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("2:30", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                    }
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(56.dp)
                            .background(PrimaryGreen, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("গতি: ${String.format("%.1fx", speed)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = speed,
                        onValueChange = { speed = it },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.width(100.dp),
                        colors = SliderDefaults.colors(thumbColor = PrimaryGreen, activeTrackColor = PrimaryGreen)
                    )
                }
            }
        }
    }
}

// --- 9. HIFZ DIALOG ---
@Composable
fun HifzDialogContent(viewModel: SettingsViewModel) {
    val hifzProgress by viewModel.hifzProgress.collectAsState()
    
    val surahs = listOf(
        "সুরা আল-ফাতিহা", "সুরা আন-নাস", "সুরা আল-ফালাক", "সুরা আল-ইখলাস",
        "সুরা আল-লাহাব", "সুরা আন-নসর", "সুরা আল-কাফিরুন", "সুরা আল-কাওসার"
    )
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(surahs) { surah ->
            val status = hifzProgress[surah] ?: "শুরু করা হয়নি"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(surah, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("চলছে", "হিফজ").forEach { label ->
                            val activeLabel = if (label == "হিফজ") "হিফজ করা হয়েছে" else "চলছে"
                            val active = status == activeLabel
                            val col = if (label == "হিফজ") Color(0xFF10B981) else Color(0xFFFBBF24)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) col else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (active) col else Border, RoundedCornerShape(6.dp))
                                    .clickable {
                                        val newStatus = if (active) "শুরু করা হয়নি" else activeLabel
                                        viewModel.updateHifzProgress(surah, newStatus)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label, fontSize = 11.sp, color = if (active) Color.White else GrayText, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 10. LEARN DIALOG ---
@Composable
fun LearnDialogContent() {
    val lessons = listOf(
        Pair("পাঠ ১: আরবী হরফ পরিচিতি", "আরবী ভাষার হরফ বা বর্ণ মোট ২৯টি। এগুলো ডানদিক থেকে বামদিকে পড়তে হয়। যেমন: আলিফ (ا), বা (ب), তা (ت), ছা (ث), জীম (ج), হা (ح), খা (خ)..."),
        Pair("পাঠ ২: হরকত শিক্ষা", "জের ( ِ ), জবর ( َ ), পেশ ( ُ ) কে হরকত বলা হয়। এক জবর, এক জের ও এক পেশের উচ্চারণ তাড়াতাড়ি করতে হয়। যেমন: আ, ই, উ।"),
        Pair("পাঠ ৩: তানভীন পরিচয়", "দুই জবর, দুই জের ও দুই পেশকে তানভীন বলা হয়। তানভীনের উচ্চারণে শেষে 'ন' ধ্বনি আসে। যেমন: আন, ইন, উন।"),
        Pair("পাঠ ৪: মাখরাজ ও উচ্চারণস্থল", "আরবী হরফ উচ্চারণের মোট ১৭টি সুনির্দিষ্ট স্থান রয়েছে, একে মাখরাজ বলে। যেমন: ১ নং মাখরাজ- হলকের (কণ্ঠনালীর) শুরু হইতে হামযাহ ও হা উচ্চারিত হয়।")
    )
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(lessons) { (title, content) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                }
            }
        }
    }
}

// --- 11. VIDEO CLASSES DIALOG ---
@Composable
fun VideoDialogContent() {
    val classes = listOf(
        Pair("তাজবিদ পাঠ ১: আরবী উচ্চারণের নিয়মাবলী", "১০:১৫ মিনিট • ট্রেইনার: হাফেজ মাওলানা আব্দুর রহমান"),
        Pair("তাজবিদ পাঠ ২: সহজ উপায়ে মাখরাজ শিক্ষা", "১২:৪০ মিনিট • ট্রেইনার: হাফেজ মাওলানা আব্দুর রহমান"),
        Pair("তাফসির: সুরা ফাতিহার তাফসির ও বিশ্লেষণ", "২৫:৩০ মিনিট • তাফসিরকারী: ড. আবু বকর মুহাম্মাদ যাকারিয়া"),
        Pair("কুরআন তিলাওয়াত শুদ্ধিকরণ কর্মশালা", "১৮:৪৫ মিনিট • তেলাওয়াতকারী: ক্বারী আশরাফ আলী")
    )
    
    val context = LocalContext.current
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(classes) { (title, subtitle) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp, 50.dp)
                            .background(Color.LightGray, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    IconButton(onClick = { Toast.makeText(context, "ভিডিও লোড হচ্ছে...", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = PrimaryGreen)
                    }
                }
            }
        }
    }
}

// --- 12. CLOUD BACKUP DIALOG ---
@Composable
fun BackupDialogContent() {
    var isBackingUp by remember { mutableStateOf(false) }
    var lastBackupTime by remember { mutableStateOf("আজ সকাল ১০:৩০") }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Cloud, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("ক্লাউড ব্যাকআপ অ্যান্ড রিস্টোর", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text("আপনার বুকমার্ক ও নোট সুরক্ষিত রাখতে ব্যাকআপ নিন।", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("সর্বশেষ ব্যাকআপের সময়:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(lastBackupTime, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isBackingUp) {
            CircularProgressIndicator(color = PrimaryGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text("সার্ভারে ডাটা পাঠানো হচ্ছে...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Button(
                onClick = {
                    isBackingUp = true
                    scope.launch {
                        delay(2500) // Simulate cloud delay
                        isBackingUp = false
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        lastBackupTime = sdf.format(Date())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ব্যাকআপ নিন", color = Color.White)
            }
        }
    }
}

// --- OFFLINE SYNC DIALOG ---
@Composable
fun OfflineSyncDialogContent(viewModel: SettingsViewModel) {
    val isDownloading by viewModel.isDownloadingQuran.collectAsState()
    val progress by viewModel.quranDownloadProgress.collectAsState()
    val error by viewModel.quranDownloadError.collectAsState()
    val downloadedCount by viewModel.downloadedSurahsCount.collectAsState()
    val audioCacheSize by viewModel.audioCacheSize.collectAsState()

    // Audio Manual Download States
    val surahList by viewModel.surahList.collectAsState()
    val isDownloadingAudio by viewModel.isDownloadingAudio.collectAsState()
    val audioDownloadProgress by viewModel.audioDownloadProgress.collectAsState()
    val audioDownloadStatus by viewModel.audioDownloadStatus.collectAsState()
    val audioDownloadError by viewModel.audioDownloadError.collectAsState()

    var showSurahSelectorSheet by remember { mutableStateOf(false) }


    // Tafsir States
    val availableTafsirs by viewModel.availableTafsirs.collectAsState()
    val availableTranslations by viewModel.availableTranslations.collectAsState()
    val selectedTranslationIds by viewModel.selectedTranslationIds.collectAsState()
    val selectedTafsirIds by viewModel.selectedTafsirIds.collectAsState()
    val downloadedTafsirIds by viewModel.downloadedTafsirIds.collectAsState()
    val downloadingTafsirIds by viewModel.downloadingTafsirIds.collectAsState()
    val tafsirDownloadProgress by viewModel.tafsirDownloadProgress.collectAsState()

    var expandedSection by remember { mutableStateOf<Int?>(0) }

    // Refresh states
    LaunchedEffect(Unit) {
        
        viewModel.updateAudioCacheSize()
        viewModel.loadSurahList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "কুরআন অফলাইন ডাউনলোড ও ক্যাশ",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))


        Text(
            text = "আপনার কুরআন অডিও অফলাইন ব্যবহারের জন্য ডাউনলোড করে রাখুন যাতে ইন্টারনেট না থাকলেও শুনতে পারেন। কুরআন টেক্সট (আরবি ও বাংলা) ইতিমধ্যেই অ্যাপে অফলাইনে দেওয়া আছে।",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Word by Word & Tajweed Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (expandedSection == 0) null else 0 }.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "শব্দে শব্দে অর্থ (WbW) ও তাজবীদ ডাটা",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "১১৪টি সুরার শব্দে শব্দে বাংলা অর্থ, অনুবাদ ও তাজবীদ অফলাইন ডাটা",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expandedSection == 0) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                androidx.compose.animation.AnimatedVisibility(visible = expandedSection == 0) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))

                // Download Status UI
                val statusText: String
                val statusColor: Color
                val statusIcon: ImageVector

                if (downloadedCount == 114) {
                    statusText = "সম্পূর্ণ অফলাইন ডাউনলোড করা হয়েছে (১১৪টি সুরা WbW)"
                    statusColor = PrimaryGreen
                    statusIcon = Icons.Default.CheckCircle
                } else if (downloadedCount > 0) {
                    statusText = "আংশিক ডাউনলোড হয়েছে (${com.example.utils.DateUtil.toBengaliNumerals(downloadedCount)}/১১৪ সুরা)"
                    statusColor = Color(0xFFF59E0B)
                    statusIcon = Icons.Default.Warning
                } else {
                    statusText = "কোনো অফলাইন ডাটা নেই"
                    statusColor = Color.Red
                    statusIcon = Icons.Default.Info
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "সুরা ডাউনলোড হচ্ছে...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${com.example.utils.DateUtil.toBengaliNumerals(progress)} / ১১৪",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val progressPct = progress.toFloat() / 114f
                        LinearProgressIndicator(
                            progress = { progressPct },
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryGreen,
                            trackColor = Color.LightGray
                        )
                    }
                }

                error?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ত্রুটি: $err",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isDownloading) {
                        Button(
                            onClick = { viewModel.stopQuranDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ডাউনলোড বন্ধ করুন", color = Color.White, fontSize = 12.sp, maxLines = 1)
                        }
                    } else {
                        if (downloadedCount < 114) {
                            Button(
                                onClick = { viewModel.downloadAllQuranData() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ডাউনলোড শুরু করুন", color = Color.White, fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        if (downloadedCount > 0) {
                            OutlinedButton(
                                onClick = { viewModel.deleteDownloadedQuranData() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("মুছে ফেলুন", fontSize = 12.sp, color = Color.Red, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

        // 2. Audio Cache Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (expandedSection == 1) null else 1 }.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF06B6D4).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অডিও প্লেব্যাক অফলাইন ক্যাশ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "প্লে হওয়া আয়াতে অফলাইন ফাইল সংরক্ষণ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expandedSection == 1) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                androidx.compose.animation.AnimatedVisibility(visible = expandedSection == 1) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ব্যবহৃত ক্যাশ মেমোরি:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val formattedSize = formatBytesLocal(audioCacheSize)
                        Text(
                            text = formattedSize,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF06B6D4)
                        )
                    }

                    if (audioCacheSize > 0) {
                        OutlinedButton(
                            onClick = { viewModel.clearAudioCache() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ক্যাশ মুছুন", fontSize = 12.sp, color = Color.Red)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Border)
                Spacer(modifier = Modifier.height(16.dp))

                // Manual Audio Download Progress & Status
                if (isDownloadingAudio) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF06B6D4).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = audioDownloadStatus ?: "অডিও ফাইল ডাউনলোড করা হচ্ছে...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "অগ্রগতি:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$audioDownloadProgress%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF06B6D4)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { audioDownloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF06B6D4),
                            trackColor = Border
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.cancelAudioDownload() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ডাউনলোড বাতিল করুন", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Show last status/result if downloaded successfully
                    audioDownloadStatus?.let { status ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PrimaryGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = status,
                                color = PrimaryGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Show error if failed
                    audioDownloadError?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "ত্রুটি: $err",
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Button to manually select and download surah audio
                    Button(
                        onClick = { showSurahSelectorSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ম্যানুয়ালি সুরা অডিও ডাউনলোড করুন", fontSize = 12.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "নিয়ম: অ্যাপে যেকোনো সুরা বা আয়াত শোনার সময় সেটি স্বয়ংক্রিয়ভাবে ব্যাকগ্রাউন্ডে ক্যাশ হয়ে যাবে। তবে আপনি চাইলে উপরোক্ত বাটন ব্যবহার করে যেকোনো সুরার সম্পূর্ণ অডিও আগে থেকেই অফলাইনে প্লে করার জন্য ডাউনলোড করে রাখতে পারবেন।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Translation Data Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (expandedSection == 2) null else 2 }.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryGreen.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Translate,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অনুবাদ ডাটা",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "আপনার পছন্দের বাংলা অনুবাদ নির্বাচন করুন",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expandedSection == 2) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                androidx.compose.animation.AnimatedVisibility(visible = expandedSection == 2) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "আপনি চাইলে একাধিক অনুবাদ একসাথে নির্বাচন করতে পারেন।",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    availableTranslations.forEach { translation ->
                        val translationId = translation.id.toString()
                        val isSelected = selectedTranslationIds.contains(translationId)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleTranslationId(translationId) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleTranslationId(translationId) },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = translation.translatedName?.name ?: translation.name ?: "Unknown",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(PrimaryGreen.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = translation.languageName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                            fontSize = 9.sp,
                                            color = PrimaryGreen,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = translation.authorName ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val downloadedTranslations by viewModel.downloadedTranslations.collectAsState()
                            val translationDownloadProgress by viewModel.translationDownloadProgress.collectAsState()
                            
                            val isDownloaded = downloadedTranslations.contains(translationId)
                            val isDownloading = viewModel.downloadingTranslationIds.collectAsState().value.contains(translationId)
                            val progress = translationDownloadProgress[translationId] ?: 0f
                            
                            if (!isDownloaded) {
                                if (isDownloading) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp).padding(2.dp)) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            progress = { progress },
                                            color = PrimaryGreen,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else {
                                    androidx.compose.material3.IconButton(
                                        onClick = { viewModel.downloadTranslation(translationId) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            androidx.compose.material.icons.Icons.Default.Download,
                                            contentDescription = "Download Translation",
                                            tint = PrimaryGreen
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    androidx.compose.material.icons.Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    if (availableTranslations.isEmpty()) {
                        Text(
                            text = "অনুবাদ ডাটা লোড হচ্ছে...",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Tafsir Data Card
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (expandedSection == 3) null else 3 }.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryGreen.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.LibraryBooks,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "তাফসীর ডাটা",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "অফলাইনে পড়ার জন্য তাফসীর ডাউনলোড করুন",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expandedSection == 3) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                androidx.compose.animation.AnimatedVisibility(visible = expandedSection == 3) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "একসাথে সর্বোচ্চ ৩টি তাফসীর নির্বাচন করতে পারবেন",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    availableTafsirs.forEach { tafsir ->
                        val tafsirId = tafsir.id.toString()
                        val isSelected = selectedTafsirIds.contains(tafsirId)
                        val isDownloaded = downloadedTafsirIds.contains(tafsirId)
                        val isDownloading = downloadingTafsirIds.contains(tafsirId)
                        val progress = tafsirDownloadProgress[tafsirId] ?: 0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleTafsir(tafsirId) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDownloaded) {
                                androidx.compose.material3.Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleTafsir(tafsirId) },
                                    colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                                )
                            } else {
                                androidx.compose.material3.RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleTafsir(tafsirId) },
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = tafsir.name ?: "Unknown", 
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(PrimaryGreen.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tafsir.languageName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                            fontSize = 9.sp,
                                            color = PrimaryGreen,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                                Text(text = tafsir.authorName ?: "Unknown", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (!isDownloaded) {
                                if (isDownloading) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp).padding(2.dp)) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            progress = { progress },
                                            color = PrimaryGreen,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { viewModel.downloadTafsir(tafsirId) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            androidx.compose.material.icons.Icons.Default.Download,
                                            contentDescription = "Download Tafsir",
                                            tint = PrimaryGreen
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    androidx.compose.material.icons.Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    if (availableTafsirs.isEmpty()) {
                        Text("তাফসীর লোড হচ্ছে...", modifier = Modifier.padding(16.dp), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

        // 5. Offline Features Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (expandedSection == 4) null else 4 }.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অফলাইনে কি কি সুবিধা পাবেন?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ইন্টারনেট ছাড়াই অ্যাপের সকল প্রধান ফিচার ব্যবহার করতে পারবেন",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expandedSection == 4) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                androidx.compose.animation.AnimatedVisibility(visible = expandedSection == 4) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                val offlineFeatures = listOf(
                    Triple(Icons.Default.MenuBook, "কুরআন টেক্সট ও অনুবাদ", "১১৪টি সুরার সুপাঠ্য আরবি টেক্সট, বাংলা অনুবাদ ও তাজবীদ কালার সম্পূর্ণ অফলাইনে দেখতে ও পড়তে পারবেন।"),
                    Triple(Icons.Default.Translate, "শব্দে শব্দে অর্থ (Word by Word)", "একবার লোড বা ডাউনলোড করে নিলে প্রতিটি সুরার শব্দে শব্দে অর্থ ও উচ্চারণ অফলাইনে দেখতে পাবেন।"),
                    Triple(Icons.Default.Headphones, "অডিও তিলাওয়াত", "পূর্বে ব্যাকগ্রাউন্ডে ক্যাশ হওয়া বা আগে থেকে ডাউনলোড করা সুরার অডিও ইন্টারনেট ছাড়াই অফলাইনে শুনতে পারবেন।"),
                    Triple(Icons.Default.LibraryBooks, "তাফসীর ও বিষয়ভিত্তিক কুরআন", "ডাউনলোড করে রাখা তাফসীর এবং বিষয়ভিত্তিক কুরআনের সকল ক্যাটাগরি ও আয়াত অফলাইনে পড়তে পারবেন।"),
                    Triple(Icons.Default.Favorite, "দুআ, বুকমার্ক ও হিফজ ট্র্যাকার", "কুরআনিক দুআ, দৈনিক মাসনুন দুআ, প্রিয় আয়াত বুকমার্ক ও হিফজ ট্র্যাকিং অফলাইনে ব্যবহার করতে পারবেন।"),
                    Triple(Icons.Default.Sync, "স্বয়ংক্রিয় ব্যাকগ্রাউন্ড সিংক", "ইন্টারনেট সংযোগ এলে নতুন কোনো তথ্য থাকলে তা ব্যাকগ্রাউন্ডে স্বয়ংক্রিয়ভাবে আপডেট হবে।")
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    offlineFeatures.forEach { (icon, title, desc) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(26.dp)
                                    .background(PrimaryGreen.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

        // 5. Online Features Info Card
        val onlineAccentColor = Color(0xFF0EA5E9)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, onlineAccentColor.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (expandedSection == 5) null else 5 }.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(onlineAccentColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = onlineAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অনলাইন কানেকশনে কি কি পাবেন?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ইন্টারনেট থাকলে যেসব সুবিধা পাওয়া যাবে ও নতুন তথ্য আপডেট হবে",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expandedSection == 5) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                androidx.compose.animation.AnimatedVisibility(visible = expandedSection == 5) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                val onlineFeatures = listOf(
                    Triple(Icons.Default.CloudDownload, "অন-ডিমান্ড অডিও স্ট্রিমিং", "যেসব আয়াতের অডিও পূর্বে ডাউনলোড করা থাকবে না, অনলাইনে প্লে করলে তা সাথে সাথে শুনতে পাবেন এবং অফলাইনের জন্য সেভ হবে।"),
                    Triple(Icons.Default.MenuBook, "মুসহাফ পেজ ডাউনলোড", "বিভিন্ন স্টাইলের প্রিন্টেড মুসহাফের পেজ ও উচ্চমানের স্ক্যান প্রথমবারের মত অনলাইন থেকে ডাউনলোড করতে হবে।"),
                    Triple(Icons.Default.Update, "সর্বশেষ তাফসীর ও অনুবাদ সিংক", "সেটিংস থেকে নতুন কোনো তাফসীর বা অনুবাদ নির্বাচন করলে তা সার্ভার থেকে ইনস্ট্যান্ট লোড হবে।"),
                    Triple(Icons.Default.Sync, "ব্যাকগ্রাউন্ড অটো সিংক", "অনলাইনে থাকলে অ্যাপ ব্যাকগ্রাউন্ডে স্বয়ংক্রিয়ভাবে নতুন তথ্য সিংক করে রাখবে।")
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    onlineFeatures.forEach { (icon, title, desc) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(26.dp)
                                    .background(onlineAccentColor.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = onlineAccentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Searchable Surah Selector Dialog
    if (showSurahSelectorSheet) {
        var searchQuery by remember { mutableStateOf("") }
        val trimmedQuery = searchQuery.trim()
        val filteredSurahs = if (trimmedQuery.isEmpty()) {
            surahList
        } else {
            val diacriticsRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670-\\u06D6\\u06DC-\\u06ED]")
            val normalizedQuery = trimmedQuery.replace(diacriticsRegex, "").lowercase()

            surahList.filter { surah ->
                val surahNamePair = com.example.data.QuranData.surahNames.find { it.first == surah.number }
                val bengaliName = surahNamePair?.second?.first ?: ""
                val bengaliMeaning = surahNamePair?.second?.second ?: ""
                val arabicNameRaw = surah.name ?: ""
                val normalizedArabicName = arabicNameRaw.replace(diacriticsRegex, "")

                surah.englishName.contains(trimmedQuery, ignoreCase = true) ||
                surah.englishNameTranslation.contains(trimmedQuery, ignoreCase = true) ||
                arabicNameRaw.contains(trimmedQuery, ignoreCase = true) ||
                (normalizedQuery.isNotEmpty() && normalizedArabicName.lowercase().contains(normalizedQuery)) ||
                bengaliName.contains(trimmedQuery, ignoreCase = true) ||
                bengaliMeaning.contains(trimmedQuery, ignoreCase = true) ||
                surah.number.toString().contains(trimmedQuery) ||
                com.example.utils.DateUtil.toBengaliNumerals(surah.number).contains(trimmedQuery)
            }
        }
        
        AlertDialog(
            onDismissRequest = { showSurahSelectorSheet = false },
            title = {
                Column {
                    Text(
                        text = "অডিও ডাউনলোডের জন্য সুরা নির্বাচন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("সুরা খুঁজুন (যেমন: ফাতিহা বা 1)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF06B6D4),
                            unfocusedBorderColor = Border
                        ),
                        singleLine = true
                    )
                }
            },
            text = {
                if (filteredSurahs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("কোনো সুরা পাওয়া যায়নি", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredSurahs) { surah ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.downloadAudioForSurah(surah.number, surah.name ?: "Unknown")
                                        showSurahSelectorSheet = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFF06B6D4).copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = surah.number.toString(),
                                            color = Color(0xFF06B6D4),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = surah.name ?: "Unknown",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${surah.englishName} • ${surah.numberOfAyahs} আয়াত",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF06B6D4),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            HorizontalDivider(color = Border)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSurahSelectorSheet = false }) {
                    Text("বন্ধ করুন", color = Color(0xFF06B6D4))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

fun formatBytesLocal(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val i = (java.lang.Math.log10(bytes.toDouble()) / java.lang.Math.log10(1024.0)).toInt()
    val cappedI = if (i >= units.size) units.size - 1 else i
    return String.format(java.util.Locale.US, "%.1f %s", bytes / java.lang.Math.pow(1024.0, cappedI.toDouble()), units[cappedI])
}


@Composable
fun AboutDialogContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
        )
        Text(
            text = "কুরআন রিডার",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "সংস্করণ: 1.0.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "আমাদের সম্পর্কে",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PrimaryGreen
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "এই অ্যাপটি ডেভেলপ করা হয়েছে কুরআন তেলাওয়াত, হিফজ, এবং তাফসীর অধ্যয়নের সুবিধার্থে। এখানে শব্দে শব্দে অর্থ, তাজবীদ কালার, একাধিক ক্বারী এর অডিও, এবং সম্পূর্ণ অফলাইন সুবিধা যুক্ত করা হয়েছে। আমাদের লক্ষ্য হলো কুরআন শিক্ষাকে আরও সহজ ও সুন্দর করে তোলা।",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Justify
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "প্রাইভেসি পলিসি (Privacy Policy)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PrimaryGreen
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "এই অ্যাপটি ব্যবহারকারীর কোনো ব্যক্তিগত তথ্য (Personal Data) সংগ্রহ বা সংরক্ষণ করে না। অ্যাপের বুকমার্ক, হিফজ প্রগ্রেস এবং ইউজার সেটিংস সম্পূর্ণভাবে আপনার ডিভাইসে লোকালি সংরক্ষিত হয়। অ্যাপের কোনো ডেটা কোনো থার্ড-পার্টির সার্ভারে পাঠানো হয় না বা শেয়ার করা হয় না।\n\nকোরআনের অডিও ডাউনলোড এবং অফলাইন সুবিধার জন্য শুধুমাত্র ইন্টারনেট পারমিশন ব্যবহার করা হয়।",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Justify
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Made with ❤️ by MuslimsLibrary",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun ContactDialogContent() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ContactMail,
            contentDescription = null,
            tint = Color(0xFFF97316),
            modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
        )
        Text(
            text = "আমাদের সাথে যোগাযোগ করুন",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "যেকোনো পরামর্শ বা প্রশ্নের জন্য",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Email
        ContactCard(
            title = "ইমেইল",
            value = "ammamun94@gmail.com",
            icon = Icons.Default.Email,
            color = Color(0xFFEA4335),
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:ammamun94@gmail.com")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle case where no email app is available
                }
            }
        )
        
        // Mobile
        ContactCard(
            title = "মোবাইল নম্বর",
            value = "+880 1600-989555",
            icon = Icons.Default.Phone,
            color = Color(0xFF34A853),
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:+8801600989555")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle case
                }
            }
        )
        
        // WhatsApp
        ContactCard(
            title = "হোয়াটসঅ্যাপ (WhatsApp)",
            value = "মেসেজ করুন",
            icon = Icons.Default.Chat,
            color = Color(0xFF25D366),
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://wa.me/qr/PC4IQUUS3OGJE1")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle case
                }
            }
        )
        
        // Facebook
        ContactCard(
            title = "ফেসবুক (Facebook)",
            value = "MuslimsLibrary",
            icon = Icons.Default.ThumbUp,
            color = Color(0xFF1877F2),
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://www.facebook.com/MuslimsLibrary")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle case
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ContactCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
fun ThemeDialogContent(viewModel: SettingsViewModel) {
    val currentTheme by viewModel.themeState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = Color(0xFF9C27B0),
            modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
        )
        Text(
            text = "থিম নির্বাচন করুন",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        ThemeOption(
            title = "সিস্টেম ডিফল্ট (System)",
            isSelected = currentTheme == "System",
            onClick = { viewModel.setTheme("System") }
        )
        
        ThemeOption(
            title = "লাইট থিম (Light)",
            isSelected = currentTheme == "Light",
            onClick = { viewModel.setTheme("Light") }
        )
        
        ThemeOption(
            title = "ডার্ক থিম (Dark)",
            isSelected = currentTheme == "Dark",
            onClick = { viewModel.setTheme("Dark") }
        )
    }
}

@Composable
fun ThemeOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PrimaryGreen
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDialogContent(viewModel: SettingsViewModel) {
    var activeDhikrType by remember { mutableStateOf<com.example.utils.DhikrType?>(null) }

    if (activeDhikrType != null) {
        DhikrReminderScreen(
            type = activeDhikrType!!,
            onBackClick = { activeDhikrType = null }
        )
        return
    }

    val dailyEnabled by viewModel.dailyMessageEnabled.collectAsState()
    val dailyHour by viewModel.dailyMessageHour.collectAsState()
    val dailyMinute by viewModel.dailyMessageMinute.collectAsState()

    val context = LocalContext.current

    var isIslamicEventsEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("quran_menu_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("islamic_events_reminder_enabled", true)
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleDailyMessage(true)
        } else {
            android.widget.Toast.makeText(context, "নোটিফিকেশন পারমিশন প্রয়োজন", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val timeState = androidx.compose.material3.rememberTimePickerState(
            initialHour = dailyHour,
            initialMinute = dailyMinute,
            is24Hour = false
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDailyMessageTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("বাতিল")
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.TimePicker(state = timeState)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFBBF24).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFFBBF24))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "দৈনিক ইসলামিক বার্তা",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "প্রতিদিন আয়াত বা হাদিস রিমাইন্ডার পান",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dailyEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        viewModel.toggleDailyMessage(true)
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    viewModel.toggleDailyMessage(true)
                                }
                            } else {
                                viewModel.toggleDailyMessage(false)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen, checkedTrackColor = PrimaryGreen.copy(alpha = 0.5f))
                    )
                }

                if (dailyEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Border)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("রিমাইন্ডারের সময়", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            val timeStr = String.format("%02d:%02d %s", if (dailyHour % 12 == 0) 12 else dailyHour % 12, dailyMinute, if (dailyHour >= 12) "PM" else "AM")
                            Text(timeStr, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        }
                    }
                }
            }
        }

        // রোজা ও দিবস নোটিফিকেশন
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF10B981))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "সোম-বৃহস্পতি, আইয়ামে বীজ ও দিবস রিমাইন্ডার",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "রবি ও বুধবার বিকেলে (আসরের পর) সোম-বৃহস্পতিবারের রোজা, ১২ হিজরী বিকেলে আইয়ামে বীজ ও বিশেষ দিবসের নোটিফিকেশন আসবে।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isIslamicEventsEnabled,
                        onCheckedChange = { checked ->
                            if (checked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            isIslamicEventsEnabled = checked
                            context.getSharedPreferences("quran_menu_prefs", android.content.Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("islamic_events_reminder_enabled", checked)
                                .apply()

                            if (checked) {
                                com.example.receiver.IslamicEventReceiver.scheduleNextAlarm(context)
                            } else {
                                com.example.receiver.IslamicEventReceiver.cancelAlarm(context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen, checkedTrackColor = PrimaryGreen.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // দরূদ রিমাইন্ডার কার্ড
        val duroodConfig = remember(activeDhikrType) { com.example.utils.DhikrReminderManager.getConfig(context, com.example.utils.DhikrType.DUROOD) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { activeDhikrType = com.example.utils.DhikrType.DUROOD },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (duroodConfig.isEnabled) PrimaryGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF00A86B).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📿", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "দরূদ রিমাইন্ডার",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (duroodConfig.isEnabled) "চালু (${duroodConfig.intervalMinutes} মিনিট পরপর)" else "বন্ধ • সেটিংস পরিবর্তন করতে ট্যাপ করুন",
                            fontSize = 12.sp,
                            color = if (duroodConfig.isEnabled) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "বিস্তারিত",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ইস্তেগফার রিমাইন্ডার কার্ড
        val istighfarConfig = remember(activeDhikrType) { com.example.utils.DhikrReminderManager.getConfig(context, com.example.utils.DhikrType.ISTIGHFAR) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { activeDhikrType = com.example.utils.DhikrType.ISTIGHFAR },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (istighfarConfig.isEnabled) PrimaryGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🤲", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ইস্তেগফার রিমাইন্ডার",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (istighfarConfig.isEnabled) "চালু (${istighfarConfig.intervalMinutes} মিনিট পরপর)" else "বন্ধ • সেটিংস পরিবর্তন করতে ট্যাপ করুন",
                            fontSize = 12.sp,
                            color = if (istighfarConfig.isEnabled) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "বিস্তারিত",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}



@Composable
fun TextWithArabicFont(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    arabicFontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    fontWeight: FontWeight? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
    lineHeight: androidx.compose.ui.unit.TextUnit = 24.sp
) {
    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
        // Regex to match Arabic letters
        val arabicRegex = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]+(?:[0-9\\s{}«».,:؛؟\\-]+[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]+)*")
        var lastIndex = 0
        arabicRegex.findAll(text).forEach { matchResult ->
            append(text.substring(lastIndex, matchResult.range.first))
            withStyle(
                androidx.compose.ui.text.SpanStyle(
                    fontFamily = com.example.ui.theme.meQuranFont,
                    fontSize = arabicFontSize
                )
            ) {
                append(matchResult.value)
            }
            lastIndex = matchResult.range.last + 1
        }
        append(text.substring(lastIndex))
    }
    Text(
        text = annotatedString,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        lineHeight = lineHeight,
        modifier = modifier
    )
}

@Composable
fun ManzilInfoContent() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        val fullText = """মানযিল এর আমল: পরিচিতি, বৈশিষ্ট্য ও কিছু কথা

মানযিল কী?

মানযিল মূলত কুরআনে নির্বাচিত কিছু আয়াতের সমষ্টি। কুরআনের ১৮ টি স্থান থেকে মোট ৭৯ টি আয়াত নিয়ে প্রস্তুত হয়েছে মানযিল। যথা:

১. সূরা ফাতেহা, ৭ আয়াত
২. সূরা বাকারার শুরুর ৫ আয়াত
৩. সূরা বাকারার ১৬৩ নং আয়াত
৪. সূরা বাকারা আয়াত নং ২৫৫, ২৫৬ ও ২৫৭, অর্থাৎ আয়াতুল কুরসী ও তার পরবর্তী ২ আয়াত
৫. সূরা বাকারার শেষ ৩ আয়াত, আয়াত নং ২৮৪, ২৮৫ ও ২৮৬
৬. সূরা আলে ইমরান এর ১৮ নং আয়াত
৭. সূরা আলে ইমরান এর ২৬ ও ২৭ নং আয়াত
৮. সূরা আরাফ এর ৫৪, ৫৫ ও ৫৬ নং আয়াত
৯. সূরা বনী ইসরাঈল এর শেষ ২ আয়াত, ১১০ ও ১১১ নং আয়াত
১০. সূরা মুমিনুন এর শেষ ৪ আয়াত, ১১৫, ১১৬, ১১৭ ও ১১৮ নং আয়াত
১১. সূরা সাফ্ফাত এর শুরু থেকে ১১ আয়াত
১২. সূরা রহমান এর ৩৩ থেকে ৪০ পর্যন্ত, মোট ৮ আয়াত
১৩. সূরা হাশর এর শেষ ৪ আয়াত। ২১, ২২, ২৩ ও ২৪ নং আয়াত
১৪. সূরা জ্বিন এর শুরুর ৪ আয়াত
১৫. সূরা কাফিরুন, মোট ৬ আয়াত
১৬. সূরা ইখলাস, মোট ৪ আয়াত
১৭. সূরা ফালাক্ব, মোট ৫ আয়াত
১৮. সূরা নাস, মোট ৬ আয়াত

আমাদের সমাজের দ্বীনদার শ্রেণীর মাঝে ‘মানযিল’ এর আয়াত সমষ্টি তেলাওয়াতের আমল প্রচলিত আছে। উলামায়ে কিরামও এ আমল করার প্রতি উৎসাহিত করে থাকেন।

আমাদের জানামতে মানযিল নামে এ আমলের প্রচলন মূলত শাইখুল হাদীস মাওলানা যাকারিয়া কান্ধলভী রহ. (মৃত ১৪০২) এর মাধ্যমে। এটি শাইখুল হাদীস রহ. এর বংশের বুজুর্গদের আমল ছিল। আমাদের বাজারে যে মনযিল নামে ছোট পুস্তিকা পাওয়া যায় তা মূলত শাইখুল হাদীস মাওলানা যাকারিয়া রহ. এর সাহেবজাদা মাওলানা তালহা কান্ধলভী রহ. এর উদ্যোগে প্রথম প্রকাশিত হয়েছিল। পরবর্তীতে তার অনুকরণে অরো অনেক প্রকাশক ছাপার উদ্যোগ নিয়েছেন।

হযরত মাওলানা তালহা কান্ধলভী রহ. এ পুস্তিকার ভূমিকায় লিখেছেন,
আমাদের ঘরের নারীরা যখন কোন অসুস্থ নারীর জন্য মনযিল এর আমল দিতেন তখন তাদের মূল কুরআনে চিহ্নিত করে দিতে হত। তাই ইচ্ছা হল যে, এটিকে ভিন্নভাবে পাছানোর উদ্যোগ নেওয়া হোক, তাহলে তাদেরকে আমলটি বলে দেওয়া সহজ হবে। [মানযিল পৃষ্ঠা ৩]

অবশ্য এরও আগে শাহ ওয়ালি উল্লাহ দেহলভী রহ. (মৃত ১১৭৬) এর বংশেও এ আমলের কথা পাওয়া যায়। তবে তাদের সময় এ আমল ‘৩৩ আয়াতের আমল’ নামে পরিচিত ছিল। যার সাথে আমাদের প্রচলিত মানযিলের আয়াত সংখ্যার কিছু পার্থক্য রয়েছে। এর আলোচনা একটু পরেই আসছে।

এ আমলের ফযিলত ও বৈশিষ্ট্য

এক.
এতে সন্দেহের কোন অবকাশ নেই যে কুরআন তিলাওয়াত সর্বোত্তম যিকির। এর প্রতি অক্ষর তিলাওয়াতের বিনিময়ে কমপক্ষে দশ নেকি লাভ হয়। হযরত আব্দুল্লাহ ইবনে মাসউদ রা. বলেন, রাসূল সা. বলেছেন, যে ব্যক্তি কুরআনের একটি হরফ তিলাওয়াত করবে সে একটি নেকি লাভ করবে। আর একটি নেকি দশটি নেকিতে বৃদ্ধি করা হবে। আমি বলছি না ‘আলিফ লাম মিম’ একটি হরফ; বরং ‘আলিফ’ একটি হরফ, ‘লাম’ একটি হরফ ও ‘মিম’ একটি হরফ। [সুনানে তিরমিযী ২৯১০, সনদ সহীহ]
তাই মানযিল এর নির্বাচিত কিছু আয়াত তিলাওয়াত করলে অবশ্যই উল্লেখিত সওয়াব লাভ হবে।

দুই.
পাশাপাশি এর যে সকল আয়াতের ব্যাপারে ভিন্ন কোন ফযিলত ও বৈশিষ্ট্যের কথা হাদীসে রয়েছে তেলাওয়াতের মাধ্যমে তাও লাভ হবে। নিচে উদাহরণ স্বরূপ এমন কিছু নির্ভরযোগ্য হাদীসের উদ্ধৃতি পেশ করা হল।

সূরা ফাতেহা সম্পর্কে : আবূ সা’ঈদ খুদরী রা. বর্ণনা করেছেন। তিনি বলেন, একবার আমরা সফরে চলছিলাম। পথিমধ্যে অবতরণ করলাম। তখন একটি বালিকা এসে বলল, এখানকার গোত্রের সরদারকে সাপে কেটেছে। আমাদের পুরুষগণ বাড়িতে নেই। অতএব, আপনাদের মধ্যে এমন কেউ আছেন কি, যিনি ঝাড়-ফুঁক করতে পারেন? তখন আমাদের মধ্য থেকে একজন ঐ বালিকাটির সঙ্গে গেলেন। যদিও আমরা ভাবিনি যে সে ঝাড়-ফুঁক জানে। এরপর সে ঝাড়-ফুঁক করল এবং গোত্রের সরদার সুস্থ হয়ে উঠল। এতে সর্দার খুশী হয়ে তাকে ত্রিশটি বকরী দান করলেন এবং আমাদের সকলকে দুধ পান করালেন। ফিরে আসার পথে আমারা জিজ্ঞেস করলাম, তুমি ভালভাবে ঝাড়-ফুঁক করতে জান? সে উত্তর করল, না, আমি তো কেবল উম্মুল কিতাব- সূরা ফাতিহা দিয়েই ঝাড়-ফুঁক করেছি। আমরা তখন বললাম, যতক্ষণ না আমরা নবী সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম এর কাছে পৌঁছে তাঁকে জিজ্ঞেস করি ততক্ষণ কেউ কিছু বলবে না। এরপর আমরা মদিনায় পৌঁছে নবী সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম এর কাছে ঘটনাটি বললাম। তিনি বললেন, সে কেমন করে জানল যে, তা (সূরা ফাতিহা) রোগ আরোগ্যের জন্য ব্যবহার করা যেতে পারে? তোমরা নিজেদের মধ্যে এগুলো বণ্টন করে নাও এবং আমার জন্যও একটা ভাগ রেখো। [সহীহ বুখারী ৫০০৭]

সূরা বাকারা সম্পর্কে : আবূ হুরায়রাহ রা. বর্ণনা করেছেন, রসূলুল্লাহ সা. বলেছেন, তোমাদের ঘরসমূহকে কবর সদৃশ করে রেখো না, কারণ যে ঘরে সূরাহ্ বাক্বারাহ্ পাঠ করা হয় শয়তান সে ঘর থেকে পালিয়ে যায়। [সহীহ মুসলিম ৭৮০]

সূরা বাকারার শেষ দুই আয়াত সম্পর্কে : ১. হযরত আবু মাসউদ রা. বলেন, রাসূলুল্লাহ সা. বলেছেন, যে ব্যক্তি কোন রাতে সূরা বাকারার শেষ দুই আয়াত পাঠ করবে তার জন্য তা যথেষ্ট হয়ে যাবে। [সহীহ বুখারী ৪০০৮, ৫০০৮, ৫০৪০]

২. নু‘মান ইবনু বাশীর রা. বর্ণনা করেছেন, নবী সা. বলেছেন, আল্লাহ তা‘আলা আসমান-জামিন সৃষ্টির দুই হাজার বছর পূর্বে একটি কিতাব লিখেছেন। সেই কিতাব হতে তিনি দু‘টি আয়াত নাযিল করছেন। সেই দু‘টি আয়াতের মাধ্যমেই সূরা আল-বাক্বারা সমাপ্ত করেছেন। যে ঘরে তিন রাত এ দু‘টি আয়াত তিলাওয়াত করা হয় শয়তান সেই ঘরের নিকট আসতে পারে না। [সুনানে তিরমিযী ২৮৮২, মুসনাদে আহমদ ১৮৪১৪, সনদ সহীহ]

সূরা ফাতেহা ও সূরা বাকারার শেষাংশ সম্পর্কে : আবদুল্লাহ ইবনু আব্বাস রা. বর্ণনা করেছেন, তিনি বলেন, একদিন জিবরাঈল আ. নবী সা. এর কাছে বসেছিলেন। সে সময় তিনি উপর দিক থেকে দরজা খোলার একটা প্রচণ্ড আওয়াজ শুনতে পেয়ে মাথা উঠিয়ে বললেন, এটি আসমানের একটি দরজা। আজকেই এটি খোলা হলো- ইতোপূর্বে আর কখনো খোলা হয়নি। আর এ দরজা দিয়ে একজন ফেরেশতা পৃথিবীতে নেমে আসলেন। আজকের এ দিনের আগে আর কখনো তিনি পৃথিবীতে আসেননি। তারপর তিনি সালাম দিয়ে বললেন, আপনি আপনাকে দেয়া দু’টি নূর বা আলোর সুসংবাদ গ্রহণ করুন। আপনার পূর্বে আর কোন নবীকে তা দেয়া হয়নি। আর ঐ দু’টি নূর হলো সূরা ফাতিহা এবং সূরা আল বাক্বারাহ্ এর শেষাংশ। এর যে কোন হরফ আপনি পড়বেন তার মধ্যকার প্রার্থিত বিষয় আপনাকে দেয়া হবে। [সহীহ মুসলিম ৮০৬]

আয়াতুল কুরসী সম্পর্কে : আবূ হুরায়রা (রাঃ) বর্ণনা করেছেন, তিনি বলেন, আল্লাহর রাসূল সা. আমাকে রমযানের যাকাত (সাদাকাতুল ফিতরের) হিফাজতের দায়িত্ব প্রদান করলেন। অতঃপর আমার নিকট এক আগন্তুক আসল। সে তার দু’হাতের আঁজলা ভরে খাদ্যশস্য গ্রহণ করতে লাগল। তখন আমি তাকে ধরে ফেললাম এবং বললাম, আমি অবশ্যই তোমাকে আল্লাহর রাসূল সা. এর নিকট নিয়ে যাব। তখন সে একটি হাদীস উল্লেখ করল এবং বলল, যখন তুমি বিছানায় শুতে যাবে, তখন আয়াতুল কুরসী পড়বে। তাহলে সর্বদা আল্লাহর পক্ষ হতে তোমার জন্য একজন হিফাযতকারী থাকবে এবং সকাল হওয়া অবধি তোমার নিকট শয়তান আসতে পারবে না। তখন নবী সা. বললেন, সে তোমাকে সত্য বলেছে, অথচ সে মিথ্যাচারী এবং শয়তান ছিল। [সহীহ বুখারী ৩২৭৫]

সূরা আলে ইমরান সম্পর্কে : আবূ উসামা আল বাহিলী রা. বর্ণনা করেছেন, তিনি বলেন, আমি রসূলুল্লাহ সা. কে বলতে শুনেছি, তোমরা কুরআন পাঠ কর। কারন কিয়ামতের দিন তার পাঠকারীর জন্য সে শাফাআতকারী হিসেবে আসবে। তোমরা দু’টি উজ্জ্বল সূরা অর্থাৎ সূরা আল বাক্বারাহ এবং সূরা আল ইমরান পড়। ক্বিয়ামতের দিন এ দু’টি সুরা এমনভাবে আসবে যেন তা দু’খ- মেঘ অথবা দু’টি ছায়াদানকারী অথবা দু’ঝাঁক উড়ন্ত পাখি যা তার পাঠকারীর পক্ষ হয়ে কথা বলবে। [সহীহ মুসলিম ৮০৪]

সূরা ইসরা সম্পর্কে : আয়িশা রা. বর্ণনা করেছেন, সূরা বানী ইসরাঈল ও সূরা আয্-যুমার তিলাওয়াত না করা পর্যন্ত নবী সা. ঘুমাতেন fixনা। [সুনানে তিরমিযী ২৯২০, মুসনাদে আহমদ ২৪৯০৮, সনদ হাসান]

সূরা কাফিরুন সম্পর্কে : ফরওয়াহ ইবনু নাওফাল রা. বর্ণনা করেছেন, তিনি নবী সা. এর কাছে এসে বললেন, হে আল্লাহর রাসূল! আমাকে কিছু শিখিয়ে দিন, যা আমি বিছানাগত হওয়াকালে বলতে পারি। তিনি বললেন, তুমি ‘কুল ইয়া আইয়্যুহাল কাফিরুন’ সূরাটি তিলাওয়াত কর। কারণ তা শিরক হতে মুক্তির ঘোষণা। [সুনানে তিরমিযী ৩৪০৩, সুনানে আবু দাউদ ৫০৫৫, সনদ হাসান]

সূরা ইখলাস ও সূরা ফালাক-নাস সম্পর্কে : আবদুল্লাহ ইবনু খুবাইব রা. বর্ণনা করেছেন, তিনি বলেন, এক ঘুটঘুটে অন্ধকার ও বৃষ্টিমুখর রাতে আমাদের নামায আদায় করানোর জন্য আমরা রাসূল সা. এর সন্ধানে বের হলাম। আমি তাঁর দেখা পেলে তিনি বললেন, বল। কিন্তু আমি কিছুই বললাম না। তিনি পুনরায় বললেন, বল। এবারও আমি কিছুই বললাম না। তিনি আবার বললেন, বল। এবার আমি প্রশ্ন করলাম, আমি কি বলব? তিনি বললেন, তুমি প্রতি দিন বিকালে ও সকালে তিনবার সূরা আল-ইখলাস, সূরা আল-ফালাক্ব ও সুরা আন-নাস পাঠ করবে, তাহলে তা সবকিছু থেকে তোমার জন্য যথেষ্ট হয়ে যাবে। [সুনানে তিরমিযী ৩৫৭৫, মুসনাদে আহমদ ২২৬৬৪, সনদ হাসান]

তিন.
উক্ত ফযিলত ছাড়াও সুস্থতা লাভ কিংবা বিভিন্ন আছর ও যাদু-টোনা আত্মরক্ষার উদ্দেশ্যে কুরআনের আয়াত দ্বারা ঝাড়ফুঁক করাও জায়েয। তাই শুধু এ উদ্দেশ্যে হলেও এ আয়াতগুলো পড়া যেতে পারে। ইবনে তাইমিয়া রহ. বলেন,

وَأَمَّا مُعَالَجَةُ الْمَصْرُوعِ بِالرُّقَى، وَالتَّعَوُّذَاتِ. فَهَذَا عَلَى وَجْهَيْنِ: فَإِنْ كَانَتْ الرُّقَى وَالتَّعَاوِيذُ مِمَّا يُعْرَفُ مَعْنَاهَا، وَمِمَّا يَجُوزُ فِي دِينِ الْإِسْلَامِ أَنْ يَتَكَلَّمَ بِهَا الرَّجُلُ، دَاعِيًا لِلَّهِ، ذَاكِرًا لَهُ، وَمُخَاطِبًا لِخَلْقِهِ، وَنَحْوُ ذَلِكَ، فَإِنَّهُ يَجُوزُ أَنْ يُرْقَى بِهَا الْمَصْرُوعُ، وَيُعَوَّذَ، فَإِنَّهُ قَدْ ثَبَتَ فِي الصَّحِيحِ عَنْ النَّبِيِّ - صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ -: «أَنَّهُ أَذِنَ فِي الرُّقَى، مَا لَمْ تَكُنْ شِرْكًا». وَقَالَ: «مَنْ اسْتَطَاعَ مِنْكُمْ أَنْ يَنْفَعَ أَخَاهُ فَلْيَفْعَلْ»، وَإِنْ كَانَ فِي ذَلِكَ كَلِمَاتٌ مُحَرَّمَةٌ مِثْلُ أَنْ يَكُونَ فِيهَا شِرْكٌ، أَوْ كَانَتْ مَجْهُولَةَ الْمَعْنَى يُحْتَمَلُ أَنْ يَكُونَ فِيهَا كُفْرٌ، فَلَيْسَ لِأَحَدٍ أَنْ يَرْقِيَ بِهَا.

অর্থ: আছরগ্রস্থ ব্যক্তিকে ঝাড়-ফুঁকের মাধ্যমে চিকিৎসা করা দুই প্রকার। তা যদি এমন শব্দ দ্বারা হয় যার অর্থ সুস্পষ্ট এবং তা ইসলামী শরীয়ত অনুযায়ী পড়াও বৈধ (যেমন, আল্লাহর কোন নাম, যিকির বা সৃষ্টিকুলকে লক্ষ করে আল্লাহর কোন সম্বোধন ইত্যাদি) দ্বারা আছরগ্রস্থ ব্যক্তিকে ঝাড়-ফুঁক করা জায়েয। কারণ, সহীহ হাদীসের বর্ণনা মতে রাসূল সা. শিরক না হলে ঝাড়-ফুঁক করার অনুমতি দিয়ে বলেছেন, তোমাদের কেউ যদি তার ভাইকে উপকার করতে সক্ষম হয় তাহলে সে যেন তা করে। আর যদি ঝাড়-ফুঁক হারাম বাক্য দ্বারা হয় (যেমন, কুফর-শিরকযুক্ত বাক্য) অথবা এমন বাক্য যার অর্থ জানা নেই (যাতে কুফর ও শিরকের সম্ভাবনা রয়েছে) তা দ্বারা ঝাড়-ফুঁক করা জায়েয নয়। [আল ফাতাওয়াল কুবরা ৩/১৩; মজমূউল ফাতাওয়া ২৪/২৭৮]

হাফেজ ইবনে হাজার আসকালানী রহ. বলেন,
وَقَدْ أَجْمَعَ الْعُلَمَاءُ عَلَى جَوَازِ الرُّقَى عِنْدَ اجْتِمَاعِ ثَلَاثَةِ شُرُوطٍ أَنْ يَكُونَ بِكَلَامِ اللَّهِ تَعَالَى أَوْ بِأَسْمَائِهِ وَصِفَاتِهِ وَبِاللِّسَانِ الْعَرَبِيِّ أَوْ بِمَا يُعْرَفُ مَعْنَاهُ مِنْ غَيْرِهِ وَأَنْ يُعْتَقَدَ أَنَّ الرُّقْيَةَ لَا تُؤَثِّرُ بِذَاتِهَا بَلْ بِذَاتِ اللَّهِ تَعَالَى.

তিনটি শর্ত বিদ্যমান থাকার শর্তে উলামায়ে কিরাম ঝাড়-ফুঁক বৈধ হওয়ার ব্যাপারে একমত। ঝাড়-ফুঁকের বাক্যগুলো আল্লাহর কালাম, তাঁর নাম এ সিফাত হতে হবে, আরবই ভাষা বা অর্থ সুস্পষ্ট এমন বাক্য দ্বারা হতে হবে এবং বিশ্বাস রাখতে হবে যে ঝাড়-ফুঁক শুধু মাধ্যম মাত্র; মূলত পতিক্রিয়া সৃষ্টি করেন আল্লাহ তায়ালা। [ফাতহুল বারী ১০/১৯৫]

এ বিষয়ে আরো দেখুন: সিলসিলাতুল আহাদীসিস সহীহা আলবানী, হাদীস নং ৪৭২; তাইসিরুল আজিজিল হামীদ শরহু কিতাবিত তাউহীদ পৃষ্ঠা নং ১৩৩, শরহুন নববী ১৪/১৬৮]

ফযিলত হিসাবে বিশেষ হাদীসের উদ্ধৃতি

মুদ্রিত মানযিলের কোন কোন সংস্করণে মানযিলের ফযিলত হিসাবে একটি বিশেষ হাদীসের উদ্ধৃতি দেওয়া হয়েছে। কোন নুসখায় মুসনাদে আহমদ থেকে কোন নুসখায় ইবনে মাযা থেকে। হাদীসটির বিবরণ এমন,

حَدَّثَنَا هَارُونُ بْنُ حَيَّانَ قَالَ: حَدَّثَنَا إِبْرَاهِيمُ بْنُ مُوسَى قَالَ: أَنْبَأَنَا عَبْدَةُ بْنُ سُلَيْمَانَ قَالَ: حَدَّثَنَا أَبُو جَنَابٍ، عَنْ عَبْدِ الرَّحْمَنِ بْنِ أَبِي لَيْلَى، عَنْ أَبِيهِ أَبِي لَيْلَى قَالَ: كُنْتُ جَالِسًا عِنْدَ النَّبِيِّ صَلَّى اللهُ عَلَيْهِ وَسَلَّمَ إِذْ جَاءَهُ أَعْرَابِيٌّ فَقَالَ: إِنَّ لِي أَخًا وَجِعًا، قَالَ: «مَا وَجَعُ أَخِيكَ؟» قَالَ: بِهِ لَمَمٌ، قَالَ: «اذْهَبْ فَأْتِنِي بِهِ». قَالَ: فَذَهَبَ فَجَاءَ بِهِ، فَأَجْلَسَهُ بَيْنَ يَدَيْهِ، فَسَمِعْتُهُ عَوَّذَهُ بِفَاتِحَةِ الْكِتَابِ، وَأَرْبَعِ آيَاتٍ مِنْ أَوَّلِ الْبَقَرَةِ، وَآيَتَيْنِ مِنْ وَسَطِهَا، {وَإِلَهُكُمْ إِلَهٌ وَاحِدٌ}، وَآيَةِ الْكُرْسِيِّ، وَثَلَاثِ آيَاتٍ مِنْ خَاتِمَتِهَا، وَآيَةٍ مِنْ آلِ عِمْرَانَ أَحْسِبُهُ قَالَ: {شَهِدَ اللَّهُ أَنَّهُ لَا إِلَهَ إِلَّا هُوَ}، وَآيَةٍ مِنَ الْأَعْرَافِ: {إِنَّ رَبَّكُمُ اللَّهُ الَّذِي خَلَقَ} الْآيَةَ، وَآيَةٍ مِنَ الْمُؤْمِنِينَ، {وَمَنْ يَدْعُ مَعَ اللَّهِ إِلَهًا آخَرَ لَا بُرْهَانَ لَهُ بِهِ}، وَآيَةٍ مِنَ الْجِنِّ، {وَأَنَّهُ تَعَالَى جَدُّ رَبِّنَا مَا اتَّخَذَ صَاحِبَةً وَلَا وَلَدًا}، وَعَشْرِ آيَاتٍ مِنْ أَوَّلِ الصَّافَّاتِ، وَثَلَاثِ آيَاتٍ مِنْ آخِرِ الْحَشْرِ، وَقُلْ هُوَ اللَّهُ أَحَدٌ، وَالْمُعَوِّذَتَيْنِ، فَقَامَ الْأَعْرَابِيُّ، قَدْ بَرَأَ لَيْسَ بِهِ بَأْسٌ.

অর্থ: আব্দুর রহমান ইবনে আবি লাইলা রহ. বর্ণনা করেন তাঁর পিতা আবু লাইলা থেকে। তিনি বলেন, আমি নবী সা. এর নিকট বসে থাকা অবস্থায় এক বেদুইন তাঁর নিকটে এসে বললো, আমার এক অসুস্থ ভাই আছে। তিনি বললেন, তোমার ভাই কী রোগে আক্রান্ত? সে বললো, (কোন কিছুর) কুপ্রভাব (আছর)। তিনি বললেন, তুমি যাও এবং তাকে আমার নিকট নিয়ে এসো। আবূ লায়লা রা. বলেন, সে গিয়ে তার ভাইকে নিয়ে আসলে তিনি তাকে নিজের সামনে বসলেন। আমি শুনতে পেলাম, তিনি সূরা ফাতিহা, সূরা বাকারার প্রথম চার আয়াত, সূরা বাকারার মধ্যখানের দু’আয়াত (১৬৩-১৬৪ নং আয়াত), আয়াতুল কুরসী (২৫৫ নং আয়াত) এবং বাকারার শেষ তিন আয়াত (২৮৪-২৮৬ আয়াত) এবং আল ইমরানের একটি আয়াত, আমার মনে হয় তিনি ১৮ নং আয়াত পড়েছিলেন এবং সূরা আরাফের এক আয়াত (৫৪ নং আয়াত), সূরা মুমিনূনের এক আয়াত (১১৭ নং আয়াত), সূরা জিন-এর এক আয়াত (৩ নং আয়াত), সুরা সাফ্ফাত এর প্রথম দশ আয়াত, সুরা হাশরের শেষ তিন (২২, ২৩ ও ২৪) আয়াত, সূরা ইখলাস, সূরা ফালাক ও সূরা নাস পড়ে তাকে ফুঁ দিলেন। তাতে বেদুইন এমনভাবে সুস্থ হয়ে দাঁড়ালো যে, তার কোন রোগই অবশিষ্ট নেই।

হাদীসটি যে সকল কিতাবে উদ্ধৃত হয়েছে: মুসনাদে আহমদ (হাদীস নং ২১১৭৪); সুনানে ইবনে মাজাহ (হাদীস নং ৩৫৪৯); মুসনাদে আবু ইয়লা আল-মাউসিলী (হাদীস নং ১৫৯৪); আদ্-দুআ তাবরানী (হাদীস নং ১০৮০); আমালুল ইয়াউমি ওয়াল লাইলা, ইবনুস্ সুন্নী (হাদীস নং ৬৩২); মুস্তাদরাকে হাকিম (হাদীস নং ৮২৬৯); আদ-দাউআতুল কাবীর বাইহাকী (হাদীস নং ৫৯৫); আল-আযকার নববী (হাদীস নং ৩৭৬); সিলাহুল মুমিন তকিউদ্দিন ইবনুল ইমাম (হাদীস নং ৭৭০); আল হিসনুল হাসীন ইবনুল যাজারী

তবে প্রমাণ হিসাবে এ হাদীস উদ্ধৃত করার ক্ষেত্রে কিছু আপত্তি রয়েছে। যার কারণে এ হাদীস প্রমাণ হিসাবে পেশ করার যোগ্য বলে বিবেচিত হয় না।

১ নং আপত্তি : সনদের বিচারে হাদীসটি দুর্বল
২ নং আপত্তি : আয়াত সংখ্যার অমিল
৩ নং আপত্তি : মানযিলের যে সব ফযিলত বলা হয় তার সব এ হাদীসে নেই

এ সব কারণে মানযিলের ফযিলত হিসাবে এ হাদীসকে প্রমাণ হিসাবে পেশ করা অনুচিত। এটিকে প্রমাণ হিসাবে পেশ করার প্রয়োজনও নেই। বৈশিষ্ট্য হিসাবে প্রবন্ধের শুরুতে উল্লেখিত ফযিলত ও বৈশিষ্ট্যগুলোই যথেষ্ট।

৩৩ আয়াত এর আমল বনাম মানযিলের আমল

মানযিলের এ আমল ‘৩৩ আয়াতের আমল’ নামেও অনেকের মুখে পরিচিত। এ নামকরণের কারণ হচ্ছে, শাহ ওয়ালি উল্লাহ দেহলবী রহ. এর বংশে এ আমল ৩৩ আয়াতের আমল নামে প্রচলিত ছিল। তবে সুস্পষ্ট যে, আয়াত সংখ্যার ব্যাপারে প্রচলিত মানযিল ও উদ্ধৃত হাদীসের সাথে এর গড়মিল রয়েছে। কারণ, প্রচলিত মানযিলের আয়াত সংখ্যা ৭৯ টি, উদ্ধৃত হাদীসের আয়াত সংখ্যা ৫০টি আর এর আয়াত সংখ্যা ৩৩টি।

শাহ ওয়ালি উল্লাহ দেহলভী রহ. তার পিতার কথা উল্লেখ করে লিখেছেন,

وسمعته (يريد والده) يقول: ثلاث وثلاثون آية تنفع من السحر، وتكون حرزا من اللصوص والسباع، أربع آيات من أول البقرة، وآية الكرسي وآيتان بعدها إلى خالدون، وثلاث من آخر البقرة، وثلاث من الأعراف {إن ربكم الله} إلى {المحسنين}، وآخر بني إسرائيل {قل ادعو الله أو ادعو الرحمن}، وعشر آيات من أول الصافات إلى {لازب}، وآيتان من سورة الرحمن {يا معشر الجن} إلى {تنتصران}، وآخر سورة الحشر {لو أنزلنا هذا القرآن}، وآيتان من {قل أوحي} {وأنه تعالى جد ربنا} إلى {شططا}، فهذه هي الآيات المسميات بثلاث وثلاثين آية

আমি আমার পিতাকে বলতে শুনেছি। তিনি বলেছেন, ৩৩ টি আয়াত এমন আছে যা যাদু-টোনা থেকে রক্ষার ক্ষেত্রে উপকারী এবং চোর-ডাকাত ও হিংস্র প্রাণী থেকে আত্মরক্ষার মাধ্যম। আয়াতগুলো এই,
১. সূরা বাকারার প্রথম ৪ আয়াত
২. আয়াতুল কুরসী ও তার পরবর্তী দুই আয়াত মোট ৩ আয়াত
৩. সূরা বাকারার শেষ ৩ আয়াত
৪. সূরা আরাফ এর ৫৪, ৫৫ ও ৫৬ নং আয়াত, মোট ৩ আয়াত
৫. সূরা ইসরা এর ১১০ নং আয়াত
৬. সূরা সাফ্ফাত এর প্রথম ১০ আয়াত
৭. সূর আর রহমান এর ৩৩ ও ৩৪ নং আয়াত
৮. সূরা হাশর এর শেষ ৪ আয়াত
৯. সূরা জিন এর ৩য় ও ৪র্থ নং আয়াত
এই মোট ৩৩ আয়াত। তবে এরপরেই শাহ ওয়ালি উল্লাহ দেহলভী রহ. লিখেছেন,

وكان سيدي الوالد يزيد عليها الفاتحة، وقل أيها الكافرون، وقل هو الله أحد، والمعوذتين، ويأخذ من سورة قل {أوحي} إلى {شططا}.

তবে আমার সম্মানিত পিতা এর যোগ করতেন, সূরা ফাতিহা (৭ আয়াত), সূরা কাফিরুন (৬ আয়াত), সূরা ইখলাস (৪ আয়াত), সূরা ফালাক (৫ আয়াত), সূরা নাস (৬ আয়াত) ও সূরা জ্বিন থেকে ‘শাত্বাত্বা’ পর্যন্ত (অতিরিক্ত ২ আয়াত)। [প্রাগুক্ত]

অর্থাৎ তিনি এই ৩৩ আয়াতের সাথে আরো ৩০ আয়াত যোগ করে ৬৩ আয়াত পড়তেন।

আয়াত সংখ্যাই যাই হোক, এ আমলের ক্ষেত্রে এটি কোন বিবেচ্য বিষয় নয়; কারণ এ আমল বা এর আয়াত সংখ্যা কোন মানসূস আলাইহ (শরীয়তের পক্ষ থেকে নির্ধারিত) বিষয় নয়। তাই এতে কম-বেশ করার সুযোগ রয়েছে। আল্লাহ তায়ালা আমাদের সঠিক বুঝ দান করুন। আমীন।

(মাওলানা আবু সায়েম দাঃ)
"""
        
        val paragraphs = fullText.split("\n\n")
        
        for (paragraph in paragraphs) {
            val trimmed = paragraph.trim()
            if (trimmed.isEmpty()) continue
            
            // Check if it's a heading
            val isHeading = trimmed.length < 50 && !trimmed.endsWith("।") && !trimmed.endsWith("]") && !trimmed.endsWith("}") && !trimmed.endsWith(")") && !trimmed.endsWith(":") && !trimmed.contains("\n") && !trimmed.contains("এক.") && !trimmed.contains("দুই.") && !trimmed.contains("তিন.")

            if (isHeading) {
                TextWithArabicFont(
                    text = trimmed,
                    fontSize = 18.sp,
                    arabicFontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, top = 14.dp)
                )
            } else {
                // Split sub-lines if the paragraph contains multiple lines
                val lines = trimmed.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                for (line in lines) {
                    val bengaliCharCount = line.count { char -> char.code in 0x0980..0x09FF }
                    val arabicCharCount = line.count { char ->
                        char.code in 0x0600..0x06FF || char.code in 0x0750..0x077F ||
                        char.code in 0x08A0..0x08FF || char.code in 0xFB50..0xFDFF ||
                        char.code in 0xFE70..0xFEFF
                    }
                    val isArabicBlock = bengaliCharCount < 3 && arabicCharCount > 15 && (arabicCharCount.toFloat() / line.length) > 0.5f

                    if (isArabicBlock) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = line,
                                    fontSize = 20.sp,
                                    fontFamily = com.example.ui.theme.meQuranFont,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 2.0.em,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                )
                            }
                        }
                    } else {
                        TextWithArabicFont(
                            text = line,
                            fontSize = 16.sp,
                            arabicFontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 28.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
          
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FontSettingsContent(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val arabicFontName by viewModel.arabicFontName.collectAsState()
    val bengaliFontName by viewModel.bengaliFontName.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val bengaliFontSize by viewModel.bengaliFontSize.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Arabic, 1: Bengali

    val arabicFontOptions = listOf(
        "Scheherazade New" to "আল কালাম কুরআন মাজীদ",
        "PDMS Saleem" to "মলভি এ-এম",
        "Amiri" to "আমিরি",
        "Me Quran" to "মি কুরআন",
        "Noorehira" to "নুরে হুদা",
        "Uthman Taha" to "উছমান তাহা নাসখ",
        "Amiri Quran" to "আমিরি কুরআন",
        "Lateef" to "লতীফ",
        "Almarai" to "আল মারাই",
        "Tajawal" to "তাজাওয়াল"
    )

    val bengaliFontOptions = listOf(
        "SolaimanLipi" to "সলাইমান লিপি",
        "Hind Siliguri" to "হিন্দ শিলিগুড়ি",
        "Shorif Shishir" to "শরীফ শিশির"
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        // 1. Live Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, PrimaryGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E2923) else Color(0xFFF4FBF7)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "কুরআন ও অনুবাদ প্রিভিউ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Arabic Sample
                Text(
                    text = "﴿ رَبَّنَا تَقَبَّلْ مِنَّا إِنَّكَ أَنْتَ السَّمِيعُ الْعَلِيمُ ﴾",
                    fontSize = arabicFontSize.sp,
                    fontFamily = com.example.ui.theme.getArabicFont(arabicFontName),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = (arabicFontSize * 1.5f).sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                HorizontalDivider(
                    color = Border.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Bengali Sample
                Text(
                    text = "পরওয়ারদেগার! আমাদের থেকে কবুল কর। নিশ্চয়ই তুমি শ্রবণকারী, সর্বজ্ঞ।",
                    fontSize = bengaliFontSize.sp,
                    fontFamily = com.example.ui.theme.getBengaliFont(bengaliFontName),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = (bengaliFontSize * 1.4f).sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                // Bengali Tafsir Sample
                Text(
                    text = "কাদের প্রতি আল্লাহর অনুগ্রহ হয়েছে, সে সম্পর্কে সূরা নিসায় এরশাদ হয়েছে, কেউ আল্লাহ ও রাসূলের আনুগত্য করলে সে নবীগণ, সিদ্দীকগণ...",
                    fontSize = (bengaliFontSize * 0.85f).coerceAtLeast(11f).sp,
                    fontFamily = com.example.ui.theme.getBengaliFont(bengaliFontName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = (bengaliFontSize * 1.2f).sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Font Size Adjusters
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Arabic Font Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "আরবি হরফের আকার",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val newSize = (arabicFontSize - 1f).coerceIn(16f, 40f)
                                viewModel.setArabicFontSize(newSize)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = "${arabicFontSize.toInt()}".toBengaliNumerals(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = PrimaryGreen
                        )
                        IconButton(
                            onClick = {
                                val newSize = (arabicFontSize + 1f).coerceIn(16f, 40f)
                                viewModel.setArabicFontSize(newSize)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bengali Font Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "বাংলা হরফের আকার",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val newSize = (bengaliFontSize - 1f).coerceIn(12f, 28f)
                                viewModel.setBengaliFontSize(newSize)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = "${bengaliFontSize.toInt()}".toBengaliNumerals(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = PrimaryGreen
                        )
                        IconButton(
                            onClick = {
                                val newSize = (bengaliFontSize + 1f).coerceIn(12f, 28f)
                                viewModel.setBengaliFontSize(newSize)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // 3. Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        if (selectedTab == 0) PrimaryGreen else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedTab = 0 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "আরবি ফন্ট",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 0) White else MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        if (selectedTab == 1) PrimaryGreen else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedTab = 1 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "বাংলা ফন্ট",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTab == 1) White else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 4. Font Options List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                if (selectedTab == 0) {
                    arabicFontOptions.forEach { (fontKey, label) ->
                        val isSelected = fontKey == arabicFontName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setArabicFontName(fontKey) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setArabicFontName(fontKey) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "وَإِلَٰهُكُمْ إِلَٰهٌ وَٰحِدٌ",
                                fontSize = 20.sp,
                                fontFamily = com.example.ui.theme.getArabicFont(fontKey),
                                color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider(color = Border.copy(alpha = 0.3f))
                    }
                } else {
                    bengaliFontOptions.forEach { (fontKey, label) ->
                        val isSelected = fontKey == bengaliFontName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setBengaliFontName(fontKey) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setBengaliFontName(fontKey) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "পরওয়ারদেগার!",
                                fontSize = 15.sp,
                                fontFamily = com.example.ui.theme.getBengaliFont(fontKey),
                                color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider(color = Border.copy(alpha = 0.3f))
                    }
                }
            }
        }

        // 5. Action Button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text(
                text = "সম্পন্ন",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        }
    }
}
