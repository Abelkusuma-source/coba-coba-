package com.at.coba.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.at.coba.R
import com.at.coba.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    themeMode: ThemeMode,
    profileImageUri: String?,
    onProfileImagePicked: (Uri?) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> onProfileImagePicked(uri) }
    )

    val openGallery: () -> Unit = { galleryLauncher.launch("image/*") }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Logout") },
            text = { Text("Apakah Anda yakin ingin keluar dan memutuskan semua koneksi trading?") },
            confirmButton = {
                TextButton(onClick = {
                    onLogout()
                    showLogoutDialog = false
                }) {
                    Text("LOGOUT", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    val currentModeInfo = remember(themeMode) {
        when (themeMode) {
            ThemeMode.SYSTEM_DEFAULT -> R.string.system_default to Icons.Default.BrightnessAuto
            ThemeMode.LIGHT -> R.string.light_mode to Icons.Default.LightMode
            ThemeMode.DARK -> R.string.dark_mode to Icons.Default.DarkMode
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        val photoCd = stringResource(R.string.profile_photo_content_description)
        val changePhotoCd = stringResource(R.string.change_profile_photo)

        Box(
            modifier = Modifier.padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
                    .semantics {
                        contentDescription = changePhotoCd
                        role = Role.Button
                    }
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
                    .clickable(onClick = openGallery),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(profileImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = photoCd,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            FilledIconButton(
                onClick = openGallery,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(44.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = changePhotoCd,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = stringResource(R.string.user_profile),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.personalization),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.theme_selection),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(currentModeInfo.first),
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = currentModeInfo.second,
                                contentDescription = null
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val labelRes = when (mode) {
                                ThemeMode.SYSTEM_DEFAULT -> R.string.system_default
                                ThemeMode.LIGHT -> R.string.light_mode
                                ThemeMode.DARK -> R.string.dark_mode
                            }
                            val icon = when (mode) {
                                ThemeMode.SYSTEM_DEFAULT -> Icons.Default.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            }

                            DropdownMenuItem(
                                text = { Text(stringResource(labelRes)) },
                                leadingIcon = { Icon(icon, contentDescription = null) },
                                onClick = {
                                    onThemeSelected(mode)
                                    expanded = false
                                }
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
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("LOGOUT DARI AKUN")
        }
    }
}
