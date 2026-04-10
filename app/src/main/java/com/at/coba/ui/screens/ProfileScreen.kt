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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(dataStoreManager: DataStoreManager) {
    val themeMode by dataStoreManager.themeMode.collectAsState(initial = DataStoreManager.MODE_SYSTEM_DEFAULT)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    val modes = listOf(
        Triple(DataStoreManager.MODE_SYSTEM_DEFAULT, "System Default", Icons.Default.BrightnessAuto),
        Triple(DataStoreManager.MODE_LIGHT, "Light Mode", Icons.Default.LightMode),
        Triple(DataStoreManager.MODE_DARK, "Dark Mode", Icons.Default.DarkMode)
    )

    val currentModeInfo = modes.find { it.first == themeMode } ?: modes[0]

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
                        value = currentModeInfo.second,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(currentModeInfo.third, contentDescription = null) },
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
                        modes.forEach { (mode, label, icon) ->
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
