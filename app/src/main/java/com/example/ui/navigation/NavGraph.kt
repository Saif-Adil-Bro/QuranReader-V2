package com.example.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build
import coil.request.ImageRequest
import com.example.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.SearchScreen
import com.example.ui.viewmodels.SearchViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HafeziModeScreen
import com.example.ui.screens.QuranListScreen
import com.example.ui.screens.ReadingModeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SurahDetailScreen
import com.example.ui.screens.TajweedIndexScreen
import com.example.ui.screens.TajweedModeScreen
import com.example.ui.screens.RecitationPlayerScreen
import com.example.ui.screens.RecitationIndexScreen
import com.example.ui.viewmodels.AppViewModelFactory
import com.example.ui.viewmodels.HafeziModeViewModel
import com.example.ui.viewmodels.TajweedModeViewModel
import com.example.ui.viewmodels.HomeViewModel
import com.example.ui.viewmodels.PostsViewModel
import com.example.ui.viewmodels.QuranListViewModel
import com.example.ui.viewmodels.ReadingModeViewModel
import com.example.ui.viewmodels.SettingsViewModel
import com.example.ui.viewmodels.SurahDetailViewModel
import kotlinx.coroutines.delay

import com.example.ui.screens.mushaf.MushafTabScreen
import com.example.ui.screens.mushaf.MushafViewerScreen
import com.example.ui.screens.mushaf.QuranPoricitiScreen
import com.example.ui.viewmodels.MushafSelectionViewModel
import com.example.ui.viewmodels.MushafViewerViewModel
import com.example.ui.viewmodels.SplashViewModel
import com.example.ui.viewmodels.SplashLoadingState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModelFactory: AppViewModelFactory,
    homeViewModel: HomeViewModel,
    postsViewModel: PostsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 500))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 500))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 500))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 500))
        }
    ) {
        
        composable("splash") {
            val splashViewModel: SplashViewModel = viewModel(factory = viewModelFactory)
            SplashScreen(
                viewModel = splashViewModel,
                onSplashComplete = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("mushaf/poriciti") {
            QuranPoricitiScreen(
                viewModel = homeViewModel,
                onNavigateToMushafPage = { mushafId, page, showIndex ->
                    navController.navigate("mushaf/viewer/$mushafId?page=$page&showIndex=$showIndex")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("mushaf") {
            val viewModel: MushafSelectionViewModel = viewModel(factory = viewModelFactory)
            MushafTabScreen(
                onMushafSelected = { mushaf ->
                    navController.navigate("mushaf/viewer/${mushaf.id}")
                },
                onLastReadSelected = { mushafId, page ->
                    navController.navigate("mushaf/viewer/$mushafId?page=$page")
                },
                viewModel = viewModel
            )
        }

        composable(
            route = "mushaf/viewer/{mushafId}?page={page}&showIndex={showIndex}",
            arguments = listOf(
                navArgument("mushafId") { type = NavType.StringType },
                navArgument("page") { type = NavType.IntType; defaultValue = 1 },
                navArgument("showIndex") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val mushafId = backStackEntry.arguments?.getString("mushafId") ?: "madani"
            val page = backStackEntry.arguments?.getInt("page") ?: 1
            val showIndex = backStackEntry.arguments?.getBoolean("showIndex") ?: false
            val viewModel: MushafViewerViewModel = viewModel(factory = viewModelFactory)
            MushafViewerScreen(
                mushafId = mushafId,
                initialPage = page,
                initialShowIndex = showIndex,
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSurah = { surahNumber -> navController.navigate("detail/$surahNumber") },
                onNavigateToJuz = { juzNumber -> navController.navigate("juz/$juzNumber") },
                onNavigateToNormalMode = { navController.navigate("list/normal") },
                onNavigateToReadingMode = { surahNumber -> navController.navigate("reading/$surahNumber") },
                onNavigateToHafeziMode = { page -> navController.navigate("hafezi/$page") },
                onNavigateToSearch = { navController.navigate("search") },
                onSettingsClick = { navController.navigate("settings") },
                onNavigateToMushaf = { navController.navigate("mushaf") },
                onNavigateToMushafPoriciti = { navController.navigate("mushaf/poriciti") },
                onNavigateToMushafPage = { mushafId, page, showIndex ->
                    navController.navigate("mushaf/viewer/$mushafId?page=$page&showIndex=$showIndex")
                },
                onNavigateToSurahWithAyah = { surahNumber, viewMode, initialAyah ->
                    navController.navigate("detail/$surahNumber?viewMode=$viewMode&initialAyah=$initialAyah")
                },
                onNavigateToTajweedIndex = {
                    navController.navigate("tajweed/index")
                },
                onNavigateToTajweedMode = { page ->
                    navController.navigate("tajweed/$page")
                },
                onNavigateToPlayer = {
                    if (homeViewModel.currentPlayingSurah.value != null) {
                        navController.navigate("recitation/player")
                    } else {
                        navController.navigate("recitation/index")
                    }
                },
                onNavigateToPosts = {
                    navController.navigate("posts")
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                },
                onNavigateToCalendar = {
                    navController.navigate("settings?subScreen=calendar")
                },
                postsViewModel = postsViewModel
            )
        }

        composable("posts") {
            com.example.ui.screens.PostsScreen(
                viewModel = postsViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToNotifications = { navController.navigate("notifications") }
            )
        }

        composable("notifications") {
            com.example.ui.screens.NotificationScreen(
                viewModel = postsViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToDua = { duaId ->
                    if (duaId != null && duaId >= 0) {
                        navController.navigate("settings?subScreen=dua&duaId=$duaId") {
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate("settings?subScreen=dua") {
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateToPlanner = {
                    navController.navigate("settings?subScreen=planner") {
                        launchSingleTop = true
                    }
                },
                onNavigateToManzil = {
                    navController.navigate("settings?subScreen=manzil") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSubjectwise = {
                    navController.navigate("settings?subScreen=subjectwise") {
                        launchSingleTop = true
                    }
                },
                onNavigateToCalendar = {
                    navController.navigate("settings?subScreen=calendar") {
                        launchSingleTop = true
                    }
                },
                onNavigateToDhikrReminder = { type ->
                    if (type == com.example.utils.DhikrType.DUROOD) {
                        navController.navigate("reminder/durood")
                    } else {
                        navController.navigate("reminder/istighfar")
                    }
                },
                onNavigateToHijriAdjustment = {
                    navController.navigate("settings?highlightHijri=true") {
                        launchSingleTop = true
                    }
                },
                onNavigateToPrayerTimes = {
                    navController.navigate("settings?subScreen=prayer_times") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("reminder/durood") {
            com.example.ui.screens.DhikrReminderScreen(
                type = com.example.utils.DhikrType.DUROOD,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("reminder/istighfar") {
            com.example.ui.screens.DhikrReminderScreen(
                type = com.example.utils.DhikrType.ISTIGHFAR,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("recitation/index") { backStackEntry ->
            RecitationIndexScreen(
                viewModel = homeViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("recitation/player") }
            )
        }

        composable("recitation/player") { backStackEntry ->
            RecitationPlayerScreen(
                viewModel = homeViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("tajweed/index") {
            TajweedIndexScreen(
                homeViewModel = homeViewModel,
                onBackClick = { navController.popBackStack() },
                onPageClick = { page -> navController.navigate("tajweed/$page") },
                onSurahClick = { surahId ->
                    val startPage = com.example.data.QuranData.surahStartPages[surahId - 1]
                    navController.navigate("tajweed/$startPage")
                },
                onJuzClick = { juzId ->
                    val startPage = com.example.data.HafeziQuranData.getParaStartPage(juzId, 1)
                    navController.navigate("tajweed/$startPage")
                },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable(
            route = "tajweed/{page}",
            arguments = listOf(navArgument("page") { type = NavType.IntType })
        ) { backStackEntry ->
            val pageArg = backStackEntry.arguments?.getInt("page") ?: 1
            val page = if (pageArg in 1..610) pageArg else 1
            val viewModel: TajweedModeViewModel = viewModel(factory = viewModelFactory)
            TajweedModeScreen(
                viewModel = viewModel,
                initialPage = page,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "list/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "normal"
            val viewModel: QuranListViewModel = viewModel(factory = viewModelFactory)
            QuranListScreen(
                viewModel = viewModel,
                homeViewModel = homeViewModel,
                mode = mode,
                onSurahClick = { surahNumber ->
                    if (mode == "normal") {
                        navController.navigate("detail/$surahNumber")
                    } else {
                        navController.navigate("reading/$surahNumber")
                    }
                },
                onNavigateToSurahWithAyah = { surahNumber, viewMode, initialAyah ->
                    navController.navigate("detail/$surahNumber?viewMode=$viewMode&initialAyah=$initialAyah")
                },
                onJuzClick = { juzNumber ->
                    if (mode == "normal") {
                        navController.navigate("juz/$juzNumber")
                    } else {
                        val startSurah = when (juzNumber) {
                            1 -> 1
                            2 -> 2
                            3 -> 2
                            4 -> 3
                            5 -> 4
                            6 -> 4
                            7 -> 5
                            8 -> 6
                            9 -> 7
                            10 -> 8
                            11 -> 9
                            12 -> 11
                            13 -> 12
                            14 -> 15
                            15 -> 17
                            16 -> 18
                            17 -> 21
                            18 -> 23
                            19 -> 25
                            20 -> 27
                            21 -> 29
                            22 -> 33
                            23 -> 36
                            24 -> 39
                            25 -> 41
                            26 -> 46
                            27 -> 51
                            28 -> 58
                            29 -> 67
                            30 -> 78
                            else -> 1
                        }
                        navController.navigate("reading/$startSurah")
                    }
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "detail/{surahNumber}?viewMode={viewMode}&initialAyah={initialAyah}",
            arguments = listOf(
                navArgument("surahNumber") { type = NavType.IntType },
                navArgument("viewMode") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("initialAyah") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
            val viewMode = backStackEntry.arguments?.getString("viewMode")
            val initialAyah = backStackEntry.arguments?.getInt("initialAyah") ?: -1
            val viewModel: SurahDetailViewModel = viewModel(factory = viewModelFactory)
            SurahDetailScreen(
                surahNumber = surahNumber,
                isJuz = false,
                initialViewMode = viewMode,
                initialAyah = initialAyah,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "juz/{juzNumber}",
            arguments = listOf(navArgument("juzNumber") { type = NavType.IntType })
        ) { backStackEntry ->
            val juzNumber = backStackEntry.arguments?.getInt("juzNumber") ?: 1
            val viewModel: SurahDetailViewModel = viewModel(factory = viewModelFactory)
            SurahDetailScreen(
                surahNumber = juzNumber,
                isJuz = true,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "reading/{surahNumber}",
            arguments = listOf(navArgument("surahNumber") { type = NavType.IntType })
        ) { backStackEntry ->
            val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
            val viewModel: ReadingModeViewModel = viewModel(factory = viewModelFactory)
            ReadingModeScreen(
                surahNumber = surahNumber,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "hafezi/{page}",
            arguments = listOf(navArgument("page") { type = NavType.IntType })
        ) { backStackEntry ->
            val pageArg = backStackEntry.arguments?.getInt("page") ?: 1
            val page = if (pageArg in 1..610) pageArg else 1
            val viewModel: HafeziModeViewModel = viewModel(factory = viewModelFactory)
            HafeziModeScreen(
                viewModel = viewModel,
                initialPage = page,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "settings?subScreen={subScreen}&duaId={duaId}&highlightHijri={highlightHijri}",
            arguments = listOf(
                navArgument("subScreen") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("duaId") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("highlightHijri") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val subScreen = backStackEntry.arguments?.getString("subScreen")
            val duaIdVal = backStackEntry.arguments?.getInt("duaId") ?: -1
            val highlightHijriVal = backStackEntry.arguments?.getBoolean("highlightHijri") ?: false
            val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSurah = { surahNumber ->
                    navController.navigate("detail/$surahNumber")
                },
                onNavigateToPage = { page ->
                    navController.navigate("hafezi/$page")
                },
                onNavigateToJuz = { juzNumber ->
                    navController.navigate("juz/$juzNumber")
                },
                onNavigateToAyah = { surahNumber, ayahNumber ->
                    navController.navigate("detail/$surahNumber?viewMode=LIST&initialAyah=$ayahNumber")
                },
                onNavigateToPlayer = {
                    if (homeViewModel.currentPlayingSurah.value != null) {
                        navController.navigate("recitation/player")
                    } else {
                        navController.navigate("recitation/index")
                    }
                },
                onNavigateToPosts = {
                    navController.navigate("posts")
                },
                onNavigateToMushafPage = { mushafId, page ->
                    navController.navigate("mushaf/viewer/$mushafId?page=$page")
                },
                initialSubScreen = subScreen,
                initialDuaId = if (duaIdVal != -1) duaIdVal else null,
                highlightHijriAdjustment = highlightHijriVal
            )
        }

        composable("search") {
            val viewModel: SearchViewModel = viewModel(factory = viewModelFactory)
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSurah = { surahNumber ->
                    navController.navigate("detail/$surahNumber")
                },
                onNavigateToAyah = { surahNumber, ayahNumber ->
                    navController.navigate("detail/$surahNumber?viewMode=LIST&initialAyah=$ayahNumber")
                }
            )
        }

        composable(
            route = "quran_video_creator?surah={surah}&ayah={ayah}",
            arguments = listOf(
                navArgument("surah") { type = NavType.IntType; defaultValue = 1 },
                navArgument("ayah") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val surah = backStackEntry.arguments?.getInt("surah") ?: 1
            val ayah = backStackEntry.arguments?.getInt("ayah") ?: 1
            val videoVm: com.example.ui.viewmodels.QuranVideoViewModel = viewModel()
            LaunchedEffect(surah, ayah) {
                videoVm.loadSurah(surah, ayah, ayah)
            }
            com.example.ui.screens.QuranVideoCreatorScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = videoVm
            )
        }
    }
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onSplashComplete: () -> Unit
) {
    val loadingState by viewModel.loadingState.collectAsState()
    
    LaunchedEffect(loadingState) {
        if (loadingState is SplashLoadingState.Complete) {
            onSplashComplete()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF115E39)), // Deep elegant dark green background
        contentAlignment = Alignment.Center
    ) {
        // Elegant pulsing logo or icon
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        val heartTransition = rememberInfiniteTransition(label = "heartBeat")
        val heartScale by heartTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = FastOutLinearInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "heartScale"
        )
        
        val glowAlpha by heartTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
        
        val context = LocalContext.current
        val splashResId = remember(context) {
            val drawableId = context.resources.getIdentifier("splash_logo", "drawable", context.packageName)
            if (drawableId != 0) drawableId else context.resources.getIdentifier("splash_logo", "raw", context.packageName)
        }
        val brandResId = remember(context) {
            val drawableId = context.resources.getIdentifier("splash_brand", "drawable", context.packageName)
            if (drawableId != 0) drawableId else context.resources.getIdentifier("splash_brand", "raw", context.packageName)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // 1. Center Logo / GIF Section
            Box(
                modifier = Modifier
                    .scale(pulseScale)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (splashResId != 0) {
                    // Animated GIF using Coil with GifDecoder support
                    val imageLoader = remember(context) {
                        ImageLoader.Builder(context)
                            .components {
                                if (Build.VERSION.SDK_INT >= 28) {
                                    add(ImageDecoderDecoder.Factory())
                                } else {
                                    add(GifDecoder.Factory())
                                }
                            }
                            .build()
                    }
                    
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(splashResId)
                            .build(),
                        imageLoader = imageLoader
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(200.dp), // Spacious centered container for animated GIF splash logo
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = "Animated Splash Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Fallback launcher icon with gentle glowing background
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF10B981).copy(alpha = 0.25f * glowAlpha),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "Quran Reader",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "কুরআন রিডার",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Dynamic loading animation (Phase 2: Loading State)
            AnimatedVisibility(
                visible = loadingState is SplashLoadingState.Loading,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "কুরআন ডাটা লোড করা হচ্ছে...",
                        fontSize = 14.sp,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
        
        // 2. Lovely 'A ❤️ S' signature or dynamic GIF positioned at the absolute bottom of the screen like a credit
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            if (brandResId != 0) {
                val brandImageLoader = remember(context) {
                    ImageLoader.Builder(context)
                        .components {
                            if (Build.VERSION.SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()
                }
                
                val brandPainter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(brandResId)
                        .build(),
                    imageLoader = brandImageLoader
                )
                
                Image(
                    painter = brandPainter,
                    contentDescription = "Brand GIF",
                    modifier = Modifier.height(36.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "A",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.95f),
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFFFFD700),
                                offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                                blurRadius = 6f
                            )
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "❤️",
                        fontSize = 24.sp,
                        modifier = Modifier
                            .scale(heartScale)
                            .align(Alignment.CenterVertically),
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFFFF0000),
                                offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                blurRadius = 12f
                            )
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "S",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.95f),
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFF60A5FA),
                                offset = androidx.compose.ui.geometry.Offset(-1.5f, 1.5f),
                                blurRadius = 6f
                            )
                        )
                    )
                }
            }
        }
    }
}
