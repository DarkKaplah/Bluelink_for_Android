package com.example.iosportattempt1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.iosportattempt1.repository.BluelinkRepository
import com.example.iosportattempt1.storage.DataStoreManager
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.iosportattempt1.ui.SettingsScreen
import com.example.iosportattempt1.ui.theme.IOSPortAttempt1Theme
import com.example.iosportattempt1.viewmodels.MainViewModel
import com.example.iosportattempt1.viewmodels.ViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val dataStoreManager = DataStoreManager(applicationContext)
        val repository = BluelinkRepository(dataStoreManager)
        val factory = ViewModelFactory(repository, dataStoreManager)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            IOSPortAttempt1Theme {
                var currentScreen by remember { mutableStateOf("main") }
                
                if (currentScreen == "main") {
                    MainScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { currentScreen = "settings" }
                    )
                } else {
                    val uiState by viewModel.uiState.collectAsState()
                    SettingsScreen(
                        currentConfig = uiState.config,
                        onSave = { 
                            viewModel.saveConfig(it)
                            currentScreen = "main"
                        },
                        onBack = { currentScreen = "main" }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(uiState.carName, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Battery Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${uiState.status?.soc ?: 0}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "~ ${uiState.status?.range ?: 0} mi",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Login/MFA Status
            if (uiState.config.auth.username.isEmpty()) {
                Text("Please configure your credentials in Settings", color = MaterialTheme.colorScheme.error)
            } else if (uiState.status == null && !uiState.isLoading) {
                Button(onClick = { viewModel.login() }) {
                    Text("Login to Kia Bluelink")
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            if (uiState.error != null) {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Car Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://bluelink.andyfase.com/app-assets/car-images/ev6.png",
                    contentDescription = "Car Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { 
                    val locked = uiState.status?.locked ?: false
                    ActionButton(
                        label = if (locked) "Unlock" else "Lock", 
                        icon = if (locked) Icons.Default.LockOpen else Icons.Default.Lock
                    ) { 
                        viewModel.lockUnlock() 
                    } 
                }
                item { 
                    val climateOn = uiState.status?.climate ?: false
                    ActionButton(
                        label = if (climateOn) "Climate Off" else "Climate On", 
                        icon = Icons.Default.Air
                    ) { 
                        viewModel.toggleClimate() 
                    } 
                }
                item { 
                    val charging = uiState.status?.isCharging ?: false
                    ActionButton(
                        label = if (charging) "Stop Charge" else "Start Charge", 
                        icon = Icons.Default.EvStation
                    ) { 
                        viewModel.toggleCharge() 
                    } 
                }
                item { ActionButton("Refresh", Icons.Default.Refresh) { viewModel.refreshStatus(true) } }
            }
            
            // Settings Button (Lower)
            Button(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("All Settings")
            }
        }
    }
    
    if (uiState.mfaRequired) {
        MfaDialog(onVerify = { viewModel.verifyMfa(it) })
    }
}

@Composable
fun ActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Text(label)
        }
    }
}

@Composable
fun MfaDialog(onVerify: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { },
        title = { Text("MFA Required") },
        text = {
            TextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Enter MFA Code") }
            )
        },
        confirmButton = {
            Button(onClick = { onVerify(code) }) {
                Text("Verify")
            }
        }
    )
}
