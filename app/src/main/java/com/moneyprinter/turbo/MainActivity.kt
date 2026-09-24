package com.moneyprinter.turbo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moneyprinter.turbo.data.SettingsRepository
import com.moneyprinter.turbo.ui.screens.CreateScreen
import com.moneyprinter.turbo.ui.screens.HistoryScreen
import com.moneyprinter.turbo.ui.screens.SettingsScreen
import com.moneyprinter.turbo.ui.theme.MoneyPrinterTurboTheme
import com.moneyprinter.turbo.ui.viewmodel.MainViewModel
import com.moneyprinter.turbo.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results ignored – app still works with app-private storage */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!PermissionHelper.hasAll(this)) {
            permissionLauncher.launch(PermissionHelper.requiredPermissions())
        }

        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            MoneyPrinterTurboTheme {
                val navController = rememberNavController()
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "create"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == "create",
                                onClick = { navController.navigate("create") { launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                label = { Text("Buat") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "history",
                                onClick = { navController.navigate("history") { launchSingleTop = true } },
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text("Riwayat") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "settings",
                                onClick = { navController.navigate("settings") { launchSingleTop = true } },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Pengaturan") }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "create",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("create") {
                            CreateScreen(
                                onStartGeneration = { params -> viewModel.startGeneration(params) },
                                isGenerating = uiState.isGenerating,
                                progressMessage = uiState.progressMessage,
                                lastError = uiState.lastError,
                                lastVideoPath = uiState.lastVideoPath
                            )
                        }
                        composable("history") {
                            HistoryScreen()
                        }
                        composable("settings") {
                            SettingsScreen(settings = settingsRepo)
                        }
                    }
                }
            }
        }
    }
}
