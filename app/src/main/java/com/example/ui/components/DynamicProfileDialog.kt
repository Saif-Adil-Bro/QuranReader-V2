package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.PrimaryGreen
import com.example.utils.DateUtil
import com.example.utils.IslamicBadge
import com.example.utils.UserProfileData
import com.example.utils.UserProfileManager

private val GoldBright = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicProfileDialog(
    profile: UserProfileData,
    isEnglish: Boolean = false,
    bookmarksCount: Int = 0,
    onNavigateToLastRead: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var showGoalPicker by remember { mutableStateOf(false) }
    var showKhatamDialog by remember { mutableStateOf(false) }
    var showManualLogDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    var editName by remember(profile.username) { mutableStateOf(profile.username) }
    var editBio by remember(profile.bio) { mutableStateOf(profile.bio) }
    var selectedAvatarType by remember(profile.avatarType) { mutableStateOf(profile.avatarType) }
    var selectedCustomUri by remember(profile.customAvatarUri) { mutableStateOf(profile.customAvatarUri) }
    var selectedGoalMins by remember(profile.dailyGoalMinutes) { mutableIntStateOf(profile.dailyGoalMinutes) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCustomUri = uri.toString()
            selectedAvatarType = "custom_uri"
        }
    }

    val badges = remember(profile) { UserProfileManager.getBadges(profile) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. HEADER PROFILE CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Box
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF047857), Color(0xFF10B981))
                                )
                            )
                            .border(2.dp, PrimaryGreen.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        RenderAvatarIcon(
                            avatarType = if (isEditing) selectedAvatarType else profile.avatarType,
                            customUri = if (isEditing) selectedCustomUri else profile.customAvatarUri,
                            size = 44.dp
                        )
                    }

                    // Edit camera badge icon
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen)
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable {
                                isEditing = !isEditing
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isEditing) {
                    Text(
                        text = profile.username,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.bio,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    // Editing Form
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(if (isEnglish) "User Name" else "ব্যবহারকারীর নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text(if (isEnglish) "Spiritual Goal / Bio" else "ব্যক্তিগত নিয়ত / বায়ো") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isEnglish) "Choose Avatar:" else "অবতার নির্বাচন করুন:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Avatar Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val avatarPresets = listOf(
                            "preset_mosque" to Icons.Default.Mosque,
                            "preset_quran" to Icons.Default.MenuBook,
                            "preset_kaaba" to Icons.Default.AccountBalance,
                            "preset_crescent" to Icons.Default.Nightlight,
                            "preset_star" to Icons.Default.Star,
                            "preset_heart" to Icons.Default.Favorite
                        )

                        avatarPresets.forEach { (type, icon) ->
                            val isSelected = selectedAvatarType == type
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) GoldBright else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedAvatarType = type
                                        selectedCustomUri = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = type,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Photo Picker Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedAvatarType == "custom_uri") PrimaryGreen else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = if (selectedAvatarType == "custom_uri") 2.dp else 1.dp,
                                    color = if (selectedAvatarType == "custom_uri") GoldBright else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Pick Photo",
                                tint = if (selectedAvatarType == "custom_uri") Color.White else PrimaryGreen,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isEnglish) "Cancel" else "বাতিল")
                        }
                        Button(
                            onClick = {
                                UserProfileManager.updateProfile(
                                    context = context,
                                    username = editName,
                                    bio = editBio,
                                    avatarType = selectedAvatarType,
                                    customAvatarUri = selectedCustomUri,
                                    dailyGoalMinutes = selectedGoalMins
                                )
                                isEditing = false
                                Toast.makeText(context, if (isEnglish) "Profile updated" else "প্রোফাইল সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text(if (isEnglish) "Save" else "সংরক্ষণ", color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. DAILY STREAK & GOAL CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.15f))
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "${profile.streakDays} Days Streak" else "${DateUtil.toBengaliNumerals(profile.streakDays)} দিনের স্ট্রিক",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnglish) "Active daily recitation" else "দৈনিক নিয়মিত তিলাওয়াত",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Goal status chip with click to change target
                    val isGoalAchieved = profile.todayReadingMinutes >= profile.dailyGoalMinutes && profile.dailyGoalMinutes > 0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGoalAchieved) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isGoalAchieved) Color(0xFF10B981) else Color(0xFFF59E0B)),
                        modifier = Modifier.clickable { showGoalPicker = !showGoalPicker }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isGoalAchieved) {
                                    if (isEnglish) "Achieved 🎉" else "অর্জিত 🎉"
                                } else {
                                    val remaining = (profile.dailyGoalMinutes - profile.todayReadingMinutes).coerceAtLeast(0)
                                    if (isEnglish) "$remaining min left" else "${DateUtil.toBengaliNumerals(remaining)} মি. বাকি"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGoalAchieved) Color(0xFF059669) else Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Change Goal",
                                tint = if (isGoalAchieved) Color(0xFF059669) else Color(0xFFD97706),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                val progressFraction = if (profile.dailyGoalMinutes > 0) {
                    (profile.todayReadingMinutes.toFloat() / profile.dailyGoalMinutes).coerceIn(0f, 1f)
                } else 1f

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEnglish) "Today: ${profile.todayReadingMinutes} mins" else "আজকে পড়া: ${DateUtil.toBengaliNumerals(profile.todayReadingMinutes)} মিনিট",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isEnglish) "Target: ${profile.dailyGoalMinutes} mins/day" else "লক্ষ্য: ${DateUtil.toBengaliNumerals(profile.dailyGoalMinutes)} মি./দিন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Dynamic Goal Picker Chips
                AnimatedVisibility(visible = showGoalPicker) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Select Daily Target:" else "দৈনিক লক্ষ্যমাত্রা নির্ধারণ করুন:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(10, 15, 20, 30, 45, 60).forEach { mins ->
                                val isSelected = profile.dailyGoalMinutes == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        UserProfileManager.setDailyGoal(context, mins)
                                        showGoalPicker = false
                                        Toast.makeText(context, if (isEnglish) "Goal set to $mins mins/day" else "দৈনিক লক্ষ্য $mins মিনিট নির্ধারণ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text(if (isEnglish) "${mins}m" else "${DateUtil.toBengaliNumerals(mins)}মি", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. FOUR-GRID STATS OVERVIEW ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Schedule,
                iconTint = Color(0xFF3B82F6),
                title = if (isEnglish) "Total Reading" else "মোট তিলাওয়াত",
                value = formatReadingTime(profile.totalReadingMinutes, isEnglish)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MenuBook,
                iconTint = Color(0xFF10B981),
                title = if (isEnglish) "Verses Read" else "পঠিত আয়াত",
                value = if (isEnglish) "${profile.totalAyahsRead}" else "${DateUtil.toBengaliNumerals(profile.totalAyahsRead)}"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Bookmark,
                iconTint = Color(0xFFEF4444),
                title = if (isEnglish) "Bookmarks" else "বুকমার্ক সংরক্ষিত",
                value = if (isEnglish) "$bookmarksCount" else "${DateUtil.toBengaliNumerals(bookmarksCount)}"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                iconTint = GoldBright,
                title = if (isEnglish) "Badges Earned" else "অর্জিত ব্যাজ",
                value = "${badges.count { it.isUnlocked }}/${badges.size}"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. KHATAM PROGRESS TRACKER CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.3f))
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GoldBright.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Khatam",
                                tint = GoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Quran Khatam Tracker" else "কুরআন খতম ট্র্যাকার",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val khatamPercent = (profile.khatamCompletedPages * 100f / 604f).coerceIn(0f, 100f)
                            Text(
                                text = if (isEnglish) "${String.format("%.1f", khatamPercent)}% Completed" else "${DateUtil.toBengaliNumerals(String.format("%.1f", khatamPercent))}% সম্পন্ন",
                                fontSize = 11.5.sp,
                                color = GoldBright,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Quick Log Pages button
                    OutlinedButton(
                        onClick = { showKhatamDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = GoldBright, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEnglish) "Update" else "আপডেট", fontSize = 11.sp, color = GoldBright)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val khatamFraction = (profile.khatamCompletedPages / 604f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { khatamFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GoldBright,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEnglish) "Read: ${profile.khatamCompletedPages} / 604 Pages" else "পড়া হয়েছে: ${DateUtil.toBengaliNumerals(profile.khatamCompletedPages)} / ৬০৪ পৃষ্ঠা",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val remainingPages = (604 - profile.khatamCompletedPages).coerceAtLeast(0)
                    Text(
                        text = if (isEnglish) "$remainingPages pages left" else "${DateUtil.toBengaliNumerals(remainingPages)} পৃষ্ঠা বাকি",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Increment Buttons for Khatam
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            UserProfileManager.updateKhatamProgress(context, profile.khatamCompletedPages + 1)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(if (isEnglish) "+1 Page" else "+১ পৃষ্ঠা", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = {
                            UserProfileManager.updateKhatamProgress(context, profile.khatamCompletedPages + 5)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(if (isEnglish) "+5 Pages" else "+৫ পৃষ্ঠা", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = {
                            UserProfileManager.updateKhatamProgress(context, profile.khatamCompletedPages + 20)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(if (isEnglish) "+1 Para (20p)" else "+১ পারা", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 5. LAST READ RESUME CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onNavigateToLastRead()
                    onDismiss()
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = PrimaryGreen.copy(alpha = 0.08f)
            ),
            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(PrimaryGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEnglish) "Continue Reading" else "সর্বশেষ পঠিত স্থান",
                            fontSize = 11.5.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEnglish) "Surah ${profile.lastReadSurahName} • Ayah ${profile.lastReadAyah}" else "সূরা ${profile.lastReadSurahName} • আয়াত ${DateUtil.toBengaliNumerals(profile.lastReadAyah)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = PrimaryGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 6. ISLAMIC ACHIEVEMENTS & BADGES ---
        Text(
            text = if (isEnglish) "Achievements & Spiritual Badges" else "ইসলামিক অর্জন ও মাইলফলক ব্যাজ",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        badges.forEach { badge ->
            BadgeItemCard(badge = badge, isEnglish = isEnglish)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 7. MANUAL OFFLINE READING LOGGER ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEnglish) "Log Printed Quran Reading:" else "মুসহাফ/বই পড়ার সময় যোগ করুন:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEnglish) "Track reading done outside the app" else "অ্যাপের বাইরে সরাসরি কুরআন পাঠের হিসাব রাখুন",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showManualLogDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add custom time", tint = PrimaryGreen)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            UserProfileManager.addManualReadingMinutes(context, 15)
                            Toast.makeText(context, if (isEnglish) "+15 minutes logged" else "+১৫ মিনিট যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("+15m", fontSize = 12.sp, color = Color.White)
                    }
                    Button(
                        onClick = {
                            UserProfileManager.addManualReadingMinutes(context, 30)
                            Toast.makeText(context, if (isEnglish) "+30 minutes logged" else "+৩০ মিনিট যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("+30m", fontSize = 12.sp, color = Color.White)
                    }
                    Button(
                        onClick = {
                            UserProfileManager.addManualReadingMinutes(context, 60)
                            Toast.makeText(context, if (isEnglish) "+1 hour logged" else "+১ ঘণ্টা যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("+1h", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reset Profile Stats button
        TextButton(
            onClick = { showResetConfirmDialog = true }
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isEnglish) "Reset Profile Stats" else "পরিসংখ্যান রিসেট করুন",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
    }

    // --- KHATAM UPDATE DIALOG ---
    if (showKhatamDialog) {
        var pageInput by remember { mutableStateOf(profile.khatamCompletedPages.toString()) }
        AlertDialog(
            onDismissRequest = { showKhatamDialog = false },
            title = { Text(if (isEnglish) "Update Khatam Progress" else "খতম অগ্রগতি আপডেট করুন") },
            text = {
                Column {
                    Text(
                        text = if (isEnglish) "Enter completed page number (1 to 604):" else "পঠিত শেষ পৃষ্ঠা নম্বর লিখুন (১ হতে ৬০৪):",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = pageInput.toIntOrNull() ?: profile.khatamCompletedPages
                        UserProfileManager.updateKhatamProgress(context, num)
                        showKhatamDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(if (isEnglish) "Save" else "সংরক্ষণ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKhatamDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }

    // --- MANUAL LOG DIALOG ---
    if (showManualLogDialog) {
        var minsInput by remember { mutableStateOf("15") }
        var ayahsInput by remember { mutableStateOf("0") }
        var pagesInput by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showManualLogDialog = false },
            title = { Text(if (isEnglish) "Log Reading Session" else "কুরআন পড়ার হিসাব যোগ করুন") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = minsInput,
                        onValueChange = { minsInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isEnglish) "Minutes Read" else "পড়ার সময় (মিনিট)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ayahsInput,
                        onValueChange = { ayahsInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isEnglish) "Ayahs Read (Optional)" else "পঠিত আয়াত সংখ্যা (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pagesInput,
                        onValueChange = { pagesInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isEnglish) "Pages Read (Optional)" else "পঠিত পৃষ্ঠা সংখ্যা (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = minsInput.toIntOrNull() ?: 0
                        val ayahs = ayahsInput.toIntOrNull() ?: 0
                        val pages = pagesInput.toIntOrNull() ?: 0
                        if (mins > 0 || ayahs > 0 || pages > 0) {
                            UserProfileManager.addManualReadingMinutes(context, mins, ayahs, pages)
                            Toast.makeText(context, if (isEnglish) "Session logged" else "সেশন যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                        showManualLogDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(if (isEnglish) "Add" else "যোগ করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualLogDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }

    // --- RESET STATS CONFIRMATION ---
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(if (isEnglish) "Reset Profile Stats?" else "পরিসংখ্যান রিসেট করতে চান?") },
            text = {
                Text(
                    text = if (isEnglish) "This will reset reading minutes, streak, and completed pages back to zero. Your profile name and bio will remain unchanged." else "এটি পড়ার সময়, স্ট্রিক এবং খতম পৃষ্ঠা ০ করে দেবে। আপনার নাম ও বায়ো অপরিবর্তিত থাকবে।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        UserProfileManager.resetAllStats(context)
                        showResetConfirmDialog = false
                        Toast.makeText(context, if (isEnglish) "Stats reset" else "পরিসংখ্যান রিসেট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isEnglish) "Reset" else "রিসেট করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }
}

@Composable
fun RenderAvatarIcon(avatarType: String, customUri: String?, size: androidx.compose.ui.unit.Dp) {
    if (avatarType == "custom_uri" && !customUri.isNullOrBlank()) {
        AsyncImage(
            model = customUri,
            contentDescription = "User Avatar",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        val icon = when (avatarType) {
            "preset_quran" -> Icons.Default.MenuBook
            "preset_kaaba" -> Icons.Default.AccountBalance
            "preset_crescent" -> Icons.Default.Nightlight
            "preset_star" -> Icons.Default.Star
            "preset_heart" -> Icons.Default.Favorite
            else -> Icons.Default.Mosque
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BadgeItemCard(badge: IslamicBadge, isEnglish: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) PrimaryGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (badge.isUnlocked) PrimaryGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (badge.isUnlocked) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (badge.iconName) {
                        "fire" -> Icons.Default.LocalFireDepartment
                        "trophy" -> Icons.Default.EmojiEvents
                        "crown" -> Icons.Default.WorkspacePremium
                        "book" -> Icons.Default.MenuBook
                        "clock" -> Icons.Default.Schedule
                        "target" -> Icons.Default.TrackChanges
                        "library_books" -> Icons.Default.LibraryBooks
                        "workspace_premium" -> Icons.Default.WorkspacePremium
                        else -> Icons.Default.Stars
                    },
                    contentDescription = null,
                    tint = if (badge.isUnlocked) GoldBright else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnglish) badge.titleEn else badge.titleBn,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = if (isEnglish) badge.descEn else badge.descBn,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!badge.isUnlocked && badge.progress > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { badge.progress },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PrimaryGreen.copy(alpha = 0.7f),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            if (badge.isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isEnglish) "Unlocked" else "অর্জিত ✓",
                        color = PrimaryGreen,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatReadingTime(mins: Int, isEnglish: Boolean): String {
    return if (mins >= 60) {
        val hrs = mins / 60
        val rem = mins % 60
        if (isEnglish) {
            if (rem > 0) "$hrs hr $rem min" else "$hrs hr"
        } else {
            if (rem > 0) "${DateUtil.toBengaliNumerals(hrs)} ঘণ্টা ${DateUtil.toBengaliNumerals(rem)} মি." else "${DateUtil.toBengaliNumerals(hrs)} ঘণ্টা"
        }
    } else {
        if (isEnglish) "$mins min" else "${DateUtil.toBengaliNumerals(mins)} মিনিট"
    }
}
