package com.at.coba.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.at.coba.R
import com.at.coba.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null, // Dekoratif
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
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
                                contentDescription = null // Sudah ada label teks di sebelahnya
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
    }
}
