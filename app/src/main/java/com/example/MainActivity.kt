package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MockExamScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SolverHomeScreen
import com.example.ui.theme.MathSolverTheme
import com.example.viewmodel.MathViewModel

class MainActivity : ComponentActivity() {

    private val mathViewModel: MathViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by mathViewModel.uiState.collectAsState()

            MathSolverTheme(darkTheme = uiState.isDarkMode) {
                // Force RTL for complete Arabic experience
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: "solver"

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == "solver",
                                    onClick = {
                                        navController.navigate("solver") {
                                            popUpTo("solver") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Calculate, contentDescription = "الحل") },
                                    label = { Text("المساعد") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "history",
                                    onClick = {
                                        navController.navigate("history") {
                                            popUpTo("solver") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.History, contentDescription = "السجل") },
                                    label = { Text("السجل") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "exam",
                                    onClick = {
                                        navController.navigate("exam") {
                                            popUpTo("solver") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "الامتحان") },
                                    label = { Text("امتحان تجريبي") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        navController.navigate("settings") {
                                            popUpTo("solver") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                                    label = { Text("الإعدادات") }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "solver",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("solver") {
                                SolverHomeScreen(viewModel = mathViewModel, uiState = uiState)
                            }
                            composable("history") {
                                HistoryScreen(viewModel = mathViewModel)
                            }
                            composable("exam") {
                                MockExamScreen()
                            }
                            composable("settings") {
                                SettingsScreen(viewModel = mathViewModel, uiState = uiState)
                            }
                        }
                    }
                }
            }
        }
    }
}
