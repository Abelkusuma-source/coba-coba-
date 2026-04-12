package com.at.coba

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

class MainActivity : ComponentActivity() {
    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStoreManager = DataStoreManager(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by dataStoreManager.themeMode.collectAsState(initial = DataStoreManager.MODE_SYSTEM_DEFAULT)
            
            val darkTheme = when (themeMode) {
                DataStoreManager.MODE_LIGHT -> false
                DataStoreManager.MODE_DARK -> true
                else -> isSystemInDarkTheme()
            }

            var showBatteryDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    showBatteryDialog = true
                }
            }

            CobaTheme(darkTheme = darkTheme) {
                if (showBatteryDialog) {
                    AlertDialog(
                        onDismissRequest = { showBatteryDialog = false },
                        title = { Text("Disable Battery Optimization") },
                        text = { Text("To ensure the app runs smoothly in the background, please disable battery optimization for Coba.") },
                        confirmButton = {
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                                startActivity(intent)
                                showBatteryDialog = false
                            }) {
                                Text("Allow")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBatteryDialog = false }) {
                                Text("Deny")
                            }
                        }
                    )
                }
                MainScreen(dataStoreManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dataStoreManager: DataStoreManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    
    // Mengambil versi aplikasi secara dinamis untuk menghindari kesalahan manual
    val buildVersion = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${packageInfo.versionName}"
        } catch (e: Exception) {
            "v1.0.0"
        }
    }

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
                                launchSingleTop = true
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
