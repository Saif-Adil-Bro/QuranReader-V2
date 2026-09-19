package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.example.utils.DateUtil

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.security.MessageDigest
import com.example.data.model.BlogPost
import com.example.data.model.ShortPost
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.PostsViewModel
import com.example.utils.PostShareUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(
    viewModel: PostsViewModel,
    onBackClick: () -> Unit,
    onNavigateToNotifications: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val blogPosts by viewModel.filteredBlogPosts.collectAsState()
    val shortPosts by viewModel.filteredShortPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val pendingCardPost by viewModel.pendingPhotoCardPost.collectAsState()
    val pendingBlogPost by viewModel.pendingBlogPost.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    val pullOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 64.dp.toPx() }
    val holdOffsetPx = with(density) { 50.dp.toPx() }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullOffset.animateTo(holdOffsetPx, spring(dampingRatio = 0.8f))
        } else {
            pullOffset.animateTo(0f, spring(dampingRatio = 0.8f))
        }
    }

    val nestedScrollConnection = remember(thresholdPx, isRefreshing) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0) {
                    val newOffset = (pullOffset.value + available.y * 0.45f).coerceIn(0f, thresholdPx * 1.3f)
                    val delta = newOffset - pullOffset.value
                    coroutineScope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0 && pullOffset.value > 0f) {
                    val newOffset = (pullOffset.value + available.y).coerceAtLeast(0f)
                    val delta = newOffset - pullOffset.value
                    coroutineScope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset.value >= thresholdPx && !isRefreshing) {
                    isRefreshing = true
                    viewModel.refresh {
                        isRefreshing = false
                        Toast.makeText(context, "পোস্টগুলো আপডেট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                } else if (!isRefreshing) {
                    coroutineScope.launch {
                        pullOffset.animateTo(0f, spring(dampingRatio = 0.8f))
                    }
                }
                return Velocity.Zero
            }
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedBlogPostForReader by remember { mutableStateOf<BlogPost?>(null) }
    var selectedShortPostForCard by remember { mutableStateOf<ShortPost?>(null) }

    LaunchedEffect(pendingCardPost) {
        pendingCardPost?.let { post ->
            selectedBlogPostForReader = null
            selectedTabIndex = 1
            selectedShortPostForCard = post
            viewModel.setPendingPhotoCardPost(null)
        }
    }

    LaunchedEffect(pendingBlogPost) {
        pendingBlogPost?.let { post ->
            selectedShortPostForCard = null
            selectedTabIndex = 0
            selectedBlogPostForReader = post
            viewModel.setPendingBlogPost(null)
        }
    }

    LaunchedEffect(blogPosts) {
        selectedBlogPostForReader?.let { current ->
            if (blogPosts.none { it.id == current.id || (it.title.trim() == current.title.trim() && it.content.trim() == current.content.trim()) }) {
                selectedBlogPostForReader = null
            }
        }
    }

    LaunchedEffect(shortPosts) {
        selectedShortPostForCard?.let { current ->
            if (shortPosts.none { it.id == current.id || (it.text.trim() == current.text.trim() && it.reference.trim() == current.reference.trim()) }) {
                selectedShortPostForCard = null
            }
        }
    }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAddPostDialog by remember { mutableStateOf(false) }
    var adminClickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    val rawBlogPosts by viewModel.rawBlogPosts.collectAsState(initial = emptyList())
    var notifReadIds by remember { mutableStateOf(com.example.utils.NotificationStateHelper.getReadIds(context)) }
    var notifHiddenIds by remember { mutableStateOf(com.example.utils.NotificationStateHelper.getHiddenIds(context)) }
    var localNotifsList by remember { mutableStateOf(emptyList<BlogPost>()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        val db = com.example.data.local.NotificationDatabase.getDatabase(context)
        db.localNotificationDao().getAllNotifications().collect { entities ->
            localNotifsList = entities.map { entity ->
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

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                notifReadIds = com.example.utils.NotificationStateHelper.getReadIds(context)
                notifHiddenIds = com.example.utils.NotificationStateHelper.getHiddenIds(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val appFirstInstallTime = remember(context) { com.example.utils.NotificationStateHelper.getAppFirstInstallTime(context) }

    val unreadNotificationCount = remember(rawBlogPosts, localNotifsList, notifReadIds, notifHiddenIds, appFirstInstallTime) {
        val firestoreNotifs = rawBlogPosts.filter { 
            (it.category == "নোটিফিকেশন" || it.category == "নোটিশ") && 
            !notifHiddenIds.contains(it.id.ifBlank { (it.title + it.content).hashCode().toString() }) &&
            (it.timestamp >= appFirstInstallTime)
        }
        val localNotifs = localNotifsList.filter { 
            !notifHiddenIds.contains(it.id) &&
            (it.timestamp >= appFirstInstallTime)
        }
        val allNotifs = firestoreNotifs + localNotifs
        allNotifs.count { post ->
            val nId = post.id.ifBlank { (post.title + post.content).hashCode().toString() }
            !notifReadIds.contains(nId)
        }
    }

    val categories = listOf("সকল", "কুরআন ও জীবন", "নফল ইবাদত", "দৈনিক নসীহত", "মাসনুন জিকির", "আত্মশুদ্ধি", "সাধারণ")

    val regularBlogPosts = remember(blogPosts) { blogPosts.filter { it.category != "নোটিফিকেশন" && it.category != "নোটিশ" } }

    if (selectedBlogPostForReader != null) {
        BackHandler {
            selectedBlogPostForReader = null
        }
        BlogPostDetailScreen(
            post = selectedBlogPostForReader!!,
            onBackClick = { selectedBlogPostForReader = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ইসলামিক পোস্ট ও ফটো কার্ড",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        val now = System.currentTimeMillis()
                                        if (now - lastClickTime < 1000) {
                                            adminClickCount++
                                        } else {
                                            adminClickCount = 1
                                        }
                                        lastClickTime = now

                                        if (adminClickCount >= 5) {
                                            adminClickCount = 0
                                            showPasswordDialog = true
                                        }
                                    }
                                )
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigateToNotifications?.invoke() }) {
                            if (unreadNotificationCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = PrimaryGreen) {
                                            Text(
                                                text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "নোটিফিকেশন সেন্টার",
                                        tint = PrimaryGreen
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "নোটিফিকেশন সেন্টার",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("কীওয়ার্ড বা বিষয় দিয়ে খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                // Category Chips Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedCategory.value = category },
                            label = { Text(category, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PrimaryGreen
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ব্লগ (${regularBlogPosts.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ফটো কার্ড ও নসীহত (${shortPosts.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    )
                }

                val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
                val spinningAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(750, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "spin_angle"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    if (pullOffset.value > 0f || isRefreshing) {
                        val currentDp = with(density) { pullOffset.value.toDp() }
                        val isDeepEnough = pullOffset.value >= thresholdPx

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (currentDp * 0.7f).coerceAtMost(52.dp))
                                .zIndex(10f)
                                .size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (isRefreshing) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "রিফ্রেশ হচ্ছে",
                                        tint = PrimaryGreen,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .rotate(spinningAngle)
                                    )
                                } else {
                                    val pullProgress = (pullOffset.value / thresholdPx).coerceIn(0f, 1f)
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "রিফ্রেশ করতে টানুন",
                                        tint = if (isDeepEnough) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .rotate(pullProgress * 180f)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = with(density) { pullOffset.value.toDp() })
                    ) {
                        if (isLoading && blogPosts.isEmpty() && shortPosts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryGreen)
                            }
                        } else {
                            when (selectedTabIndex) {
                                0 -> {
                                    // Blog List
                                    if (regularBlogPosts.isEmpty()) {
                                        EmptyStateView("কোন ইসলামিক ব্লগ পোস্ট পাওয়া যায়নি")
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(regularBlogPosts, key = { index, post -> if (post.id.isNotEmpty()) post.id else "${post.title}_$index" }) { _, post ->
                                                BlogPostCard(
                                                    post = post,
                                                    onClick = { selectedBlogPostForReader = post }
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // Short Posts List
                                    if (shortPosts.isEmpty()) {
                                        EmptyStateView("কোন সংক্ষিপ্ত নসীহত পাওয়া যায়নি")
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            itemsIndexed(shortPosts, key = { index, post -> if (post.id.isNotEmpty()) post.id else "${post.text}_$index" }) { _, post ->
                                                ShortPostCard(
                                                    post = post,
                                                    onCopyClick = { PostShareUtil.copyToClipboard(context, post) },
                                                    onTextShareClick = { PostShareUtil.shareAsText(context, post) },
                                                    onPhotoCardClick = { selectedShortPostForCard = post }
                                                )
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

        // Custom Photo Card Generator BottomSheet / Dialog
        if (selectedShortPostForCard != null) {
            PhotoCardCustomizerDialog(
                post = selectedShortPostForCard!!,
                onDismiss = { selectedShortPostForCard = null }
            )
        }

        // Admin Secret Password Dialog
        if (showPasswordDialog) {
            AdminPasswordDialog(
                onSuccess = {
                    showPasswordDialog = false
                    showAddPostDialog = true
                },
                onDismiss = { showPasswordDialog = false }
            )
        }

        // Admin Secret Add Post Dialog
        if (showAddPostDialog) {
            AddPostDialog(
                viewModel = viewModel,
                onDismiss = { showAddPostDialog = false }
            )
        }
    }
}

fun formatPostDate(timestamp: Long): String {
    if (timestamp <= 0L) return "সম্প্রতি"
    val diffMillis = System.currentTimeMillis() - timestamp
    if (diffMillis < 0) return "সম্প্রতি"
    val seconds = diffMillis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    fun String.toBanglaDigits(): String {
        val banglaDigits = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        return this.map { banglaDigits[it] ?: it }.joinToString("")
    }

    return when {
        minutes < 1 -> "এখনই"
        minutes < 60 -> "${minutes.toString().toBanglaDigits()} মিনিট আগে"
        hours < 24 -> "${hours.toString().toBanglaDigits()} ঘণ্টা আগে"
        days < 7 -> "${days.toString().toBanglaDigits()} দিন আগে"
        else -> {
            try {
                val sdf = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("bn", "BD"))
                sdf.format(java.util.Date(timestamp))
            } catch (e: Exception) {
                "সম্প্রতি"
            }
        }
    }
}

@Composable
fun BlogPostCard(
    post: BlogPost,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = post.category,
                        color = PrimaryGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = getRelativeTimeBengali(post.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = post.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.author,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "সম্পূর্ণ পড়ুন",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShortPostCard(
    post: ShortPost,
    onCopyClick: () -> Unit,
    onTextShareClick: () -> Unit,
    onPhotoCardClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Surface(
                    color = Color(0xFF0EA5E9).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = post.category,
                        color = Color(0xFF0284C7),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (post.reference.isNotEmpty()) {
                    Text(
                        text = post.reference,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onCopyClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onTextShareClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share Text", modifier = Modifier.size(18.dp))
                    }
                }

                Button(
                    onClick = onPhotoCardClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ফটো কার্ড শেয়ার", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogPostDetailScreen(
    post: BlogPost,
    onBackClick: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    var textSizeSp by remember { mutableFloatStateOf(16f) }
    val sharedPrefs = remember(context) { context.getSharedPreferences("quran_menu_prefs", android.content.Context.MODE_PRIVATE) }
    val hijriOffset = sharedPrefs.getInt("hijri_offset", 0)

    var fullPostContent by remember(post.id, post.content) { mutableStateOf(post.content) }
    var fullPostTitle by remember(post.id, post.title) { mutableStateOf(post.title) }
    var fullPostAuthor by remember(post.id, post.author) { mutableStateOf(post.author) }

    LaunchedEffect(post.id) {
        if (post.id.isNotBlank() && !post.id.startsWith("local_")) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("notifications").document(post.id).get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val c = doc.getString("content") ?: doc.getString("text") ?: doc.getString("body") ?: ""
                            val t = doc.getString("title") ?: doc.getString("name") ?: ""
                            val a = doc.getString("author") ?: doc.getString("writer") ?: ""
                            if (c.isNotBlank()) fullPostContent = c
                            if (t.isNotBlank()) fullPostTitle = t
                            if (a.isNotBlank()) fullPostAuthor = a
                        } else {
                            db.collection("blog_posts").document(post.id).get()
                                .addOnSuccessListener { doc2 ->
                                    if (doc2 != null && doc2.exists()) {
                                        val c = doc2.getString("content") ?: doc2.getString("text") ?: doc2.getString("body") ?: ""
                                        val t = doc2.getString("title") ?: doc2.getString("name") ?: ""
                                        val a = doc2.getString("author") ?: doc2.getString("writer") ?: ""
                                        if (c.isNotBlank()) fullPostContent = c
                                        if (t.isNotBlank()) fullPostTitle = t
                                        if (a.isNotBlank()) fullPostAuthor = a
                                    }
                                }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val displayContent = remember(fullPostContent, post.id, hijriOffset) {
        if (post.id == com.example.utils.MoonSightingNotificationHelper.TARGET_POST_ID || fullPostContent.contains("চাঁদ অনুসন্ধানের জন্য")) {
            com.example.utils.MoonSightingNotificationHelper.formatDynamicMoonSightingContent(fullPostContent, hijriOffset)
        } else {
            fullPostContent
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val headerTitle = when {
                        post.category == "নোটিফিকেশন" || post.category == "নোটিশ" || post.category.contains("Notification", ignoreCase = true) -> "নোটিফিকেশন"
                        post.category.isNotBlank() -> post.category
                        else -> "ব্লগ পোস্ট"
                    }
                    Text(
                        text = headerTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে যান")
                    }
                },
                actions = {
                    IconButton(onClick = { if (textSizeSp > 12f) textSizeSp -= 2f }) {
                        Text("A-", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryGreen)
                    }
                    IconButton(onClick = { if (textSizeSp < 28f) textSizeSp += 2f }) {
                        Text("A+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryGreen)
                    }
                    IconButton(
                        onClick = {
                            val shareText = "✨ $fullPostTitle \n\n$displayContent\n\n— $fullPostAuthor\n\n📱 ❝কুরআন রিডার❞ অ্যাপ থেকে"
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "ব্লগ পোস্ট শেয়ার করুন"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "শেয়ার", tint = PrimaryGreen)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Category Badge & Read Time
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
                        text = post.category,
                        color = PrimaryGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = getRelativeTimeBengali(post.timestamp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Author Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = fullPostAuthor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "প্রকাশিত • ইসলামিক নসীহত ও ব্লগ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            var showFullScreenImage by remember { mutableStateOf(false) }

            // Post Title with Selection
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = fullPostTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 30.sp
                )
            }

            if (post.imageUrl.trim().isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showFullScreenImage = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = post.imageUrl.trim(),
                            contentDescription = post.title,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
                    }
                }
            }

            if (showFullScreenImage && post.imageUrl.trim().isNotBlank()) {
                Dialog(
                    onDismissRequest = { showFullScreenImage = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f))
                            .clickable { showFullScreenImage = false }
                    ) {
                        AsyncImage(
                            model = post.imageUrl.trim(),
                            contentDescription = post.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                        IconButton(
                            onClick = { showFullScreenImage = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "বন্ধ করুন",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(16.dp))

            // Body Content with Text Selection and Clickable Links
            androidx.compose.foundation.text.selection.SelectionContainer {
                com.example.ui.components.FormattedIslamicText(
                    text = displayContent,
                    baseFontSize = textSizeSp.sp,
                    baseColor = MaterialTheme.colorScheme.onSurface,
                    arabicColor = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCardCustomizerDialog(
    post: ShortPost,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val (initialCategory, initialTemplate) = remember(post) {
        PostShareUtil.findBestTemplateForPost(post)
    }

    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedTemplate by remember { mutableStateOf(initialTemplate) }
    
    var bgImageUrl by remember { mutableStateOf("") }
    var aspectRatio by remember { mutableStateOf("1:1") }
    var overlayAlpha by remember { mutableFloatStateOf(selectedTemplate.defaultOverlayAlpha) }
    var textAlignName by remember { mutableStateOf(selectedTemplate.defaultTextAlign) }
    var fontName by remember { mutableStateOf(selectedTemplate.defaultFontName) }
    var fontSizeSp by remember { mutableFloatStateOf(selectedTemplate.defaultFontSize) }
    var lineSpacingMult by remember { mutableFloatStateOf(selectedTemplate.defaultLineSpacing) }
    var textLetterSpacing by remember { mutableFloatStateOf(0f) }
    var textWidthPercent by remember { mutableFloatStateOf(1f) }
    var isTextBold by remember { mutableStateOf(false) }
    var autoFitText by remember { mutableStateOf(true) }
    var customTitleColor by remember { mutableStateOf<String?>(null) }
    var customTextColor by remember { mutableStateOf<String?>(null) }
    var customOverlayColor by remember { mutableStateOf<String?>("#000000") }

    LaunchedEffect(bgImageUrl) {
        if (bgImageUrl.isNotEmpty()) {
            overlayAlpha = 0.85f
            customOverlayColor = "#000000"
        }
    }
    var customRefColor by remember { mutableStateOf<String?>(null) }

    
    LaunchedEffect(selectedTemplate) {
        overlayAlpha = selectedTemplate.defaultOverlayAlpha
        textAlignName = selectedTemplate.defaultTextAlign
        fontName = selectedTemplate.defaultFontName
        fontSizeSp = selectedTemplate.defaultFontSize
        customTitleColor = null
        customTextColor = null
        customRefColor = null
        textLetterSpacing = 0f
        textWidthPercent = 1f
        isTextBold = false

        lineSpacingMult = selectedTemplate.defaultLineSpacing
        // We do not overwrite user's customText/customCategory here intentionally, just the visual style
    }
    
    var customCategory by remember { mutableStateOf(post.category) }
    var customText by remember { mutableStateOf(post.text) }
    var customRef by remember { mutableStateOf(post.reference) }
    var showLogo by remember { mutableStateOf(selectedTemplate.showLogo) }
    var showWatermark by remember { mutableStateOf(selectedTemplate.showWatermark) }

    var cardBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isTemplateExpanded by remember { mutableStateOf(true) }
    var isContentExpanded by remember { mutableStateOf(true) }
    var isBackgroundExpanded by remember { mutableStateOf(false) }
    var isTypographyExpanded by remember { mutableStateOf(false) }
    var isBrandingExpanded by remember { mutableStateOf(false) }
    var isLayoutExpanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            bgImageUrl = uri.toString()
        }
    }

    var customFontName by remember { mutableStateOf<String?>(null) }
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fontName = uri.toString()
            customFontName = "কাস্টম ফন্ট (.ttf/.otf)"
        }
    }

    // Sample background image presets
    val bgCategories = listOf("সব", "মসজিদ", "প্রকৃতি", "রাত", "কুরআন", "Abstract", "রমজান", "ইসলামিক")
    var selectedBgCategory by remember { mutableStateOf("সব") }
    
    data class PresetBg(val url: String, val category: String)
    val presetBgList = remember {
        listOf(
            PresetBg("https://images.unsplash.com/photo-1542816417-0983cbe33577?w=600&q=80", "মসজিদ"),
            PresetBg("https://images.unsplash.com/photo-1564769625905-50e93615e769?w=600&q=80", "মসজিদ"),
            PresetBg("https://images.unsplash.com/photo-1519817650390-64a93db51149?w=600&q=80", "মসজিদ"),
            PresetBg("https://images.unsplash.com/photo-1584551246679-0daf3d275d0f?w=600&q=80", "মসজিদ"),
            PresetBg("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&q=80", "প্রকৃতি"),
            PresetBg("https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=600&q=80", "প্রকৃতি"),
            PresetBg("https://images.unsplash.com/photo-1444464666168-49b626d49c97?w=600&q=80", "প্রকৃতি"),
            PresetBg("https://images.unsplash.com/photo-1426604966848-d7adac402bff?w=600&q=80", "প্রকৃতি"),
            PresetBg("https://images.unsplash.com/photo-1505322022379-7c3353ee6291?w=600&q=80", "রাত"),
            PresetBg("https://images.unsplash.com/photo-1488866022504-f2584929ca5f?w=600&q=80", "রাত"),
            PresetBg("https://images.unsplash.com/photo-1503264116251-35a269479413?w=600&q=80", "রাত"),
            PresetBg("https://images.unsplash.com/photo-1609599006353-e629aaab31f5?w=600&q=80", "কুরআন"),
            PresetBg("https://images.unsplash.com/photo-1576485290814-1c72aa4bbb8e?w=600&q=80", "কুরআন"),
            PresetBg("https://images.unsplash.com/photo-1509021436468-d51030005963?w=600&q=80", "Abstract"),
            PresetBg("https://images.unsplash.com/photo-1604871000636-074fa5117945?w=600&q=80", "Abstract"),
            PresetBg("https://images.unsplash.com/photo-1557672172-298e090bd0f1?w=600&q=80", "Abstract"),
            PresetBg("https://images.unsplash.com/photo-1585036156171-384164a8c675?w=600&q=80", "রমজান"),
            PresetBg("https://images.unsplash.com/photo-1555068228-4b71ab2ab1b5?w=600&q=80", "রমজান"),
            PresetBg("https://images.unsplash.com/photo-1563852028710-184518485244?w=600&q=80", "ইসলামিক"),
            PresetBg("https://images.unsplash.com/photo-1582281171801-62a26563deec?w=600&q=80", "ইসলামিক"),
            PresetBg("https://images.unsplash.com/photo-1558231908-1647ecb6243b?w=600&q=80", "ইসলামিক")
        )
    }

    LaunchedEffect(
        selectedTemplate, bgImageUrl, overlayAlpha, customOverlayColor, aspectRatio, textAlignName, fontName, fontSizeSp, lineSpacingMult, customCategory, customText, customRef, customTitleColor, customTextColor, customRefColor, showLogo, showWatermark, autoFitText, textLetterSpacing, textWidthPercent, isTextBold, post
    ) {
        kotlinx.coroutines.delay(20)
        isGeneratingPreview = true
        cardBitmap = PostShareUtil.generateCardBitmap(
            context = context,
            customTitleColor = customTitleColor,
            customTextColor = customTextColor,
            customRefColor = customRefColor,
            post = post,
            template = selectedTemplate,
            bgImageUrl = bgImageUrl.ifBlank { null },
            aspectRatio = aspectRatio,
            overlayAlpha = overlayAlpha,
            customOverlayColor = customOverlayColor,
            textAlignName = textAlignName,
            fontName = fontName,
            fontSizeSp = fontSizeSp,
            lineSpacingMult = lineSpacingMult,
            customCategory = customCategory,
            customText = customText,
            customRef = customRef,
            showLogo = showLogo,
            autoFitText = autoFitText,
            showWatermark = showWatermark,
            textWidthPercent = textWidthPercent,
            textLetterSpacing = textLetterSpacing,
            isTextBold = isTextBold,
            isForPreview = true
        )
        isGeneratingPreview = false
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ফটো কার্ড মেকার ও কাস্টমাইজ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "বন্ধ করুন")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (!isSharing) {
                                    isSharing = true
                                    coroutineScope.launch {
                                        val exportBitmap = PostShareUtil.generateCardBitmap(
                                            context = context,
                                            customTitleColor = customTitleColor,
                                            customTextColor = customTextColor,
                                            customRefColor = customRefColor,
                                            post = post,
                                            template = selectedTemplate,
                                            bgImageUrl = bgImageUrl.ifBlank { null },
                                            aspectRatio = aspectRatio,
                                            overlayAlpha = overlayAlpha,
                                            customOverlayColor = customOverlayColor,
                                            textAlignName = textAlignName,
                                            fontName = fontName,
                                            fontSizeSp = fontSizeSp,
                                            lineSpacingMult = lineSpacingMult,
                                            customCategory = customCategory,
                                            customText = customText,
                                            customRef = customRef,
                                            showLogo = showLogo,
                                            autoFitText = autoFitText,
                                            showWatermark = showWatermark,
                                            textWidthPercent = textWidthPercent,
                                            textLetterSpacing = textLetterSpacing,
                                            isTextBold = isTextBold,
                                            isForPreview = false
                                        )
                                        PostShareUtil.shareBitmap(context, exportBitmap)
                                        isSharing = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isSharing
                        ) {
                            if (isSharing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("শেয়ার করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                // FIXED TOP SECTION: Live Card Preview + Quick Action
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Fixed Live Preview Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cardBitmap != null) {
                                Image(
                                    bitmap = cardBitmap!!.asImageBitmap(),
                                    contentDescription = "Card Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }

                            if (isGeneratingPreview && cardBitmap == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = PrimaryGreen)
                                }
                            }
                        }
                    }

                    // Save to Gallery Action Button
                    Button(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                coroutineScope.launch {
                                    val exportBitmap = PostShareUtil.generateCardBitmap(
                                        context = context,
                                        customTitleColor = customTitleColor,
                                        customTextColor = customTextColor,
                                        customRefColor = customRefColor,
                                        post = post,
                                        template = selectedTemplate,
                                        bgImageUrl = bgImageUrl.ifBlank { null },
                                        aspectRatio = aspectRatio,
                                        overlayAlpha = overlayAlpha,
                                        customOverlayColor = customOverlayColor,
                                        textAlignName = textAlignName,
                                        fontName = fontName,
                                        fontSizeSp = fontSizeSp,
                                        lineSpacingMult = lineSpacingMult,
                                        customCategory = customCategory,
                                        customText = customText,
                                        customRef = customRef,
                                        showLogo = showLogo,
                                        autoFitText = autoFitText,
                                        showWatermark = showWatermark,
                                        textWidthPercent = textWidthPercent,
                                        textLetterSpacing = textLetterSpacing,
                                        isTextBold = isTextBold,
                                        isForPreview = false
                                    )
                                    val success = PostShareUtil.saveImageToGallery(context, exportBitmap)
                                    if (success) {
                                        Toast.makeText(context, "ছবি গ্যালারিতে সেভ হয়েছে", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "ছবি সেভ করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("গ্যালারিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // SCROLLABLE CONTROLS SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quick Navigation Filter Row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "⚙️ এডিটর সেটিংস ও কাস্টমাইজেশন",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        item {
                            FilterChip(
                                selected = isTemplateExpanded,
                                onClick = { isTemplateExpanded = !isTemplateExpanded },
                                label = { Text("✨ টেমপ্লেট") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryGreen
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = isContentExpanded,
                                onClick = { isContentExpanded = !isContentExpanded },
                                label = { Text("📝 কনটেন্ট") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryGreen
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = isBackgroundExpanded,
                                onClick = { isBackgroundExpanded = !isBackgroundExpanded },
                                label = { Text("🎨 ব্যাকগ্রাউন্ড") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryGreen
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = isTypographyExpanded,
                                onClick = { isTypographyExpanded = !isTypographyExpanded },
                                label = { Text("🔤 টাইপোগ্রাফি") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryGreen
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = isBrandingExpanded,
                                onClick = { isBrandingExpanded = !isBrandingExpanded },
                                label = { Text("🏷️ ব্র্যান্ডিং") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryGreen
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = isLayoutExpanded,
                                onClick = { isLayoutExpanded = !isLayoutExpanded },
                                label = { Text("🖼️ সাইজ") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryGreen
                                )
                            )
                        }
                    }
                }

                // Section 1: ✨ Template
                EditorCollapsibleSection(
                    icon = "✨",
                    title = "টেমপ্লেট ও স্টাইল",
                    subtitle = "${selectedTemplate.title} • ${selectedCategory.title}",
                    isExpanded = isTemplateExpanded,
                    onToggle = { isTemplateExpanded = !isTemplateExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "ক্যাটাগরি অনুযায়ী ফিল্টার করুন:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Category Selector
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(PostShareUtil.TemplateCategory.entries.toTypedArray()) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category.title, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                                        selectedLabelColor = PrimaryGreen
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Templates Selector
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filteredTemplates = PostShareUtil.preDefinedTemplates.filter { selectedCategory == PostShareUtil.TemplateCategory.ALL || selectedCategory in it.categories }
                            items(filteredTemplates) { template ->
                                val isSelected = selectedTemplate == template
                                val themeBg = Color(android.graphics.Color.parseColor(template.bgColors.first))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(themeBg)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) PrimaryGreen else Color.Gray.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedTemplate = template }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = template.title,
                                        fontSize = 12.sp,
                                        color = Color(android.graphics.Color.parseColor(template.textColor)),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: 📝 Content
                EditorCollapsibleSection(
                    icon = "📝",
                    title = "কনটেন্ট ও টেক্সট",
                    subtitle = if (customCategory.isNotBlank()) customCategory else "মূল বাণী ও রেফারেন্স",
                    isExpanded = isContentExpanded,
                    onToggle = { isContentExpanded = !isContentExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customCategory,
                            onValueChange = { customCategory = it },
                            label = { Text("কার্ডের টাইটেল / ক্যাটাগরি (ঐচ্ছিক)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = customText,
                            onValueChange = { customText = it },
                            label = { Text("কার্ডের মূল বাণী/নসীহত") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = customRef,
                            onValueChange = { customRef = it },
                            label = { Text("সূত্র / রেফারেন্স (ঐচ্ছিক)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Section 3: 🎨 Background
                EditorCollapsibleSection(
                    icon = "🎨",
                    title = "ব্যাকগ্রাউন্ড ও কালার",
                    subtitle = if (bgImageUrl.isNotEmpty()) "কাস্টম ছবি সিলেক্টেড" else "ডিফল্ট থিম ব্যাকগ্রাউন্ড",
                    isExpanded = isBackgroundExpanded,
                    onToggle = { isBackgroundExpanded = !isBackgroundExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "পছন্দের ক্যাটাগরি সিলেক্ট করুন:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Background Preset Categories
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(bgCategories) { category ->
                                FilterChip(
                                    selected = selectedBgCategory == category,
                                    onClick = { selectedBgCategory = category },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f), selectedLabelColor = PrimaryGreen)
                                )
                            }
                        }

                        // Background Image Grid
                        val filteredBgs = presetBgList.filter { selectedBgCategory == "সব" || it.category == selectedBgCategory }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (bgImageUrl.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { bgImageUrl = "" },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ছবি মুছুন (Clear Background)", color = PrimaryGreen)
                                }
                            }

                            filteredBgs.chunked(3).forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowItems.forEach { bg ->
                                        val isSelected = bgImageUrl == bg.url
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) PrimaryGreen else androidx.compose.ui.graphics.Color.LightGray,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { bgImageUrl = bg.url }
                                        ) {
                                            coil.compose.AsyncImage(
                                                model = bg.url,
                                                contentDescription = "Background",
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    val emptySpots = 3 - rowItems.size
                                    for (i in 0 until emptySpots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val isLocalImage = bgImageUrl.startsWith("content://") || bgImageUrl.startsWith("file://")

                        OutlinedTextField(
                            value = if (isLocalImage) "ডিভাইস গ্যালারির ছবি সিলেক্টেড" else bgImageUrl,
                            onValueChange = { bgImageUrl = it },
                            label = { Text("ছবির লিঙ্ক (URL) বা গ্যালারি থেকে নিন") },
                            readOnly = isLocalImage,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (bgImageUrl.isNotEmpty()) {
                                        IconButton(onClick = { bgImageUrl = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    IconButton(
                                        onClick = { imagePickerLauncher.launch("image/*") }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "গ্যালারি থেকে ছবি সিলেক্ট করুন",
                                            tint = PrimaryGreen
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (bgImageUrl.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            Text("ওভারলে ইনটেনসিটি (Overlay Intensity):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val intensities = listOf("None" to 0.0f, "Light" to 0.3f, "Medium" to 0.6f, "Dark" to 0.85f)
                                intensities.forEach { (label, alphaValue) ->
                                    val isSelected = (overlayAlpha == alphaValue)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { overlayAlpha = alphaValue },
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f), selectedLabelColor = PrimaryGreen)
                                    )
                                }
                            }
                            
                            ColorPlateSelector(
                                label = "ওভারলে কালার (Overlay Color):",
                                selectedColorHex = customOverlayColor,
                                onColorSelected = { customOverlayColor = it ?: "#000000" }
                            )
                        }
                    }
                }

                // Section 4: 🔤 Typography
                EditorCollapsibleSection(
                    icon = "🔤",
                    title = "টাইপোগ্রাফি ও ফন্ট",
                    subtitle = "$fontName • ${fontSizeSp.toInt()}sp",
                    isExpanded = isTypographyExpanded,
                    onToggle = { isTypographyExpanded = !isTypographyExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Bangla Fonts Selection
                        Column {
                            Text(
                                text = "বাংলা ফন্ট সিলেক্ট করুন (প্রিসেট ও কাস্টম)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val fontsList = listOf(
                                "SolaimanLipi" to "সোলাইমান লিপি",
                                "Hind Siliguri" to "হিন্দ শিলিগুড়ি",
                                "Shorif Shishir Unicode" to "শরীফ শিশির",
                                "Default" to "ডিফল্ট ফন্ট"
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (fontName.startsWith("content://") || fontName.startsWith("file://")) {
                                    item {
                                        FilterChip(
                                            selected = true,
                                            onClick = { },
                                            label = { Text("📁 কাস্টম ফন্ট", fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryGreen,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                                items(fontsList) { (key, label) ->
                                    FilterChip(
                                        selected = fontName == key,
                                        onClick = { fontName = key },
                                        label = { Text(label, fontWeight = if (fontName == key) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = PrimaryGreen
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { fontPickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(customFontName ?: "নিজের বাংলা ফন্ট (.ttf / .otf) আপলোড করুন", fontSize = 12.sp, color = PrimaryGreen)
                            }
                        }

                        // Arabic Fonts Selection
                        Column {
                            Text(
                                text = "🕌 আরবি ফন্ট",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val arabicFonts = listOf(
                                "Scheherazade New" to "শাহরাজাদ (Scheherazade)",
                                "Amiri" to "আমিরি (Amiri)"
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(arabicFonts) { (key, label) ->
                                    FilterChip(
                                        selected = fontName == key,
                                        onClick = { fontName = key },
                                        label = { Text(label, fontWeight = if (fontName == key) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = PrimaryGreen
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        // Text Alignment
                        Column {
                            Text(
                                text = "🔠 টেক্সট এলাইনমেন্ট",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val aligns = listOf("CENTER" to "মাঝখানে (Center)", "LEFT" to "বামে (Left)", "RIGHT" to "ডানে (Right)")
                                aligns.forEach { (key, label) ->
                                    FilterChip(
                                        selected = textAlignName == key,
                                        onClick = { textAlignName = key },
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f), selectedLabelColor = PrimaryGreen),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        // Font Size & Auto Fit
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔠 ফন্ট সাইজ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${fontSizeSp.toInt()} sp",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = fontSizeSp,
                                onValueChange = { fontSizeSp = it },
                                valueRange = 32f..58f,
                                colors = SliderDefaults.colors(thumbColor = PrimaryGreen, activeTrackColor = PrimaryGreen)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("অটো-ফিট (Auto Fit)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Switch(checked = autoFitText, onCheckedChange = { autoFitText = it }, colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen, checkedTrackColor = PrimaryGreen.copy(alpha = 0.5f)))
                            }
                        }

                        // Bold Text
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("বোল্ড টেক্সট (Bold Text)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = isTextBold, onCheckedChange = { isTextBold = it }, colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen, checkedTrackColor = PrimaryGreen.copy(alpha = 0.5f)))
                        }

                        // Line Spacing
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "↕️ লাইন স্পেসিং (Line Spacing)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2fx", lineSpacingMult),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = lineSpacingMult,
                                onValueChange = { lineSpacingMult = it },
                                valueRange = 0.8f..2.2f,
                                colors = SliderDefaults.colors(thumbColor = PrimaryGreen, activeTrackColor = PrimaryGreen)
                            )
                        }

                        // Letter Spacing
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "↔️ লেটার স্পেসিং (Letter Spacing)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f", textLetterSpacing),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = textLetterSpacing,
                                onValueChange = { textLetterSpacing = it },
                                valueRange = -0.1f..0.3f,
                                colors = SliderDefaults.colors(thumbColor = PrimaryGreen, activeTrackColor = PrimaryGreen)
                            )
                        }

                        // Text Width
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📏 টেক্সট প্রস্থ (Text Width)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(textWidthPercent * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = textWidthPercent,
                                onValueChange = { textWidthPercent = it },
                                valueRange = 0.5f..1f,
                                colors = SliderDefaults.colors(thumbColor = PrimaryGreen, activeTrackColor = PrimaryGreen)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        // Text Colors
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "🎨 টেক্সট কালার প্যালেট (Color Palette)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Title Color
                            ColorPlateSelector(
                                label = "টাইটেল / ক্যাটাগরি কালার:",
                                selectedColorHex = customTitleColor,
                                onColorSelected = { customTitleColor = it }
                            )

                            // Main Content Text Color
                            ColorPlateSelector(
                                label = "মূল বাণী / কনটেন্ট কালার:",
                                selectedColorHex = customTextColor,
                                onColorSelected = { customTextColor = it }
                            )

                            // Reference Color
                            ColorPlateSelector(
                                label = "রেফারেন্স / সূত্র কালার:",
                                selectedColorHex = customRefColor,
                                onColorSelected = { customRefColor = it }
                            )
                        }
                    }
                }

                // Section 5: 🏷️ Branding
                EditorCollapsibleSection(
                    icon = "🏷️",
                    title = "লোগো ও ব্র্যান্ডিং",
                    subtitle = "লোগো: ${if (showLogo) "চালু" else "বন্ধ"} • ক্রেডিট: ${if (showWatermark) "চালু" else "বন্ধ"}",
                    isExpanded = isBrandingExpanded,
                    onToggle = { isBrandingExpanded = !isBrandingExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = showLogo,
                                onClick = { showLogo = !showLogo },
                                label = { Text(if (showLogo) "লোগো চালু (Top-Left)" else "লোগো বন্ধ") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = showWatermark,
                                onClick = { showWatermark = !showWatermark },
                                label = { Text(if (showWatermark) "ক্রেডিট চালু (Bottom)" else "ক্রেডিট বন্ধ") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section 6: 🖼️ Layout & Size
                EditorCollapsibleSection(
                    icon = "🖼️",
                    title = "কার্ডের সাইজ ও লেআউট",
                    subtitle = "Aspect Ratio: $aspectRatio",
                    isExpanded = isLayoutExpanded,
                    onToggle = { isLayoutExpanded = !isLayoutExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "কার্ডের অনুপাত (Aspect Ratio):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val ratios = listOf("1:1" to "1:1", "4:5" to "4:5", "9:16" to "9:16", "16:9" to "16:9")
                            ratios.forEach { (label, value) ->
                                val isSelected = (aspectRatio == value)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { aspectRatio = value },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f), selectedLabelColor = PrimaryGreen),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
}

@Composable
fun EditorCollapsibleSection(
    icon: String,
    title: String,
    subtitle: String? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 0.5.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isExpanded) PrimaryGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = icon, fontSize = 20.sp)
                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "সংকুচিত করুন" else "প্রসারিত করুন",
                        tint = if (isExpanded) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    content()
                }
            }
        }
    }
}

@Composable
fun ColorPlateSelector(
    label: String,
    selectedColorHex: String?,
    onColorSelected: (String?) -> Unit,
    presets: List<PostShareUtil.ColorPreset> = PostShareUtil.TextColorPresets
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val activeName = if (selectedColorHex == null) {
                "অটো (ডিফল্ট)"
            } else {
                presets.find { it.hex.equals(selectedColorHex, ignoreCase = true) }?.name ?: selectedColorHex
            }
            Text(
                text = activeName,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Option 1: Template Default (অটো)
            item {
                val isDefault = selectedColorHex == null
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isDefault) PrimaryGreen.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .border(
                            width = if (isDefault) 2.dp else 1.dp,
                            color = if (isDefault) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onColorSelected(null) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isDefault) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ডিফল্ট",
                            fontSize = 11.5.sp,
                            fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDefault) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Color Plates (Swatches)
            items(presets) { preset ->
                val isSelected = selectedColorHex?.equals(preset.hex, ignoreCase = true) == true
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(preset.hex))
                } catch (e: Exception) {
                    Color.White
                }
                val isBright = (parsedColor.red * 0.299 + parsedColor.green * 0.587 + parsedColor.blue * 0.114) > 0.6

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) PrimaryGreen else if (isBright) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(preset.hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = preset.name,
                            tint = if (isBright) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddPostDialog(
    viewModel: PostsViewModel,
    defaultCategory: String = "সাধারণ",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isBlogType by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(defaultCategory) }
    var reference by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("ইসলামিক এডমিন") }
    var errorMessage by remember { mutableStateOf("") }

    val quickCategories = listOf("কুরআন ও জীবন", "নফল ইবাদত", "দৈনিক নসীহত", "মাসনুন জিকির", "আত্মশুদ্ধি", "নোটিফিকেশন", "সাধারণ")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Icon & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PostAdd,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "নতুন পোস্ট তৈরি করুন",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ইসলামিক কনটেন্ট ও নসীহত শেয়ার করুন",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Segmented Button Toggle for Post Type
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            // Blog Post Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isBlogType) PrimaryGreen else Color.Transparent)
                                    .clickable { isBlogType = true }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Article,
                                        contentDescription = null,
                                        tint = if (isBlogType) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "ব্লগ পোস্ট",
                                        fontSize = 13.sp,
                                        fontWeight = if (isBlogType) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isBlogType) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Photo Card / Short Post Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isBlogType) PrimaryGreen else Color.Transparent)
                                    .clickable { isBlogType = false }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        tint = if (!isBlogType) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "নসীহত/কার্ড",
                                        fontSize = 13.sp,
                                        fontWeight = if (!isBlogType) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isBlogType) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (isBlogType || category == "নোটিফিকেশন" || category == "নোটিশ") {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(if (category == "নোটিফিকেশন" || category == "নোটিশ") "নোটিফিকেশন শিরোনাম" else "ব্লগ শিরোনাম") },
                            placeholder = { Text("যেমন: আজকের বিশেষ নোটিশ / গুরুত্বপূর্ণ বার্তা") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Title,
                                    contentDescription = null,
                                    tint = PrimaryGreen
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("ছবি / ফটো লিঙ্ক (Image URL)") },
                        placeholder = { Text("যেমন: https://example.com/image.jpg (ঐচ্ছিক)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        label = { Text(if (isBlogType) "বিস্তারিত ব্লগ কনটেন্ট" else "সংক্ষিপ্ত নসীহত বা আয়াত/হাদিস") },
                        placeholder = { Text(if (isBlogType) "এখানে বিস্তারিত বক্তব্য লিখুন..." else "এখানে নসীহত বা বাণীটি লিখুন...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Category Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ক্যাটাগরি বাছাই করুন:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(quickCategories) { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                                        selectedLabelColor = PrimaryGreen
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("ক্যাটাগরি নাম (কাস্টম)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!isBlogType) {
                        OutlinedTextField(
                            value = reference,
                            onValueChange = { reference = it },
                            label = { Text("সূত্র / রেফারেন্স") },
                            placeholder = { Text("যেমন: সহীহ বুখারী, হাদিস ১২৩৪") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = PrimaryGreen
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("লেখকের নাম") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("বাতিল", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (isBlogType) {
                                if (title.isBlank() || contentText.isBlank()) {
                                    errorMessage = "শিরোনাম ও বিস্তারিত লেখা আবশ্যক"
                                    return@Button
                                }
                                viewModel.addBlogPost(
                                    title = title,
                                    content = contentText,
                                    category = category,
                                    author = author,
                                    imageUrl = imageUrl,
                                    onSuccess = {
                                        Toast.makeText(context, "ব্লগ পোস্ট সফলভাবে পাবলিশ হয়েছে!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    onError = { errorMessage = it }
                                )
                            } else {
                                if (contentText.isBlank()) {
                                    errorMessage = "নসীহত ও লেখা আবশ্যক"
                                    return@Button
                                }
                                viewModel.addShortPost(
                                    text = contentText,
                                    reference = reference,
                                    category = category,
                                    author = author,
                                    onSuccess = {
                                        Toast.makeText(context, "নসীহত কার্ড সফলভাবে পাবলিশ হয়েছে!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    onError = { errorMessage = it }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("পাবলিশ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AdminPasswordDialog(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = PrimaryGreen
                )
                Text(
                    text = "অ্যাডমিন পাসওয়ার্ড",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "নতুন পোস্ট যুক্ত করতে অ্যাডমিন পাসওয়ার্ড প্রদান করুন:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = false
                    },
                    label = { Text("পাসওয়ার্ড") },
                    singleLine = true,
                    isError = passwordError,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (passwordError) {
                    Text(
                        text = "ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val inputHash = try {
                        val md = MessageDigest.getInstance("SHA-256")
                        val digest = md.digest(password.trim().toByteArray())
                        digest.joinToString("") { "%02x".format(it) }
                    } catch (e: Exception) { "" }

                    // SHA-256 hash of "admin@#$%" and "admin@#$%&"
                    val targetHash1 = "2525164f23b2c17435fce1cbe4a3df578c734b46513b2e53526fa94ff1aef3f6"
                    val trimmedPass = password.trim()

                    if (inputHash == targetHash1 || trimmedPass == "admin@#$%" || trimmedPass == "admin@#$%&") {
                        onSuccess()
                    } else {
                        passwordError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("যাচাই করুন", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun NotificationCard(
    post: BlogPost,
    isRead: Boolean = true,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val backgroundColor = if (isRead) MaterialTheme.colorScheme.surface else PrimaryGreen.copy(alpha = 0.04f)
    val borderColor = if (isRead) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f) else PrimaryGreen.copy(alpha = 0.3f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
                if (!isRead) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PrimaryGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = if (post.category.isNotBlank()) "📢 ${post.category}" else "📢 নোটিফিকেশন",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = getRelativeTimeBengali(post.timestamp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onDeleteClick != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "মুছে ফেলুন",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = post.title,
                    fontSize = 15.sp,
                    fontWeight = if (isRead) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = post.content,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "বিস্তারিত দেখুন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun getRelativeTimeBengali(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val minute = 60 * 1000L
    val hour = 60 * minute
    val day = 24 * hour
    
    return when {
        diff < minute -> "এইমাত্র"
        diff < hour -> "${DateUtil.toBengaliNumerals((diff / minute).toInt())} মিনিট আগে"
        diff < day -> "${DateUtil.toBengaliNumerals((diff / hour).toInt())} ঘণ্টা আগে"
        diff < 2 * day -> "গতকাল"
        else -> "${DateUtil.toBengaliNumerals((diff / day).toInt())} দিন আগে"
    }
}
