@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.casty.music.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.casty.music.data.Song
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.PlayerServiceConnection
import com.casty.music.R
import com.casty.music.ui.components.BottomTab
import com.casty.music.ui.components.BottomNavBar
import com.casty.music.ui.components.CastyStartupAnimation
import com.casty.music.ui.components.MiniPlayer
import com.casty.music.ui.components.FullscreenNowPlaying
import com.casty.music.ui.screens.*
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.CastyPlayerViewModel
import com.casty.music.viewmodel.ContentType
import com.casty.music.backend.innertube.YouTube
import com.casty.music.backend.LocalPlayerConnection
import com.casty.music.backend.constants.UseLoginForBrowse
import com.casty.music.backend.security.SecureSessionStore
import com.casty.music.backend.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CastyMainActivity : ComponentActivity() {

    @Inject
    lateinit var playerConnection: PlayerServiceConnection

    @Inject
    lateinit var databaseRepository: DatabaseRepository

    @Inject
    lateinit var secureSessionStore: SecureSessionStore

    @OptIn(
        ExperimentalMaterial3WindowSizeClassApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalMaterial3Api::class,
        DelicateCoroutinesApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dump any previous fatal crash (written by the handler in CastyApplication) so the root cause
        // is visible in logcat even after immediate exit. This helps debug the "app closes on launch" cases.
        try {
            val crashFile = cacheDir.resolve("last_crash.txt")
            if (crashFile.exists()) {
                val content = crashFile.readText()
                Timber.tag("CastyCrash").e("=== PREVIOUS LAUNCH CRASH (persisted) ===\n$content\n=== END PREV CRASH ===")
                // keep the file for user inspection; delete only on successful full start or manually
            }
        } catch (e: Exception) {
            Timber.tag("CastyCrash").e(e, "Failed to read persisted crash log")
        }
        
        try {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        } catch (e: Exception) {
            Timber.e(e, "enableEdgeToEdge failed")
        }

        requestNotificationPermissionIfNeeded()

        // Nuclear fallback - if we reach here without crashing, dump any previous crash report
        dumpPreviousCrashToLogcat()

        // CRITICAL FIX: Initialize player service connection immediately on app start
        Timber.d("CastyMainActivity: Initializing player service connection...")
        playerConnection.connect(startPlaybackService = true)

        try {
            setContent {
                // Safe collection with default
                val castyPlayerConnection by playerConnection.connection.collectAsState()

                CompositionLocalProvider(
                    LocalPlayerConnection provides castyPlayerConnection
                ) {
                    CastyTheme {
                        MainContent(
                            playerConnection = playerConnection,
                            databaseRepository = databaseRepository,
                            secureSessionStore = secureSessionStore,
                            savedInstanceState = savedInstanceState,
                            intent = intent
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Fatal error in setContent")
            showCrashFallbackScreen(t)
        }
        }

        @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
        @Composable
        private fun MainContent(
        playerConnection: PlayerServiceConnection,
        databaseRepository: DatabaseRepository,
        secureSessionStore: SecureSessionStore,
        savedInstanceState: Bundle?,
        intent: android.content.Intent?
        ) {
        val navController = rememberNavController()

        // Safe window size calculation
        val windowSizeClass = calculateWindowSizeClass(this)
        val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

        // ViewModels handled by Hilt
        val playerViewModel: CastyPlayerViewModel = hiltViewModel()
        val playerState by playerViewModel.uiState.collectAsState()

        var showStartupAnimation by rememberSaveable {
            mutableStateOf(savedInstanceState == null)
        }

        var showFullscreenPlayer by remember {
            mutableStateOf(intent?.getBooleanExtra("expandPlayerBottomSheet", false) == true)
        }

        val onPlaySong = { song: Song ->
            databaseRepository.rememberSong(song)
            playerConnection.playSong(song)
        }

        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (!isTablet) {
                        Column(modifier = Modifier.background(Color.Transparent)) {
                            // Floating Mini Player
                            AnimatedVisibility(
                                visible = playerState.currentMediaItem != null,
                                enter = slideInVertically { it },
                                exit = slideOutVertically { it }
                            ) {
                                MiniPlayer(
                                    viewModel = playerViewModel,
                                    onExpand = { showFullscreenPlayer = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            BottomNavBar(
                                selectedTab = currentRoute.toBottomTab(),
                                onTabSelected = { tab ->
                                    val route = tab.toRoute()
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                containerColor = CastyTheme.colors.systemCanvas,
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.systemBars,
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding(),
                        )
                ) {
                    // Tablet Sidebar Navigation
                    if (isTablet) {
                        TabletNavigationSidebar(
                            navController = navController,
                            currentRoute = currentRoute,
                            playerActive = playerState.currentMediaItem != null,
                            playerViewModel = playerViewModel,
                            onPlayerClick = { showFullscreenPlayer = true }
                        )
                    }

                    // Main Navigation Graph Host
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestinationFromIntent(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("homeScreenRoute") {
                                HomeScreen(navController = navController)
                            }
                            composable("searchScreenRoute") {
                                SearchScreen(
                                    navController = navController,
                                    onPlaySong = onPlaySong
                                )
                            }
                            composable("libraryScreenRoute") {
                                LibraryScreen(navController = navController)
                            }
                            composable("accountScreenRoute") {
                                CastyAccountScreen(
                                    databaseRepository = databaseRepository,
                                    secureSessionStore = secureSessionStore,
                                )
                            }
                            composable(
                                route = "playlistScreenRoute/{type}/{id}",
                                arguments = listOf(
                                    navArgument("type") { type = NavType.StringType },
                                    navArgument("id") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val typeStr = backStackEntry.arguments?.getString("type") ?: "playlist"
                                val id = backStackEntry.arguments?.getString("id").orEmpty()
                                val contentType = if (typeStr == "album") ContentType.ALBUM else ContentType.PLAYLIST
                                PlaylistScreen(
                                    navController = navController,
                                    contentType = contentType,
                                    contentId = id
                                )
                            }
                            composable("artistScreenRoute/{id}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id").orEmpty()
                                ArtistScreen(
                                    navController = navController,
                                    artistId = id
                                )
                            }
                            composable("podcastScreenRoute/{id}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id").orEmpty()
                                PodcastScreen(navController = navController, showId = id)
                            }
                            composable("settingsScreenRoute") {
                                SettingsScreen(navController = navController)
                            }
                        }
                    }
                }
            }

            // Fullscreen Now Playing sheet overlay
            if (showFullscreenPlayer && playerState.currentMediaItem != null) {
                FullscreenNowPlaying(
                    viewModel = playerViewModel,
                    visible = true,
                    onDismiss = { showFullscreenPlayer = false }
                )
            }
            CastyStartupAnimation(
                visible = showStartupAnimation,
                onFinished = { showStartupAnimation = false },
            )
        }
        }

        private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            notificationPermissionRequestCode,
        )
        }

        private fun dumpPreviousCrashToLogcat() {
            try {
                val candidates = listOf(
                    cacheDir.resolve("last_crash.txt"),
                    getExternalFilesDir(null)?.resolve("Casty_crash_log.txt"),
                    @Suppress("DEPRECATION")
                    java.io.File(android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS), "Casty_crash_log.txt")
                )
                for (f in candidates) {
                    if (f != null && f.exists()) {
                        val content = f.readText()
                        Timber.tag("CastyCrash").e("=== PREVIOUS CRASH FOUND ===\n$content\n=== END ===")
                        break
                    }
                }
            } catch (e: Exception) {
                Timber.tag("CastyCrash").e(e, "Failed to dump previous crash")
            }
        }

        private fun showCrashFallbackScreen(throwable: Throwable) {
            val crashText = buildString {
                appendLine("CASTY FAILED TO START")
                appendLine("This screen appears when the app crashes very early.")
                appendLine()
                appendLine("Error: ${throwable.message}")
                appendLine()
                appendLine("=== STACKTRACE ===")
                appendLine(android.util.Log.getStackTraceString(throwable))
                appendLine()
                appendLine("=== LAST KNOWN CRASH LOG (if any) ===")

                val candidates = listOf(
                    cacheDir.resolve("last_crash.txt"),
                    getExternalFilesDir(null)?.resolve("Casty_crash_log.txt"),
                    @Suppress("DEPRECATION")
                    java.io.File(android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS), "Casty_crash_log.txt")
                )
                var found = false
                for (f in candidates) {
                    if (f != null && f.exists()) {
                        appendLine("File: ${f.absolutePath}")
                        appendLine(f.readText())
                        found = true
                        break
                    }
                }
                if (!found) {
                    appendLine("No persisted crash log file found.")
                    appendLine("Please check logcat with tag 'CastyCrash'.")
                }
            }

            val scrollView = android.widget.ScrollView(this)
            val textView = android.widget.TextView(this).apply {
                text = crashText
                setTextIsSelectable(true)
                textSize = 12f
                setPadding(24, 24, 24, 24)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            scrollView.addView(textView)

            // Add a simple "Copy to clipboard" button at the bottom
            val copyBtn = android.widget.Button(this).apply {
                text = "Copy Crash Report to Clipboard"
                setOnClickListener {
                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Casty Crash", crashText)
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(this@CastyMainActivity, "Crash report copied!", android.widget.Toast.LENGTH_LONG).show()
                }
            }

            val layout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                addView(copyBtn)
                addView(scrollView, android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT
                ))
            }

            setContentView(layout)
        }

        companion object {
        const val action_search = "com.casty.music.action.search"
        const val action_songs = "com.casty.music.action.songs"
        const val action_albums = "com.casty.music.action.albums"
        private const val notificationPermissionRequestCode = 9127
        }
        }

