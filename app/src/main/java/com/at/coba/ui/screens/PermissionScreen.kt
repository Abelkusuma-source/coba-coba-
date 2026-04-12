package com.at.coba.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PermissionScreen(dataStoreManager: DataStoreManager, onContinue: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Status Izin secara Real-time
    var isBatteryIgnored by remember { mutableStateOf(false) }
    var isNotificationGranted by remember { mutableStateOf(false) }
    
    // Cek status secara berkala saat layar aktif
    LaunchedEffect(Unit) {
        while (true) {
            // 1. Cek Optimasi Baterai
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            isBatteryIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            
            // 2. Cek Izin Notifikasi
            isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true 
            }
            
            delay(1000) // Re-check setiap detik
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Permissions",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "To provide the best experience, we recommend enabling the following options. You can skip them if you prefer.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // 1. Notification Permission
        PermissionItem(
            title = "Notifications",
            description = "Get alerts for trades and price updates.",
            icon = Icons.Default.Notifications,
            isGranted = isNotificationGranted, 
            onClick = { 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )

        // 2. Battery Optimization
        PermissionItem(
            title = "Disable Battery Optimization",
            description = "Ensure the app runs smoothly in the background.",
            icon = Icons.Default.BatteryAlert,
            isGranted = isBatteryIgnored,
            onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
            }
        )

        // 3. App Restrictions / Settings
        PermissionItem(
            title = "App Restrictions",
            description = "Remove system restrictions for better performance.",
            icon = Icons.Default.SettingsSuggest,
            isGranted = false,
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    dataStoreManager.setPermissionsShown(true)
                    onContinue()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue Anyway")
        }
    }
}

@Composable
fun PermissionItem(title: String, description: String, icon: ImageVector, isGranted: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = { if (!isGranted) onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = if (isGranted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isGranted) "Permission Granted" else description, 
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
