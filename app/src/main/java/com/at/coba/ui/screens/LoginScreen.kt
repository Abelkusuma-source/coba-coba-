package com.at.coba.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // State Baru untuk OTP
    var otpValue by remember { mutableStateOf("") }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess((uiState as LoginUiState.Success).userAgreed)
        } else if (uiState is LoginUiState.Is2FARequired) {
            // Auto focus ke box pertama saat 2FA muncul
            focusRequesters[0].requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Email Field (Disabled saat 2FA)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = uiState !is LoginUiState.Loading && uiState !is LoginUiState.Is2FARequired
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field (Disabled saat 2FA)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    enabled = uiState !is LoginUiState.Is2FARequired
                ) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            singleLine = true,
            enabled = uiState !is LoginUiState.Loading && uiState !is LoginUiState.Is2FARequired
        )

        Spacer(modifier = Modifier.height(24.dp))

        // UI OTP (Hanya muncul jika Is2FARequired)
        AnimatedVisibility(visible = uiState is LoginUiState.Is2FARequired) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Verifikasi Google Authenticator",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Buka aplikasi Google Authenticator di HP kamu " +
                           "dan masukkan kode 6 digit yang tertera",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(6) { index ->
                        OutlinedTextField(
                            value = otpValue.getOrNull(index)?.toString() ?: "",
                            onValueChange = { newValue ->
                                if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                                    val currentOtp = otpValue.padEnd(6, ' ')
                                    val newOtp = if (newValue.isEmpty()) {
                                        currentOtp.substring(0, index) + " " + currentOtp.substring(index + 1)
                                    } else {
                                        currentOtp.substring(0, index) + newValue + currentOtp.substring(index + 1)
                                    }
                                    otpValue = newOtp.replace(" ", "")
                                    
                                    if (newValue.isNotEmpty() && index < 5) {
                                        focusRequesters[index + 1].requestFocus()
                                    } else if (newValue.isEmpty() && index > 0) {
                                        focusRequesters[index - 1].requestFocus()
                                    }
                                }
                            },
                            modifier = Modifier
                                .width(45.dp)
                                .focusRequester(focusRequesters[index]),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = uiState !is LoginUiState.Loading
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SESUDAH ✅
                Button(
                    onClick = { viewModel.verifyOtp(context, otpValue) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = otpValue.length == 6 && uiState !is LoginUiState.Loading
                ){
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Verify")
                    }
                }
            }
        }

        // Pesan Error
        if (uiState is LoginUiState.Error) {
            val errorMessage = if (uiState is LoginUiState.Is2FARequired || otpValue.isNotEmpty()) {
                "Invalid code. Please check the app and try again"
            } else {
                (uiState as LoginUiState.Error).message
            }

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // Tombol Login (Sembunyikan saat Is2FARequired)
        AnimatedVisibility(visible = uiState !is LoginUiState.Is2FARequired) {
            Button(
                onClick = { viewModel.login(context, email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank() && uiState !is LoginUiState.Loading
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Login")
                }
            }
        }
    }
}
