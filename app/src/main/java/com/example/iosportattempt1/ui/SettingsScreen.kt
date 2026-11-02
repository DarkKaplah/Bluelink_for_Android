package com.example.iosportattempt1.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.iosportattempt1.models.AppConfig
import com.example.iosportattempt1.models.AuthConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: AppConfig,
    onSave: (AppConfig) -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf(currentConfig.auth.username) }
    var password by remember { mutableStateOf(currentConfig.auth.password) }
    var pin by remember { mutableStateOf(currentConfig.auth.pin) }
    var region by remember { mutableStateOf(currentConfig.region) }
    var manufacturer by remember { mutableStateOf(currentConfig.manufacturer) }
    var vin by remember { mutableStateOf(currentConfig.vin ?: "") }
    var tempType by remember { mutableStateOf(currentConfig.tempType) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = manufacturer,
                onValueChange = { manufacturer = it },
                label = { Text("Manufacturer (Kia/Hyundai)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = region,
                onValueChange = { region = it },
                label = { Text("Region (USA/Canada/etc)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vin,
                onValueChange = { vin = it },
                label = { Text("Optional VIN") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSave(
                        currentConfig.copy(
                            manufacturer = manufacturer,
                            region = region,
                            auth = AuthConfig(username, password, pin, region),
                            vin = vin.ifBlank { null }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}
