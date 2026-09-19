package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.utils.DateUtil
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlogPost
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.PostsViewModel
import com.example.utils.NotificationStateHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: PostsViewModel,
    onBackClick: () -> Unit,
    onNavigateToDua: ((Int?) -> Unit)? = null,
    onNavigateToPlanner: (() -> Unit)? = null,
    onNavigateToManzil: (() -> Unit)? = null,
    onNavigateToSubjectwise: (() -> Unit)? = null,
    onNavigateToCalendar: (() -> Unit)? = null,
    onNavigateToDhikrReminder: ((com.example.utils.DhikrType) -> Unit)? = null,
    onNavigateToHijriAdjustment: (() -> Unit)? = null,
    onNavigateToPrayerTimes: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val blogPosts by viewModel.rawBlogPosts.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val pendingPost by viewModel.pendingBlogPost.collectAsState(initial = null)
    
    var selectedPostForReader by remember { mutableStateOf<BlogPost?>(null) }
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var showAddNotificationDialog by remember { mutableStateOf(false) }
    var isAdminUnlocked by remember { mutableStateOf(false) }
    var adminClickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    var readIds by remember { mutableStateOf(NotificationStateHelper.getReadIds(context)) }
    var hiddenIds by remember { mutableStateOf(NotificationStateHelper.getHiddenIds(context)) }
    var forceUpdate by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableStateOf("All") } // "All" or "Unread"
    var expandedGroupAuthor by remember { mutableStateOf<String?>(null) }
    var localNotifications by remember { mutableStateOf(emptyList<BlogPost>()) }

    LaunchedEffect(Unit) {
        val db = com.example.data.local.NotificationDatabase.getDatabase(context)
        try {
            db.localNotificationDao().cleanupDummyNotifications()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        db.localNotificationDao().getAllNotifications().collect { entities ->
            localNotifications = entities.map { entity ->
                BlogPost(
                    id = "local_${entity.id}",
                    title = entity.title,
                    content = entity.content,
                    category = entity.category,
                    author = entity.author,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    val appFirstInstallTime = remember(context) {
        NotificationStateHelper.getAppFirstInstallTime(context)
    }

    LaunchedEffect(forceUpdate) {
        val prefs = context.getSharedPreferences("notification_states", android.content.Context.MODE_PRIVATE)
        readIds = prefs.getStringSet("read_notifications", emptySet())?.toSet() ?: emptySet()
        hiddenIds = prefs.getStringSet("hidden_notifications", emptySet())?.toSet() ?: emptySet()
    }

    // Handle incoming pending notification post
    LaunchedEffect(pendingPost, blogPosts) {
        val currentPending = pendingPost
        if (currentPending != null && (currentPending.category == "নোটিফিকেশন" || currentPending.category == "নোটিশ")) {
            val matchedPost = blogPosts.find { it.id == currentPending.id }
            if (matchedPost != null && matchedPost.content.isNotBlank()) {
                selectedPostForReader = matchedPost
            } else {
                selectedPostForReader = currentPending
            }
            NotificationStateHelper.markAsRead(context, currentPending.id.ifBlank { (currentPending.title + currentPending.content).hashCode().toString() })
            forceUpdate++
            viewModel.setPendingBlogPost(null)
        }
    }

    val notificationPosts = remember(blogPosts, hiddenIds, localNotifications, appFirstInstallTime) {
        val firestoreNotifs = blogPosts.filter {
            (it.category == "নোটিফিকেশন" || it.category == "নোটিশ") &&
            !hiddenIds.contains(it.id.ifBlank { (it.title + it.content).hashCode().toString() }) &&
            (it.timestamp >= appFirstInstallTime)
        }
        val localNotifs = localNotifications.filter {
            !hiddenIds.contains(it.id) &&
            (it.timestamp >= appFirstInstallTime)
        }
        (firestoreNotifs + localNotifs).sortedByDescending { it.timestamp }
    }

    val filteredNotificationPosts = remember(notificationPosts, selectedTab, readIds) {
        if (selectedTab == "Unread") {
            notificationPosts.filter { !readIds.contains(it.id.ifBlank { (it.title + it.content).hashCode().toString() }) }
        } else {
            notificationPosts
        }
    }
    
    // Grouping: Group consecutive unread notifications from the same author if there are more than 1
    val displayItems = remember(filteredNotificationPosts, readIds, expandedGroupAuthor) {
        val result = mutableListOf<Any>()
        var i = 0
        while (i < filteredNotificationPosts.size) {
            val currentPost = filteredNotificationPosts[i]
            val currentAuthor = currentPost.author
            val currentId = currentPost.id.ifBlank { (currentPost.title + currentPost.content).hashCode().toString() }
            val isCurrentRead = readIds.contains(currentId)
            
            // Check if we can group
            if (!isCurrentRead) {
                var j = i + 1
                while (j < filteredNotificationPosts.size) {
                    val nextPost = filteredNotificationPosts[j]
                    val nextId = nextPost.id.ifBlank { (nextPost.title + nextPost.content).hashCode().toString() }
                    if (nextPost.author == currentAuthor && !readIds.contains(nextId)) {
                        j++
                    } else {
                        break
                    }
                }
                
                val groupSize = j - i
                if (groupSize > 1 && expandedGroupAuthor != currentAuthor) {
                    result.add(NotificationGroupHeader(author = currentAuthor, count = groupSize, posts = filteredNotificationPosts.subList(i, j)))
                    i = j
                    continue
                }
            }
            
            result.add(currentPost)
            i++
        }
        result
    }


    if (selectedPostForReader != null) {
        androidx.activity.compose.BackHandler {
            selectedPostForReader = null
        }
        BlogPostDetailScreen(
            post = selectedPostForReader!!,
            onBackClick = { selectedPostForReader = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime < 1500) {
                                adminClickCount++
                            } else {
                                adminClickCount = 1
                            }
                            lastClickTime = now
                            if (adminClickCount >= 3) {
                                adminClickCount = 0
                                showAdminPasswordDialog = true
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "নোটিফিকেশন সেন্টার",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "পিছনে যান"
                        )
                    }
                },
                actions = {
                    if (notificationPosts.isNotEmpty()) {
                        IconButton(onClick = {
                            val allIds = notificationPosts.map { it.id.ifBlank { (it.title + it.content).hashCode().toString() } }
                            NotificationStateHelper.markAllAsRead(context, allIds)
                            forceUpdate++
                            android.widget.Toast.makeText(context, "সব নোটিফিকেশন পড়া হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "সব পড়া হয়েছে",
                                tint = PrimaryGreen
                            )
                        }
                    }
                    if (isAdminUnlocked) {
                        IconButton(onClick = { showAddNotificationDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "নতুন নোটিফিকেশন যোগ করুন",
                                tint = PrimaryGreen
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading && blogPosts.isEmpty() && localNotifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTab == "All",
                            onClick = { selectedTab = "All" },
                            label = { Text("সবগুলো", fontWeight = if (selectedTab == "All") FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryGreen,
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        FilterChip(
                            selected = selectedTab == "Unread",
                            onClick = { selectedTab = "Unread" },
                            label = { Text("আনরিড", fontWeight = if (selectedTab == "Unread") FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryGreen,
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    if (displayItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(PrimaryGreen.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (selectedTab == "Unread") "কোন অপঠিত (আনরিড) নোটিফিকেশন নেই" else "কোন নোটিফিকেশন পাওয়া যায়নি",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedTab == "Unread") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { selectedTab = "All" },
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                                    ) {
                                        Text("সবগুলো নোটিফিকেশন দেখুন", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = displayItems,
                                key = { index, item -> 
                                    if (item is BlogPost) {
                                        if (item.id.isNotEmpty()) item.id else "${item.title}_$index"
                                    } else {
                                        "group_${(item as NotificationGroupHeader).author}_$index"
                                    }
                                }
                            ) { _, item ->
                                if (item is NotificationGroupHeader) {
                                    NotificationGroupCard(
                                        header = item,
                                        onClick = { expandedGroupAuthor = if (expandedGroupAuthor == item.author) null else item.author }
                                    )
                                } else if (item is BlogPost) {
                                    val post = item
                                    val nId = post.id.ifBlank { (post.title + post.content).hashCode().toString() }
                                    val isRead = readIds.contains(nId)
                                    
                                    NotificationCard(
                                        post = post,
                                        isRead = isRead,
                                        onClick = { 
                                            NotificationStateHelper.markAsRead(context, nId)
                                            forceUpdate++
                                            val isSystemNotification = post.id.startsWith("local_")
                                            
                                            if (isSystemNotification) {
                                                val isDuaTarget = post.author == "কুরআনিক দুআ"
                                                val isPlannerTarget = post.author == "কুরআন প্ল্যানার"
                                                val isManzilTarget = post.author == "মানযিল" || post.title.contains("মানযিল")
                                                val isSubjectwiseTarget = post.author == "বিষয়ভিত্তিক কুরআন" || post.title.contains("বিষয়ভিত্তিক")
                                                val isCalendarTarget = post.author == "ক্যালেন্ডার" || post.title.contains("ক্যালেন্ডার")
                                                val isDuroodTarget = post.author == "দরূদ রিমাইন্ডার" || post.title.contains("দরূদ")
                                                val isIstighfarTarget = post.author == "ইস্তেগফার রিমাইন্ডার" || post.title.contains("ইস্তেগফার")
                                                val isHijriAdjustmentTarget = post.author == "হিজরি তারিখ সমন্বয়" || post.title.contains("হিজরি") || post.content.contains("হিজরি তারিখ সমন্বয়")
                                                
                                                val isPrayerTimesTarget = post.author == "নামাজের সময়সূচি" || 
                                                                          post.category == "সালাত রিমাইন্ডার" || 
                                                                          post.author == "সালাত রিমাইন্ডার" ||
                                                                          post.title.contains("ওয়াক্ত") || 
                                                                          post.title.contains("সালাত") || 
                                                                          post.title.contains("নামাজ")
                                                
                                                if (isPrayerTimesTarget && onNavigateToPrayerTimes != null) {
                                                    onNavigateToPrayerTimes()
                                                } else if (isHijriAdjustmentTarget && onNavigateToHijriAdjustment != null) {
                                                    onNavigateToHijriAdjustment()
                                                } else if (isDuaTarget && onNavigateToDua != null) {
                                                    var duaId: Int? = null
                                                    try {
                                                        if (com.example.data.DuaData.richDuas.isEmpty()) {
                                                            com.example.data.DuaData.initialize(context)
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                    val possibleTitle = post.content.lines().firstOrNull()?.trim() ?: ""
                                                    val foundDua = com.example.data.DuaData.richDuas.find { 
                                                        it.title.trim() == possibleTitle || post.content.contains(it.title) || post.title.contains(it.title)
                                                    }
                                                    if (foundDua != null) {
                                                        duaId = foundDua.id
                                                    }
                                                    onNavigateToDua(duaId)
                                                } else if (isPlannerTarget && onNavigateToPlanner != null) {
                                                    onNavigateToPlanner()
                                                } else if (isManzilTarget && onNavigateToManzil != null) {
                                                    onNavigateToManzil()
                                                } else if (isSubjectwiseTarget && onNavigateToSubjectwise != null) {
                                                    onNavigateToSubjectwise()
                                                } else if (isCalendarTarget && onNavigateToCalendar != null) {
                                                    onNavigateToCalendar()
                                                } else if (isDuroodTarget && onNavigateToDhikrReminder != null) {
                                                    onNavigateToDhikrReminder(com.example.utils.DhikrType.DUROOD)
                                                } else if (isIstighfarTarget && onNavigateToDhikrReminder != null) {
                                                    onNavigateToDhikrReminder(com.example.utils.DhikrType.ISTIGHFAR)
                                                } else {
                                                    selectedPostForReader = post 
                                                }
                                            } else {
                                                // Firebase posts always use the notification detail screen
                                                selectedPostForReader = post
                                            }
                                        },
                                        onDeleteClick = {
                                            NotificationStateHelper.hideNotification(context, nId)
                                            forceUpdate++
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdminPasswordDialog) {
        AdminPasswordDialog(
            onSuccess = {
                showAdminPasswordDialog = false
                isAdminUnlocked = true
                showAddNotificationDialog = true
            },
            onDismiss = { showAdminPasswordDialog = false }
        )
    }

    if (showAddNotificationDialog) {
        AddPostDialog(
            viewModel = viewModel,
            defaultCategory = "নোটিফিকেশন",
            onDismiss = { showAddNotificationDialog = false }
        )
    }
}

data class NotificationGroupHeader(
    val author: String,
    val count: Int,
    val posts: List<BlogPost>
)

@Composable
fun NotificationGroupCard(
    header: NotificationGroupHeader,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryGreen.copy(alpha = 0.04f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${header.author} এবং আরও ${DateUtil.toBengaliNumerals(header.count - 1)}টি নতুন নোটিফিকেশন",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "বিস্তারিত দেখতে ট্যাপ করুন",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
