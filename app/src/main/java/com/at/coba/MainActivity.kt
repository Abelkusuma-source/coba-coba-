package com.at.coba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.at.coba.data.DataStoreManager
import com.at.coba.ui.Screen
import com.at.coba.ui.bottomNavItems
import com.at.coba.ui.screens.*
import com.at.coba.ui.theme.CobaTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStoreManager = DataStoreManager(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by dataStoreManager.themeMode.collectAsState(initial = DataStoreManager.MODE_SYSTEM_DEFAULT)
            val hasUserAgreed by dataStoreManager.hasUserAgreed.collectAsState(initial = null)
            val hasPermissionsShown by dataStoreManager.hasPermissionsShown.collectAsState(initial = null)
            
            var showSplash by remember { mutableStateOf(true) }

            val darkTheme = when (themeMode) {
                DataStoreManager.MODE_LIGHT -> false
                DataStoreManager.MODE_DARK -> true
                else -> isSystemInDarkTheme()
            }

            LaunchedEffect(Unit) {
                delay(2000) // Splash Screen Duration
                showSplash = false
            }

            CobaTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    when {
                        showSplash -> {
                            SplashScreen()
                        }
                        hasUserAgreed == null || hasPermissionsShown == null -> {
                            // Loading State
                        }
                        !hasUserAgreed!! -> {
                            UserAgreementScreen(dataStoreManager) { }
                        }
                        !hasPermissionsShown!! -> {
                            PermissionScreen(dataStoreManager) { }
                        }
                        else -> {
                            MainScreen(dataStoreManager)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "COBA APP",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dataStoreManager: DataStoreManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val buildVersion = "v6.6.6"

    val isTopLevelDestination = bottomNavItems.any { it.route == currentDestination?.route }
    val isDebugScreen = currentDestination?.route == Screen.Debug.route

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val title = when (currentDestination?.route) {
                        Screen.Trade.route -> Screen.Trade.title
                        Screen.History.route -> Screen.History.title
                        Screen.Web.route -> Screen.Web.title
                        Screen.Profile.route -> Screen.Profile.title
                        Screen.Debug.route -> Screen.Debug.title
                        else -> "App"
                    }
                    Text(text = "$title $buildVersion")
                },
                navigationIcon = {
                    if (!isTopLevelDestination && currentDestination != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isDebugScreen) {
                        IconButton(onClick = {
                            navController.navigate(Screen.Debug.route) {
                            navController.navigate(Screen.Debug.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                                launchSingleTop = true
                                restoreState = true

                            }
                        }) {
                            Icon(Icons.Default.BugReport, contentDescription = "Debug")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Trade.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            composable(Screen.Trade.route) { TradeScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Web.route) { WebScreen() }
            composable(Screen.Profile.route) { ProfileScreen(dataStoreManager) }
            composable(Screen.Debug.route) { DebugScreen(dataStoreManager) }
        }
    }
}
