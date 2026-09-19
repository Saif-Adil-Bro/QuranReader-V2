package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.AppViewModelFactory

import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import com.example.ui.components.BottomNavBar

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodels.HomeViewModel
import com.example.ui.screens.FloatingPlayerShortcut
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

  private val currentIntentState = androidx.compose.runtime.mutableStateOf<android.content.Intent?>(null)

  private val requestPermissionLauncher = registerForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
      // Handle permission result if needed
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    currentIntentState.value = intent

    val fcmPrefs = getSharedPreferences("fcm_prefs", android.content.Context.MODE_PRIVATE)
    val isSubscribed = fcmPrefs.getBoolean("subscribed_to_all", false)
    if (!isSubscribed) {
        try {
            val playServicesAvailable = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this) == com.google.android.gms.common.ConnectionResult.SUCCESS

            if (playServicesAvailable) {
                Firebase.messaging.subscribeToTopic("all")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            fcmPrefs.edit().putBoolean("subscribed_to_all", true).apply()
                        }
                    }
            }
        } catch (e: Throwable) {
            // Handled safely if Play Services / FCM broker is not accessible
        }
    }

    // Request notification permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    setContent {
      val appContainer = remember { (application as QuranApplication).container }
      val themeState by appContainer.settingsRepository.themeFlow.collectAsState(initial = "System")
      val keepScreenOn by appContainer.settingsRepository.keepScreenOnFlow.collectAsState(initial = false)
      val arabicFontName by appContainer.settingsRepository.arabicFontNameFlow.collectAsState(initial = "Me Quran")
      val bengaliFontName by appContainer.settingsRepository.bengaliFontNameFlow.collectAsState(initial = "SolaimanLipi")
      
      LaunchedEffect(keepScreenOn) {
          if (keepScreenOn) {
              window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          } else {
              window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          }
      }
      val isSystemDark = isSystemInDarkTheme()
      val darkTheme = when (themeState) {
          "Dark" -> true
          "Light" -> false
          else -> isSystemDark
      }
      MyApplicationTheme(
          darkTheme = darkTheme,
          arabicFontName = arabicFontName,
          bengaliFontName = bengaliFontName
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val viewModelFactory = AppViewModelFactory(
            quranRepository = appContainer.quranRepository,
            settingsRepository = appContainer.settingsRepository,
            audioRepository = appContainer.audioRepository,
            aiRepository = appContainer.aiRepository,
            bookmarkDao = appContainer.bookmarkDao,
            memorizedPageDao = appContainer.memorizedPageDao,
            mushafRepository = appContainer.mushafRepository,
            postsRepository = appContainer.postsRepository
          )
          
          val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
          val postsViewModel: com.example.ui.viewmodels.PostsViewModel = viewModel(factory = viewModelFactory)
          val navController = rememberNavController()
          val navBackStackEntry by navController.currentBackStackEntryAsState()
          val currentRoute = navBackStackEntry?.destination?.route
          val visibleEntries by navController.visibleEntries.collectAsState()
          val isSplashVisible = visibleEntries.any { it.destination.route == "splash" }
          
          val activeIntent by currentIntentState
          
          LaunchedEffect(activeIntent, currentRoute) {
              val targetIntent = activeIntent
              if (targetIntent != null && currentRoute != "splash" && currentRoute != null) {
                  val navigateTo = targetIntent.getStringExtra("navigate_to") ?: targetIntent.getStringExtra("target_screen")
                  if (navigateTo == "posts" || navigateTo == "notifications") {
                      val openBlogDetailBool = targetIntent.getBooleanExtra("open_blog_post_detail", false)
                      val openBlogDetailStr = targetIntent.getStringExtra("open_blog_post_detail") == "true"
                      var isNotificationCategory = false
                      if (openBlogDetailBool || openBlogDetailStr) {
                          val id = targetIntent.getStringExtra("blog_post_id") ?: ""
                          val title = targetIntent.getStringExtra("blog_post_title") ?: ""
                          val content = targetIntent.getStringExtra("blog_post_content") ?: ""
                          val category = targetIntent.getStringExtra("blog_post_category") ?: ""
                          val author = targetIntent.getStringExtra("blog_post_author") ?: ""
                          val readTime = targetIntent.getStringExtra("blog_post_read_time") ?: ""
                          val imageUrl = targetIntent.getStringExtra("blog_post_image_url") ?: ""
                          val timestamp = targetIntent.getLongExtra("blog_post_timestamp", System.currentTimeMillis())

                          if (category == "নোটিফিকেশন" || category == "নোটিশ") {
                              isNotificationCategory = true
                          }

                          if (title.isNotBlank() || content.isNotBlank()) {
                              val blogPost = com.example.data.model.BlogPost(
                                  id = id,
                                  title = title.ifBlank { "নতুন ইসলামিক পোস্ট" },
                                  content = content,
                                  category = category,
                                  author = author,
                                  readTime = readTime,
                                  imageUrl = imageUrl,
                                  timestamp = timestamp
                              )
                              postsViewModel.setPendingBlogPost(blogPost)
                          }
                          targetIntent.removeExtra("open_blog_post_detail")
                      }

                      val openEditBool = targetIntent.getBooleanExtra("open_photo_card_edit", false)
                      val openEditStr = targetIntent.getStringExtra("open_photo_card_edit") == "true"
                      if (openEditBool || openEditStr) {
                          val postId = targetIntent.getStringExtra("post_id") ?: ""
                          val postText = targetIntent.getStringExtra("post_text") ?: ""
                          val postRef = targetIntent.getStringExtra("post_ref") ?: ""
                          val postCategory = targetIntent.getStringExtra("post_category") ?: ""
                          val postAuthor = targetIntent.getStringExtra("post_author") ?: ""

                          if (postText.isNotBlank()) {
                              val shortPost = com.example.data.model.ShortPost(
                                  id = postId,
                                  text = postText,
                                  reference = postRef,
                                  category = postCategory,
                                  author = postAuthor
                              )
                              postsViewModel.setPendingPhotoCardPost(shortPost)
                          }
                          targetIntent.removeExtra("open_photo_card_edit")
                      }

                      val dest = if (navigateTo == "notifications" || isNotificationCategory) "notifications" else "posts"
                      if (currentRoute != dest) {
                          navController.navigate(dest) {
                              launchSingleTop = true
                          }
                      }
                      targetIntent.removeExtra("navigate_to")
                      targetIntent.removeExtra("target_screen")
                      currentIntentState.value = null
                  } else if (navigateTo == "dua") {
                      val duaId = targetIntent.getIntExtra("dua_id", -1)
                      if (currentRoute != "notifications") {
                          navController.navigate("notifications") { launchSingleTop = true }
                      }
                      navController.navigate("settings?subScreen=dua&duaId=$duaId") {
                          launchSingleTop = true
                      }
                      targetIntent.removeExtra("target_screen")
                      targetIntent.removeExtra("navigate_to")
                      targetIntent.removeExtra("dua_id")
                      currentIntentState.value = null
                  } else if (navigateTo == "prayer_times" || navigateTo == "planner" || navigateTo == "manzil" || navigateTo == "subjectwise" || navigateTo == "calendar") {
                      val sub = navigateTo
                      if (currentRoute != "notifications") {
                          navController.navigate("notifications") { launchSingleTop = true }
                      }
                      navController.navigate("settings?subScreen=$sub") {
                          launchSingleTop = true
                      }
                      targetIntent.removeExtra("target_screen")
                      targetIntent.removeExtra("navigate_to")
                      currentIntentState.value = null
                  } else if (navigateTo == "hijri_adjustment" || targetIntent.getBooleanExtra("highlight_hijri_adjustment", false)) {
                      if (currentRoute != "notifications") {
                          navController.navigate("notifications") { launchSingleTop = true }
                      }
                      navController.navigate("settings?highlightHijri=true") {
                          launchSingleTop = true
                      }
                      targetIntent.removeExtra("target_screen")
                      targetIntent.removeExtra("navigate_to")
                      targetIntent.removeExtra("highlight_hijri_adjustment")
                      currentIntentState.value = null
                  }
              }
          }
          
          Scaffold(
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            bottomBar = {
              BottomNavBar(
                navController = navController,
                currentRoute = currentRoute,
                isSplashVisible = isSplashVisible
              )
            }
          ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
              AppNavGraph(
                navController = navController,
                viewModelFactory = viewModelFactory,
                homeViewModel = homeViewModel,
                postsViewModel = postsViewModel,
                modifier = Modifier.fillMaxSize()
              )
              
              if (currentRoute != "splash" && currentRoute != "recitation/player") {
                FloatingPlayerShortcut(
                  viewModel = homeViewModel,
                  onClick = { navController.navigate("recitation/player") },
                  modifier = Modifier.align(Alignment.BottomEnd)
                )
              }
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      currentIntentState.value = intent
  }
}
