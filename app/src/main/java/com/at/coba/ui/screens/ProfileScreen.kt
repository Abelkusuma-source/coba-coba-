package com.at.coba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.at.coba.data.ThemeMode
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    themeMode: ThemeMode,
    avatarDisplayUrl: String?,
    avatarCacheEpoch: Long,
    userEmail: String?,
    userPhone: String?,
    userNickname: String?,
    isEmailVerified: Boolean,
    isPhoneVerified: Boolean,
    isDocsVerified: Boolean,
    uiState: ProfileViewModel.ProfileUiState,
    messageFlow: SharedFlow<String>,
    isPullRefreshing: Boolean,
    onThemeSelected: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    onRetryInitialLoad: () -> Unit,
    onProfileResumed: () -> Unit,
    onPullRefresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onProfileResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isPullRefreshing,
        onRefresh = onPullRefresh
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(messageFlow) {
        messageFlow.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Konfirmasi Logout") },
                text = { Text("Apakah Anda yakin ingin keluar?") },
                confirmButton = {
                    TextButton(onClick = { onLogout(); showLogoutDialog = false }) {
                        Text("LOGOUT", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("BATAL") }
                }
            )
        }

        when (uiState) {
            ProfileViewModel.ProfileUiState.Loading -> {
                ProfileSkeleton(Modifier.padding(padding))
            }
            is ProfileViewModel.ProfileUiState.LoadError -> {
                ProfileLoadError(
                    message = uiState.message,
                    modifier = Modifier.padding(padding),
                    onRetry = onRetryInitialLoad
                )
            }
            ProfileViewModel.ProfileUiState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .pullRefresh(pullRefreshState)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // Avatar — hanya tampilan (sinkron dari server)
                    Box(
                        modifier = Modifier.padding(bottom = 32.dp, top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    3.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarDisplayUrl.isNullOrBlank()) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    Modifier.size(80.dp),
                                    MaterialTheme.colorScheme.primary
                                )
                            } else {
                                val epoch = avatarCacheEpoch
                                val req = remember(avatarDisplayUrl, epoch) {
                                    ImageRequest.Builder(context)
                                        .data(avatarDisplayUrl)
                                        .memoryCacheKey(
                                            MemoryCache.Key("stockity_avatar_${epoch}_${avatarDisplayUrl.orEmpty()}")
                                        )
                                        .diskCacheKey("stockity_avatar_${epoch}_${avatarDisplayUrl.orEmpty()}")
                                        .crossfade(true)
                                        .build()
                                }
                                AsyncImage(
                                    model = req,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Surface(
                        color = if (isDocsVerified) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isDocsVerified) Icons.Default.CheckCircle else Icons.Default.Person,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isDocsVerified) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isDocsVerified) "Akun Terverifikasi" else "Akun Belum Terverifikasi",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isDocsVerified) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Data akun",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            ProfileReadOnlyRow(
                                label = "Nama panggilan",
                                value = userNickname?.takeIf { it.isNotBlank() } ?: "—"
                            )
                            Spacer(Modifier.height(12.dp))
                            ProfileReadOnlyRow(
                                label = "Telepon",
                                value = userPhone?.takeIf { it.isNotBlank() } ?: "—",
                                verified = isPhoneVerified
                            )
                            Spacer(Modifier.height(12.dp))
                            ProfileReadOnlyRow(
                                label = "Email",
                                value = userEmail?.takeIf { it.isNotBlank() } ?: "—",
                                verified = isEmailVerified
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Pengaturan Tema",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = when (themeMode) {
                                        ThemeMode.LIGHT -> "Terang"
                                        ThemeMode.DARK -> "Gelap"
                                        else -> "Ikuti Sistem"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                    },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(expanded, { expanded = false }) {
                                    ThemeMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode.name) },
                                            onClick = { onThemeSelected(mode); expanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text("LOGOUT")
                    }

                    Spacer(modifier = Modifier.height(64.dp))
                    }
                    PullRefreshIndicator(
                        refreshing = isPullRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileReadOnlyRow(
    label: String,
    value: String,
    verified: Boolean = false
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (verified) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Terverifikasi",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileLoadError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Profil tidak dapat dimuat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Coba lagi")
        }
    }
}

@Composable
fun ProfileSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 32.dp, top = 16.dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.2f))
        )

        Box(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.1f))
        )

        repeat(3) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Box(
                    modifier = Modifier.width(100.dp).height(16.dp).background(Color.Gray.copy(alpha = 0.1f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.05f))
                )
            }
        }
    }
}
