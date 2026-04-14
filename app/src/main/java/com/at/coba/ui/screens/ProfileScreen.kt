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
import androidx.compose.ui.unit.dp
import com.at.coba.data.DataStoreManager
import com.at.coba.data.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(dataStoreManager: DataStoreManager) {
    val themeMode by dataStoreManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM_DEFAULT)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    val currentModeInfo = remember(themeMode) {
        when (themeMode) {
            ThemeMode.SYSTEM_DEFAULT -> "System Default" to Icons.Default.BrightnessAuto
            ThemeMode.LIGHT -> "Light Mode" to Icons.Default.LightMode
            ThemeMode.DARK -> "Dark Mode" to Icons.Default.DarkMode
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
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(text = "User Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Personalization", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Theme Selection", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = currentModeInfo.first,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(currentModeInfo.second, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM_DEFAULT -> "System Default"
                                ThemeMode.LIGHT -> "Light Mode"
                                ThemeMode.DARK -> "Dark Mode"
                            }
                            val icon = when (mode) {
                                ThemeMode.SYSTEM_DEFAULT -> Icons.Default.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            }

                            DropdownMenuItem(
                                text = { Text(label) },
                                leadingIcon = { Icon(icon, contentDescription = null) },
                                onClick = {
                                    scope.launch {
                                        dataStoreManager.setThemeMode(mode)
                                    }
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
