package com.at.coba.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.at.coba.R
import com.at.coba.data.ThemeMode
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

// Skema warna biru muda sesuai referensi gambar
val LightBlueButton = Color(0xFFD1E9FF)
val LightPurpleText = Color(0xFFB8B1E0) // Warna teks Simpan yang agak keunguan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    themeMode: ThemeMode,
    profileImageUri: String?,
    userEmail: String?,
    userPhone: String?,
    userNickname: String?,
    isEmailVerified: Boolean,
    isPhoneVerified: Boolean,
    isDocsVerified: Boolean,
    uiState: ProfileViewModel.ProfileUiState,
    messageFlow: SharedFlow<String>,
    onProfileImagePicked: (Uri?) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onUpdatePhone: (String) -> Unit,
    onUpdateNickname: (String) -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Form states - keyed by DataStore values to update when fetch succeeds (200 OK)
    var phone by remember(userPhone) { mutableStateOf(userPhone ?: "") }
    var nickname by remember(userNickname) { mutableStateOf(userNickname ?: "") }
    
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

        if (uiState is ProfileViewModel.ProfileUiState.Loading) {
            ProfileSkeleton(Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent(),
                    onResult = { uri: Uri? -> onProfileImagePicked(uri) }
                )
                val openGallery: () -> Unit = { galleryLauncher.launch("image/*") }

                // Avatar Section
                Box(
                    modifier = Modifier.padding(bottom = 32.dp, top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        .clickable(onClick = openGallery),
                    contentAlignment = Alignment.Center
                ) {
                        // Flow: jika data tidak memiliki avatar atau avatar = null -> icon saja
                        if (profileImageUri.isNullOrBlank()) {
                            Icon(Icons.Default.Person, null, Modifier.size(80.dp), MaterialTheme.colorScheme.primary)
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(profileImageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    FilledIconButton(
                        onClick = openGallery,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(40.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, null, Modifier.size(20.dp), Color.White)
                    }
                }

                // Status Verifikasi
                Surface(
                    color = if (isDocsVerified) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isDocsVerified) Icons.Default.CheckCircle else Icons.Default.Person,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isDocsVerified) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isDocsVerified) "Akun Terverifikasi" else "Akun Belum Terverifikasi",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDocsVerified) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Nama Panggilan Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Nama panggilan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = { Text("Nama panggilan") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onUpdateNickname(nickname) },
                            enabled = uiState is ProfileViewModel.ProfileUiState.Idle,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LightBlueButton,
                                contentColor = LightPurpleText
                            )
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Kontak Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Kontak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telepon") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            trailingIcon = {
                                if (isPhoneVerified) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onUpdatePhone(phone) },
                            enabled = uiState is ProfileViewModel.ProfileUiState.Idle,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LightBlueButton,
                                contentColor = LightPurpleText
                            )
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = userEmail ?: "Memuat email...",
                        onValueChange = {},
                        label = { Text("Email") },
                        readOnly = true,
                        enabled = false,
                        trailingIcon = { 
                            Icon(
                                Icons.Default.CheckCircle, 
                                null, 
                                tint = if (isEmailVerified) Color(0xFF4CAF50) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            disabledBorderColor = Color.Transparent,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Theme Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Pengaturan Tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = when(themeMode) {
                                    ThemeMode.LIGHT -> "Terang"
                                    ThemeMode.DARK -> "Gelap"
                                    else -> "Ikuti Sistem"
                                },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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
        // Avatar Placeholder
        Box(
            modifier = Modifier
                .padding(bottom = 32.dp, top = 16.dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.2f))
        )

        // Status Badge Placeholder
        Box(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .width(150.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.1f))
        )

        repeat(3) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Box(modifier = Modifier.width(100.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.1f)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(Color.Gray.copy(alpha = 0.05f)))
            }
        }
    }
}
