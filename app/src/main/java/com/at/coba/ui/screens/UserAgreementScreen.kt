package com.at.coba.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.at.coba.data.DataStoreManager
import kotlinx.coroutines.launch

@Composable
fun UserAgreementScreen(dataStoreManager: DataStoreManager, onAgreed: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val agreementText = "To the fullest extent permitted by law, the developers, creators, and distributors of this application shall not be held liable for any financial losses, damages, or negative outcomes arising from the use of this application. This limitation applies regardless of the legal theory upon which any claim is based."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "User Agreement",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = agreementText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Justify
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    dataStoreManager.setUserAgreed(true)
                    onAgreed()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I Understand and Agree")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I Disagree - Exit App")
        }
    }
}