private fun CastyMainActivity.startDestinationFromIntent(): String = when (intent?.action) {
    CastyMainActivity.action_search -> "searchScreenRoute"
    CastyMainActivity.action_songs,
    CastyMainActivity.action_albums -> "libraryScreenRoute"
    else -> "homeScreenRoute"
}

private fun String?.toBottomTab(): BottomTab = when (this) {
    "searchScreenRoute" -> BottomTab.Search
    "libraryScreenRoute" -> BottomTab.Library
    "accountScreenRoute" -> BottomTab.Account
    else -> BottomTab.Home
}

private fun BottomTab.toRoute(): String = when (this) {
    BottomTab.Home -> "homeScreenRoute"
    BottomTab.Search -> "searchScreenRoute"
    BottomTab.Library -> "libraryScreenRoute"
    BottomTab.Account -> "accountScreenRoute"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CastyAccountScreen(
    databaseRepository: DatabaseRepository,
    secureSessionStore: SecureSessionStore,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var useLoginForBrowse by rememberPreference(UseLoginForBrowse, true)
    var visitorData by remember { mutableStateOf(secureSessionStore.visitorData.orEmpty()) }
    var dataSyncId by remember { mutableStateOf(secureSessionStore.dataSyncId.orEmpty()) }
    var innerTubeCookie by remember { mutableStateOf(secureSessionStore.innerTubeCookie.orEmpty()) }
    var accountName by remember { mutableStateOf(secureSessionStore.accountName.orEmpty()) }
    var accountEmail by remember { mutableStateOf(secureSessionStore.accountEmail.orEmpty()) }
    var accountChannelHandle by remember { mutableStateOf(secureSessionStore.accountChannelHandle.orEmpty()) }
    var showLogin by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isCompletingLogin by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun completeLogin() {
        if (isCompletingLogin) return
        isCompletingLogin = true
        coroutineScope.launch {
            val currentCookie = CookieManager.getInstance()
                .getCookie("https://music.youtube.com")
                .orEmpty()

            if (currentCookie.isBlank()) {
                statusText = "No YouTube Music cookie found yet."
                isCompletingLogin = false
                showLogin = false
                return@launch
            }

            innerTubeCookie = currentCookie
            secureSessionStore.updateSession(
                innerTubeCookie = currentCookie,
                visitorData = visitorData.takeIf { it.isNotBlank() },
                dataSyncId = dataSyncId.takeIf { it.isNotBlank() },
            )
            useLoginForBrowse = true
            delay(500)

            YouTube.cookie = currentCookie
            YouTube.visitorData = visitorData.takeIf { it.isNotBlank() }
            YouTube.dataSyncId = dataSyncId.takeIf { it.isNotBlank() }
            YouTube.useLoginForBrowse = true

            YouTube.accountInfo()
                .onSuccess { account ->
                    accountName = account.name
                    accountEmail = account.email.orEmpty()
                    accountChannelHandle = account.channelHandle.orEmpty()
                    secureSessionStore.updateAccount(
                        name = accountName,
                        email = accountEmail.takeIf { it.isNotBlank() },
                        channelHandle = accountChannelHandle.takeIf { it.isNotBlank() },
                    )
                    statusText = "Signed in as ${account.name}. Syncing library..."
                    withContext(Dispatchers.IO) {
                        databaseRepository.refreshLibraryContent()
                    }
                    statusText = "Signed in as ${account.name}. Library synced."
                    webView?.apply {
                        stopLoading()
                        clearHistory()
                        clearCache(true)
                        clearFormData()
                    }
                    showLogin = false
                }
                .onFailure {
                    Timber.e(it, "Casty account validation failed")
                    statusText = "Saved login cookie, but account validation failed."
                    showLogin = false
                }

            isCompletingLogin = false
        }
    }

    if (showLogin) {
        BackHandler {
            val currentWebView = webView
            if (currentWebView?.canGoBack() == true) {
                currentWebView.goBack()
            } else {
                completeLogin()
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CastyTheme.colors.systemCanvas)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { webViewContext ->
                    WebView(webViewContext).apply {
                        settings.javaScriptEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                view.loadUrl("javascript:Android.onRetrieveVisitorData(window.yt?.config_?.VISITOR_DATA)")
                                view.loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt?.config_?.DATASYNC_ID)")
                            }
                        }
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onRetrieveVisitorData(newVisitorData: String?) {
                                    if (!newVisitorData.isNullOrBlank() && newVisitorData != "undefined") {
                                        visitorData = newVisitorData
                                    }
                                }

                                @JavascriptInterface
                                fun onRetrieveDataSyncId(newDataSyncId: String?) {
                                    if (!newDataSyncId.isNullOrBlank() && newDataSyncId != "undefined") {
                                        dataSyncId = newDataSyncId.substringBefore("||")
                                    }
                                }
                            },
                            "Android"
                        )
                        webView = this
                        loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(CastyTheme.colors.systemCanvas.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCompletingLogin) "Completing sign in..." else "YouTube Music sign in",
                    style = CastyTheme.typography.titleMedium.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Button(
                    onClick = { completeLogin() },
                    enabled = !isCompletingLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = CastyTheme.colors.accentPink)
                ) {
                    Text(text = "Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CastyTheme.colors.systemCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Text(
            text = "Account",
            style = CastyTheme.typography.displayMedium.copy(
                color = CastyTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CastyTheme.colors.elevation2)
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = accountName.ifBlank { "YouTube Music" },
                    style = CastyTheme.typography.titleLarge.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        accountEmail.isNotBlank() -> accountEmail
                        accountChannelHandle.isNotBlank() -> accountChannelHandle
                        innerTubeCookie.isNotBlank() -> "Signed in through Casty"
                        else -> "Sign in to sync playlists and account library."
                    },
                    style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary)
                )
                statusText?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = it,
                        style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.accentPink)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { showLogin = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CastyTheme.colors.accentPink)
        ) {
            Text(
                text = if (innerTubeCookie.isBlank()) "Sign in with Google" else "Refresh Google sign in",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    YouTube.cookie = innerTubeCookie.takeIf { it.isNotBlank() }
                    YouTube.visitorData = visitorData.takeIf { it.isNotBlank() }
                    YouTube.dataSyncId = dataSyncId.takeIf { it.isNotBlank() }
                    YouTube.useLoginForBrowse = useLoginForBrowse
                    YouTube.accountInfo()
                        .onSuccess {
                            accountName = it.name
                            accountEmail = it.email.orEmpty()
                            accountChannelHandle = it.channelHandle.orEmpty()
                            secureSessionStore.updateAccount(
                                name = accountName,
                                email = accountEmail.takeIf { it.isNotBlank() },
                                channelHandle = accountChannelHandle.takeIf { it.isNotBlank() },
                            )
                            statusText = "Refreshing library..."
                            withContext(Dispatchers.IO) {
                                databaseRepository.refreshLibraryContent()
                            }
                            statusText = "Account and library refreshed"
                        }
                        .onFailure {
                            Timber.e(it, "Casty account refresh failed")
                            statusText = "Unable to refresh account right now."
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Refresh account data", color = CastyTheme.colors.textPrimary)
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = {
                CookieManager.getInstance().removeAllCookies(null)
                innerTubeCookie = ""
                visitorData = ""
                dataSyncId = ""
                accountName = ""
                accountEmail = ""
                accountChannelHandle = ""
                secureSessionStore.clear()
                YouTube.cookie = null
                YouTube.visitorData = null
                YouTube.dataSyncId = null
                statusText = "Signed out"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Sign out", color = CastyTheme.colors.textSecondary)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Casty stores Casty's YouTube Music cookie, visitor data, and data sync id locally for playback, library, and sync APIs.",
            style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.textSecondary)
        )
    }
}

@Composable
private fun TabletNavigationSidebar(
    navController: NavController,
    currentRoute: String?,
    playerActive: Boolean,
    playerViewModel: CastyPlayerViewModel,
    onPlayerClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (expanded) 320.dp else 92.dp,
        label = "TabletSidebarWidth"
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(sidebarWidth)
            .background(CastyTheme.colors.elevation1)
            .padding(horizontal = 12.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CastyTheme.colors.accentPink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "C",
                    style = CastyTheme.typography.titleMedium.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            if (expanded) {
                Text(
                    text = "Casty",
                    style = CastyTheme.typography.displaySmall.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
            }
            Text(
                text = if (expanded) "Hide" else "Open",
                style = CastyTheme.typography.labelMedium.copy(color = CastyTheme.colors.textSecondary),
                modifier = Modifier
                    .clip(RoundedCornerShape(500.dp))
                    .background(CastyTheme.colors.elevation2)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        SidebarDestination(
            label = "Home",
            iconRes = com.casty.music.R.drawable.sparkles,
            selected = currentRoute == "homeScreenRoute",
            expanded = expanded,
            onClick = { navController.navigate("homeScreenRoute") }
        )

        SidebarDestination(
            label = "Search",
            iconRes = com.casty.music.R.drawable.search,
            selected = currentRoute == "searchScreenRoute",
            expanded = expanded,
            onClick = { navController.navigate("searchScreenRoute") }
        )

        SidebarDestination(
            label = "Your Library",
            iconRes = com.casty.music.R.drawable.playlist,
            selected = currentRoute == "libraryScreenRoute",
            expanded = expanded,
            onClick = { navController.navigate("libraryScreenRoute") }
        )

        SidebarDestination(
            label = "Account",
            iconRes = com.casty.music.R.drawable.person,
            selected = currentRoute == "accountScreenRoute",
            expanded = expanded,
            onClick = { navController.navigate("accountScreenRoute") }
        )

        if (expanded) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Browse while your music keeps playing.",
                style = CastyTheme.typography.bodySmall.copy(
                    color = CastyTheme.colors.textSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (playerActive) {
            if (expanded) {
                MiniPlayer(
                    viewModel = playerViewModel,
                    onExpand = onPlayerClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CastyTheme.colors.elevation2)
                        .clickable(onClick = onPlayerClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = com.casty.music.R.drawable.play),
                        contentDescription = "Now Playing",
                        tint = CastyTheme.colors.accentPink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarDestination(
    label: String,
    iconRes: Int,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CastyTheme.colors.elevation4 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp)
                    .background(CastyTheme.colors.accentPink, RoundedCornerShape(500.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = if (selected) CastyTheme.colors.textPrimary else CastyTheme.colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        if (expanded) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = CastyTheme.typography.titleSmall.copy(
                    color = if (selected) CastyTheme.colors.textPrimary else CastyTheme.colors.textSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
